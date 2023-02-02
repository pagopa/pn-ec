package it.pagopa.pn.ec.sms.service;

import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.SnsService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.rest.v1.dto.DigitalRequestDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.sms.model.dto.NtStatoSmsQueueDto;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import static it.pagopa.pn.ec.commons.constant.ProcessId.INVIO_SMS;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.*;
import static it.pagopa.pn.ec.commons.constant.status.CommonStatus.*;
import static it.pagopa.pn.ec.commons.service.SnsService.DEFAULT_RETRY_STRATEGY;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestDto.ChannelEnum.SMS;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestDto.MessageContentTypeEnum.PLAIN;

@Service
@Slf4j
public class SmsService extends PresaInCaricoService {

    private final SqsService sqsService;
    private final SnsService snsService;
    private final GestoreRepositoryCall gestoreRepositoryCall;

    protected SmsService(AuthService authService, SqsService sqsService, SnsService snsService,
                         GestoreRepositoryCall gestoreRepositoryCall) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
        this.snsService = snsService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo, RequestDto requestDtoPresentOrToInsert) {
//      Cast PresaInCaricoInfo to specific SmsPresaInCaricoInfo
        return Mono.just((SmsPresaInCaricoInfo) presaInCaricoInfo)

                   .flatMap(smsPresaInCaricoInfo -> {
                       if (requestDtoPresentOrToInsert.getRequestIdx() == null) {
                           var digitalCourtesySmsRequest = smsPresaInCaricoInfo.getDigitalCourtesySmsRequest();
                           digitalCourtesySmsRequest.setRequestId(presaInCaricoInfo.getRequestIdx());
                           return insertRequestFromSms(digitalCourtesySmsRequest);
                       } else {
                           return Mono.just(requestDtoPresentOrToInsert);
                       }
                   })

                   .onErrorResume(throwable -> Mono.error(new EcInternalEndpointHttpException()))

                   .flatMap(requestDto -> sqsService.send(NT_STATO_SMS_QUEUE_NAME,
                                                          new NtStatoSmsQueueDto(presaInCaricoInfo.getXPagopaExtchCxId(),
                                                                                 INVIO_SMS,
                                                                                 null,
                                                                                 BOOKED)))

                   .thenReturn((SmsPresaInCaricoInfo) presaInCaricoInfo)

                   .flatMap(smsPresaInCaricoInfo -> {
                       DigitalCourtesySmsRequest.QosEnum qos = smsPresaInCaricoInfo.getDigitalCourtesySmsRequest().getQos();
                       if (qos == INTERACTIVE) {
                           return sqsService.send(SMS_INTERACTIVE_QUEUE_NAME, smsPresaInCaricoInfo.getDigitalCourtesySmsRequest());
                       } else if (qos == BATCH) {
                           return sqsService.send(SMS_BATCH_QUEUE_NAME, smsPresaInCaricoInfo.getDigitalCourtesySmsRequest());
                       } else {
                           return Mono.empty();
                       }
                   })

                   .then();
    }

    private Mono<RequestDto> insertRequestFromSms(final DigitalCourtesySmsRequest digitalCourtesySmsRequest) {
        return Mono.fromCallable(() -> {
            var requestDto = new RequestDto();
            requestDto.setRequestIdx(digitalCourtesySmsRequest.getRequestId());
            var digitalRequestDto = new DigitalRequestDto();
            digitalRequestDto.setCorrelationId(digitalCourtesySmsRequest.getCorrelationId());
            // TODO: set event type ?
            digitalRequestDto.setEventType("");
            digitalRequestDto.setQos(DigitalRequestDto.QosEnum.valueOf(digitalCourtesySmsRequest.getQos().name()));
            digitalRequestDto.setTags(digitalCourtesySmsRequest.getTags());
            digitalRequestDto.setReceiverDigitalAddress(digitalCourtesySmsRequest.getReceiverDigitalAddress());
            digitalRequestDto.setMessageText(digitalCourtesySmsRequest.getMessageText());
            digitalRequestDto.setSenderDigitalAddress(digitalCourtesySmsRequest.getSenderDigitalAddress());
            digitalRequestDto.setChannel(SMS);
            // TODO: set subject text ?\
            digitalRequestDto.setSubjectText("");
            digitalRequestDto.setMessageContentType(PLAIN);
            requestDto.setDigitalReq(digitalRequestDto);
            return requestDto;
        }).flatMap(gestoreRepositoryCall::insertRichiesta);
    }

    @SqsListener(value = SMS_INTERACTIVE_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
    public void lavorazioneRichiesta(final DigitalCourtesySmsRequest digitalCourtesySmsRequest) {

        log.info("<-- START LAVORAZIONE RICHIESTA SMS -->");
        log.info("Incoming message from '{}' queue", SMS_INTERACTIVE_QUEUE_NAME);

        String requestIdx = digitalCourtesySmsRequest.getRequestId();

        Mono.just(digitalCourtesySmsRequest).doOnNext(message -> log.info("Incoming message {}", message))
//          Try to send SMS
            .then(snsService.send(digitalCourtesySmsRequest.getReceiverDigitalAddress(), digitalCourtesySmsRequest.getMessageText()))
//          Retrive request
            .flatMap(publishResponse -> gestoreRepositoryCall.getRichiesta(requestIdx))
//          Send to Notification Tracker with next status -> SENT
            .flatMap(requestDto -> sqsService.send(NT_STATO_SMS_QUEUE_NAME,
                                                   new NotificationTrackerQueueDto(requestIdx, null, INVIO_SMS, BOOKED, SENT)))
//          An error occurred during SMS send, the retries are started, send to Notification Tracker with next status -> RETRY
            .onErrorResume(SnsSendException.class, snsMaxRetriesExceeded -> retrySmsSend(digitalCourtesySmsRequest)).subscribe();
    }

    private Mono<SendMessageResponse> retrySmsSend(final DigitalCourtesySmsRequest digitalCourtesySmsRequest) {
        return sqsService.send(NT_STATO_SMS_QUEUE_NAME, new NotificationTrackerQueueDto(null, null, INVIO_SMS, BOOKED, RETRY))
                         .then(snsService.send(digitalCourtesySmsRequest.getReceiverDigitalAddress(),
                                               digitalCourtesySmsRequest.getMessageText()).retryWhen(DEFAULT_RETRY_STRATEGY))
                         .then(sqsService.send(NT_STATO_SMS_QUEUE_NAME,
                                               new NotificationTrackerQueueDto(digitalCourtesySmsRequest.getRequestId(),
                                                                               null,
                                                                               INVIO_SMS,
                                                                               BOOKED,
                                                                               SENT)))
                         .onErrorResume(SnsSendException.SnsMaxRetriesExceededException.class,
                                        snsMaxRetriesExceeded -> smsRetriesExceeded(digitalCourtesySmsRequest));
    }

    private Mono<SendMessageResponse> smsRetriesExceeded(final DigitalCourtesySmsRequest digitalCourtesySmsRequest) {
        return sqsService.send(NT_STATO_SMS_QUEUE_NAME,
                               new NotificationTrackerQueueDto(digitalCourtesySmsRequest.getRequestId(), null, INVIO_SMS, RETRY, ERROR))
                         .then(sqsService.send(SMS_ERROR_QUEUE_NAME, digitalCourtesySmsRequest));
    }
}
