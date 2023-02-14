package it.pagopa.pn.ec.sms.service;

import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.SnsService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sms.configurationproperties.SmsSqsQueueName;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.concurrent.atomic.AtomicReference;

import static it.pagopa.pn.ec.commons.constant.ProcessId.INVIO_SMS;
import static it.pagopa.pn.ec.commons.service.SnsService.DEFAULT_RETRY_STRATEGY;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.SMS;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestStatus.*;
import static java.time.OffsetDateTime.now;

@Service
@Slf4j
public class SmsService extends PresaInCaricoService {

    private final SqsService sqsService;
    private final SnsService snsService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final SmsSqsQueueName smsSqsQueueName;
    private final NotificationTrackerSqsName notificationTrackerSqsName;


    protected SmsService(AuthService authService, SqsService sqsService, SnsService snsService,
                         GestoreRepositoryCall gestoreRepositoryCall, NotificationTrackerSqsName notificationTrackerSqsName,
                         SmsSqsQueueName smsSqsQueueName) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
        this.snsService = snsService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.smsSqsQueueName = smsSqsQueueName;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {

        var smsPresaInCaricoInfo = (SmsPresaInCaricoInfo) presaInCaricoInfo;
        var digitalCourtesySmsRequest = smsPresaInCaricoInfo.getDigitalCourtesySmsRequest();
        digitalCourtesySmsRequest.setRequestId(presaInCaricoInfo.getRequestIdx());

//      Insert request from SMS request and publish to Notification Tracker with next status -> BOOKED
        return insertRequestFromSms(digitalCourtesySmsRequest).then(sqsService.send(notificationTrackerSqsName.statoSmsName(),
                                                                                    new NotificationTrackerQueueDto(presaInCaricoInfo.getRequestIdx(),
                                                                                                                    presaInCaricoInfo.getXPagopaExtchCxId(),
                                                                                                                    now(),
                                                                                                                    INVIO_SMS,
                                                                                                                    null,
                                                                                                                    BOOKED.getValue(),
                                                                                                                    // TODO: Populate
                                                                                                                    //  GeneratedMessageDto
                                                                                                                    // Use this syntax
                                                                                                                    // new
                                                                                                                    // GeneratedMessageDto().id("foo").location("bar").system("bla")
                                                                                                                    new GeneratedMessageDto())))
//                                                            Publish to SMS INTERACTIVE or SMS BATCH
                                                              .flatMap(sendMessageResponse -> {
                                                                  DigitalCourtesySmsRequest.QosEnum qos =
                                                                          smsPresaInCaricoInfo.getDigitalCourtesySmsRequest().getQos();
                                                                  if (qos == INTERACTIVE) {
                                                                      return sqsService.send(smsSqsQueueName.interactiveName(),
                                                                                             smsPresaInCaricoInfo);
                                                                  } else if (qos == BATCH) {
                                                                      return sqsService.send(smsSqsQueueName.batchName(),
                                                                                             smsPresaInCaricoInfo);
                                                                  } else {
                                                                      return Mono.empty();
                                                                  }
                                                              }).then();
    }

    @SuppressWarnings("Duplicates")
    private Mono<RequestDto> insertRequestFromSms(final DigitalCourtesySmsRequest digitalCourtesySmsRequest) {
        return Mono.fromCallable(() -> {
            var requestDto = new RequestDto();
            requestDto.setRequestIdx(digitalCourtesySmsRequest.getRequestId());
            requestDto.setClientRequestTimeStamp(digitalCourtesySmsRequest.getClientRequestTimeStamp());

            var requestPersonalDto = new RequestPersonalDto();
            var digitalRequestPersonalDto = new DigitalRequestPersonalDto();
            digitalRequestPersonalDto.setQos(DigitalRequestPersonalDto.QosEnum.valueOf(digitalCourtesySmsRequest.getQos().name()));
            digitalRequestPersonalDto.setReceiverDigitalAddress(digitalCourtesySmsRequest.getReceiverDigitalAddress());
            digitalRequestPersonalDto.setMessageText(digitalCourtesySmsRequest.getMessageText());
            digitalRequestPersonalDto.setSenderDigitalAddress(digitalCourtesySmsRequest.getSenderDigitalAddress());
            requestPersonalDto.setDigitalRequestPersonal(digitalRequestPersonalDto);

            var requestMetadataDto = new RequestMetadataDto();
            var digitalRequestMetadataDto = new DigitalRequestMetadataDto();
            digitalRequestMetadataDto.setCorrelationId(digitalCourtesySmsRequest.getCorrelationId());
            digitalRequestMetadataDto.setEventType(digitalCourtesySmsRequest.getEventType());
            digitalRequestMetadataDto.setTags(digitalCourtesySmsRequest.getTags());
            digitalRequestMetadataDto.setChannel(SMS);
            requestMetadataDto.setDigitalRequestMetadata(digitalRequestMetadataDto);

            requestDto.setRequestPersonal(requestPersonalDto);
            requestDto.setRequestMetadata(requestMetadataDto);
            return requestDto;
        }).flatMap(gestoreRepositoryCall::insertRichiesta);
    }

