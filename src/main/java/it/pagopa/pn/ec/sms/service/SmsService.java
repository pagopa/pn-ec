package it.pagopa.pn.ec.sms.service;

import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
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

import java.util.concurrent.atomic.AtomicReference;

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
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo, RequestDto requestDtoToInsert) {
        return Mono.just((SmsPresaInCaricoInfo) presaInCaricoInfo)

                   .flatMap(smsPresaInCaricoInfo -> {
                       var digitalCourtesySmsRequest = smsPresaInCaricoInfo.getDigitalCourtesySmsRequest();
                       digitalCourtesySmsRequest.setRequestId(presaInCaricoInfo.getRequestIdx());
                       return insertRequestFromSms(digitalCourtesySmsRequest);
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
                           return sqsService.send(SMS_INTERACTIVE_QUEUE_NAME, smsPresaInCaricoInfo);
                       } else if (qos == BATCH) {
                           return sqsService.send(SMS_BATCH_QUEUE_NAME, smsPresaInCaricoInfo);
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
            // TODO: set subject text ?
            digitalRequestDto.setSubjectText("");
            digitalRequestDto.setMessageContentType(PLAIN);
            requestDto.setDigitalReq(digitalRequestDto);
            return requestDto;
        }).flatMap(gestoreRepositoryCall::insertRichiesta);
    }

    @SqsListener(value = SMS_INTERACTIVE_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
    public void lavorazioneRichiesta(final SmsPresaInCaricoInfo smsPresaInCaricoInfo) {

        log.info("<-- START LAVORAZIONE RICHIESTA SMS -->");
        log.info("Incoming message from '{}' queue", SMS_INTERACTIVE_QUEUE_NAME);

        String requestId = smsPresaInCaricoInfo.getRequestIdx();
        String clientId = smsPresaInCaricoInfo.getXPagopaExtchCxId();
        DigitalCourtesySmsRequest digitalCourtesySmsRequest = smsPresaInCaricoInfo.getDigitalCourtesySmsRequest();

        AtomicReference<RequestDto> currentRequestStatus = new AtomicReference<>();

        Mono.just(smsPresaInCaricoInfo)
            .doOnNext(message -> log.info("Incoming message {}", message))
//          Retrive request
            .flatMap(publishResponse -> gestoreRepositoryCall.getRichiesta(requestId))
            .doOnNext(currentRequestStatus::set)
//          Try to send SMS
            .then(snsService.send(digitalCourtesySmsRequest.getReceiverDigitalAddress(), digitalCourtesySmsRequest.getMessageText()))
//          Send to Notification Tracker with next status -> SENT
            .flatMap(requestDto -> sqsService.send(NT_STATO_SMS_QUEUE_NAME,
                                                   new NotificationTrackerQueueDto(requestId, clientId, INVIO_SMS, BOOKED, SENT)))
//          An error occurred during SMS send, the retries are started, send to Notification Tracker with next status -> RETRY
            .onErrorResume(SnsSendException.class, snsSendException -> retrySmsSend(smsPresaInCaricoInfo, currentRequestStatus.get()))
//          An error occurred during SQS publishing to the Notification Tracker -> Publish to Errori SMS queue and notify to retry update
//          status only
            // TODO: CHANGE THE PAYLOAD
            .onErrorResume(SqsPublishException.class, sqsPublishException -> sqsService.send(SMS_ERROR_QUEUE_NAME, smsPresaInCaricoInfo))
            .subscribe();
    }

    private Mono<SendMessageResponse> retrySmsSend(final SmsPresaInCaricoInfo smsPresaInCaricoInfo, final RequestDto requestDto) {

        String requestId = smsPresaInCaricoInfo.getRequestIdx();
        String clientId = smsPresaInCaricoInfo.getXPagopaExtchCxId();
        DigitalCourtesySmsRequest digitalCourtesySmsRequest = smsPresaInCaricoInfo.getDigitalCourtesySmsRequest();

        return sqsService.send(NT_STATO_SMS_QUEUE_NAME, new NotificationTrackerQueueDto(requestId, clientId, INVIO_SMS, BOOKED, RETRY))
                         .then(snsService.send(digitalCourtesySmsRequest.getReceiverDigitalAddress(),
                                               digitalCourtesySmsRequest.getMessageText()).retryWhen(DEFAULT_RETRY_STRATEGY))
                         .then(sqsService.send(NT_STATO_SMS_QUEUE_NAME,
                                               new NotificationTrackerQueueDto(requestId, clientId, INVIO_SMS, RETRY, SENT)))
                         .onErrorResume(SnsSendException.SnsMaxRetriesExceededException.class,
                                        snsMaxRetriesExceeded -> smsRetriesExceeded(smsPresaInCaricoInfo));
    }

    private Mono<SendMessageResponse> smsRetriesExceeded(final SmsPresaInCaricoInfo smsPresaInCaricoInfo) {

        String requestId = smsPresaInCaricoInfo.getRequestIdx();
        String clientId = smsPresaInCaricoInfo.getXPagopaExtchCxId();

        return gestoreRepositoryCall.getRichiesta(requestId)
                                    .flatMap(requestDto -> sqsService.send(NT_STATO_SMS_QUEUE_NAME,
                                                                           new NotificationTrackerQueueDto(requestId,
                                                                                                           clientId,
                                                                                                           INVIO_SMS,
                                                                                                           RETRY,
                                                                                                           ERROR)))
                                    .then(sqsService.send(SMS_ERROR_QUEUE_NAME, smsPresaInCaricoInfo));
    }
}