    @SqsListener(value = "${sqs.queue.sms.interactive-name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void lavorazioneRichiesta(final SmsPresaInCaricoInfo smsPresaInCaricoInfo) {

        log.info("<-- START LAVORAZIONE RICHIESTA SMS -->");
        logIncomingMessage(smsSqsQueueName.interactiveName(), smsPresaInCaricoInfo);

        String requestId = smsPresaInCaricoInfo.getRequestIdx();
        String clientId = smsPresaInCaricoInfo.getXPagopaExtchCxId();
        DigitalCourtesySmsRequest digitalCourtesySmsRequest = smsPresaInCaricoInfo.getDigitalCourtesySmsRequest();

        AtomicReference<String> currentRequestStatus = new AtomicReference<>();

//      Retrive current status from request and set the atomic reference variable
        gestoreRepositoryCall.getRichiesta(requestId)
                             .doOnNext(requestDto -> currentRequestStatus.set(requestDto.getStatusRequest()))
//                           Try to send SMS
                             .then(snsService.send(digitalCourtesySmsRequest.getReceiverDigitalAddress(),
                                                   digitalCourtesySmsRequest.getMessageText()))
//                           The SMS in sent, publish to Notification Tracker with next status -> SENT
                             .flatMap(publishResponse -> sqsService.send(notificationTrackerSqsName.statoSmsName(),
                                                                         new NotificationTrackerQueueDto(requestId,
                                                                                                         clientId,
                                                                                                         now(),
                                                                                                         INVIO_SMS,
                                                                                                         currentRequestStatus.get(),
                                                                                                         SENT.getValue(),
                                                                                                         // TODO: Populate
                                                                                                         //  GeneratedMessageDto
                                                                                                         // Use this syntax new
                                                                                                         // GeneratedMessageDto().id
                                                                                                         // ("foo").location("bar")
                                                                                                         // .system("bla")
                                                                                                         new GeneratedMessageDto())))
//                           An error occurred during SMS send, start retries
                             .onErrorResume(SnsSendException.class,
                                            snsSendException -> retrySmsSend(smsPresaInCaricoInfo, currentRequestStatus.get()))
//                           An error occurred during SQS publishing to the Notification Tracker -> Publish to Errori SMS queue and
//                           notify to retry update status only
//                           TODO: CHANGE THE PAYLOAD
                             .onErrorResume(SqsPublishException.class,
                                            sqsPublishException -> sqsService.send(smsSqsQueueName.errorName(), smsPresaInCaricoInfo))
                             .subscribe();
    }

    private Mono<SendMessageResponse> retrySmsSend(final SmsPresaInCaricoInfo smsPresaInCaricoInfo, final String currentStatus) {

        String requestId = smsPresaInCaricoInfo.getRequestIdx();
        String clientId = smsPresaInCaricoInfo.getXPagopaExtchCxId();
        DigitalCourtesySmsRequest digitalCourtesySmsRequest = smsPresaInCaricoInfo.getDigitalCourtesySmsRequest();

//      Publish to Notification Tracker with next status -> RETRY
        return sqsService.send(notificationTrackerSqsName.statoSmsName(),
                               new NotificationTrackerQueueDto(requestId, clientId, now(), INVIO_SMS, currentStatus, RETRY.getValue(),
                                                               // TODO: Populate GeneratedMessageDto
                                                               // Use this syntax new GeneratedMessageDto().id("foo").location("bar")
                                                               // .system("bla")
                                                               new GeneratedMessageDto()))
//                       Try to send SMS, retry when fail
                         .then(snsService.send(digitalCourtesySmsRequest.getReceiverDigitalAddress(),
                                               digitalCourtesySmsRequest.getMessageText()).retryWhen(DEFAULT_RETRY_STRATEGY))
//                       The SMS in sent, publish to Notification Tracker with next status -> SENT
                         .then(sqsService.send(notificationTrackerSqsName.statoSmsName(),
                                               new NotificationTrackerQueueDto(requestId,
                                                                               clientId,
                                                                               now(),
                                                                               INVIO_SMS,
                                                                               RETRY.getValue(),
                                                                               SENT.getValue(),
                                                                               // TODO: Populate GeneratedMessageDto
                                                                               // Use this syntax new GeneratedMessageDto().id("foo")
                                                                               // .location("bar").system("bla")
                                                                               new GeneratedMessageDto())))
//                       The maximum number of retries has ended
                         .onErrorResume(SnsSendException.SnsMaxRetriesExceededException.class,
                                        snsMaxRetriesExceeded -> smsRetriesExceeded(smsPresaInCaricoInfo));
    }

    private Mono<SendMessageResponse> smsRetriesExceeded(final SmsPresaInCaricoInfo smsPresaInCaricoInfo) {

        String requestId = smsPresaInCaricoInfo.getRequestIdx();
        String clientId = smsPresaInCaricoInfo.getXPagopaExtchCxId();

//      Retrieve current request to get the current status
        return gestoreRepositoryCall.getRichiesta(requestId)
//                                  Publish to Notification Tracker with next status -> ERROR
                                    .flatMap(requestDto -> sqsService.send(notificationTrackerSqsName.statoSmsName(),
                                                                           new NotificationTrackerQueueDto(requestId,
                                                                                                           clientId,
                                                                                                           now(),
                                                                                                           INVIO_SMS,
                                                                                                           requestDto.getStatusRequest(),
                                                                                                           ERROR.getValue(),
                                                                                                           // TODO: Populate
                                                                                                           //  GeneratedMessageDto
                                                                                                           // Use this syntax new
                                                                                                           // GeneratedMessageDto().id
                                                                                                           // ("foo").location("bar")
                                                                                                           // .system("bla")
                                                                                                           new GeneratedMessageDto())))
//                                  Publish to ERRORI SMS queue
                                    .then(sqsService.send(smsSqsQueueName.errorName(), smsPresaInCaricoInfo));
    }
}
