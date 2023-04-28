package it.pagopa.pn.ec.sms.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.StatusToDeleteException;
import it.pagopa.pn.ec.commons.model.pojo.MonoResultWrapper;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
import it.pagopa.pn.ec.commons.policy.Policy;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.*;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sms.configurationproperties.SmsSqsQueueName;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital;
import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.NOTIFICATION_TRACKER_STEP;
import static it.pagopa.pn.ec.commons.service.SnsService.DEFAULT_RETRY_STRATEGY;
import static it.pagopa.pn.ec.commons.utils.ReactorUtils.pullFromMonoUntilIsEmpty;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.SMS;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.MessageContentTypeEnum.PLAIN;

@Service
@Slf4j
public class SmsService extends PresaInCaricoService implements QueueOperationsService {

    private final SqsService sqsService;
    private final SnsService snsService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final SmsSqsQueueName smsSqsQueueName;
    private final NotificationTrackerSqsName notificationTrackerSqsName;

    private String idSaved;

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

        log.info("<-- Start presa in carico SMS --> richiesta: {}", presaInCaricoInfo.getRequestIdx());
        var smsPresaInCaricoInfo = (SmsPresaInCaricoInfo) presaInCaricoInfo;

        var digitalCourtesySmsRequest = smsPresaInCaricoInfo.getDigitalCourtesySmsRequest();
        digitalCourtesySmsRequest.setRequestId(smsPresaInCaricoInfo.getRequestIdx());

//      Insert request from SMS request and publish to Notification Tracker with next status -> BOOKED
        return insertRequestFromSms(digitalCourtesySmsRequest, smsPresaInCaricoInfo.getXPagopaExtchCxId()).then(sendNotificationOnStatusQueue(smsPresaInCaricoInfo, BOOKED.getStatusTransactionTableCompliant(), new DigitalProgressStatusDto()))
//                                                                                                        Publish to SMS INTERACTIVE or
//                                                                                                        SMS BATCH
                                                                                                          .flatMap(sendMessageResponse -> {
                                                                                                              DigitalCourtesySmsRequest.QosEnum
                                                                                                                      qos =
                                                                                                                      smsPresaInCaricoInfo.getDigitalCourtesySmsRequest()
                                                                                                                                          .getQos();
                                                                                                              if (qos == INTERACTIVE) {
                                                                                                                  return sendNotificationOnInteractiveQueue(smsPresaInCaricoInfo);
                                                                                                              } else if (qos == BATCH) {
                                                                                                                  return sendNotificationOnBatchQueue(smsPresaInCaricoInfo);
                                                                                                              } else {
                                                                                                                  return Mono.empty();
                                                                                                              }
                                                                                                          })
                .onErrorResume(SqsClientException.class, sqsClientException->
                {
                    sendNotificationOnStatusQueue(smsPresaInCaricoInfo, INTERNAL_ERROR.getStatusTransactionTableCompliant(), new PaperProgressStatusDto());
                    return Mono.error(sqsClientException);
                })
                .then();
    }

    @SuppressWarnings("Duplicates")
    private Mono<RequestDto> insertRequestFromSms(final DigitalCourtesySmsRequest digitalCourtesySmsRequest, String xPagopaExtchCxId) {
        return Mono.fromCallable(() -> {
            var requestDto = new RequestDto();

            requestDto.setRequestIdx(digitalCourtesySmsRequest.getRequestId());
            requestDto.setClientRequestTimeStamp(digitalCourtesySmsRequest.getClientRequestTimeStamp());
            requestDto.setxPagopaExtchCxId(xPagopaExtchCxId);

            var requestPersonalDto = new RequestPersonalDto();
            var digitalRequestPersonalDto = new DigitalRequestPersonalDto();
            digitalRequestPersonalDto.setQos(DigitalRequestPersonalDto.QosEnum.valueOf(digitalCourtesySmsRequest.getQos().name()));
            digitalRequestPersonalDto.setReceiverDigitalAddress(digitalCourtesySmsRequest.getReceiverDigitalAddress());
            digitalRequestPersonalDto.setMessageText(digitalCourtesySmsRequest.getMessageText());
            digitalRequestPersonalDto.setSenderDigitalAddress(digitalCourtesySmsRequest.getSenderDigitalAddress());
            digitalRequestPersonalDto.setSubjectText("");
            requestPersonalDto.setDigitalRequestPersonal(digitalRequestPersonalDto);

            var requestMetadataDto = new RequestMetadataDto();
            var digitalRequestMetadataDto = new DigitalRequestMetadataDto();
            digitalRequestMetadataDto.setCorrelationId(digitalCourtesySmsRequest.getCorrelationId());
            digitalRequestMetadataDto.setEventType(digitalCourtesySmsRequest.getEventType());
            digitalRequestMetadataDto.setTags(digitalCourtesySmsRequest.getTags());
            digitalRequestMetadataDto.setMessageContentType(PLAIN);
            digitalRequestMetadataDto.setChannel(SMS);
            requestMetadataDto.setDigitalRequestMetadata(digitalRequestMetadataDto);

            requestDto.setRequestPersonal(requestPersonalDto);
            requestDto.setRequestMetadata(requestMetadataDto);
            return requestDto;
        }).flatMap(gestoreRepositoryCall::insertRichiesta);
    }

    @SqsListener(value = "${sqs.queue.sms.interactive-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    void lavorazioneRichiestaInteractive(final SmsPresaInCaricoInfo smsPresaInCaricoInfo, final Acknowledgment acknowledgment) {

        logIncomingMessage(smsSqsQueueName.interactiveName(), smsPresaInCaricoInfo);
        lavorazioneRichiesta(smsPresaInCaricoInfo).doOnNext(result -> acknowledgment.acknowledge()).subscribe();
    }

    @Scheduled(cron = "${cron.value.lavorazione-batch-sms}")
    void lavorazioneRichiestaBatch() {

        sqsService.getOneMessage(smsSqsQueueName.batchName(), SmsPresaInCaricoInfo.class)
                  .doOnNext(smsPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(smsSqsQueueName.batchName(),
                                                                                        smsPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                  .flatMap(smsPresaInCaricoInfoSqsMessageWrapper -> Mono.zip(Mono.just(smsPresaInCaricoInfoSqsMessageWrapper.getMessage()),
                                                                             lavorazioneRichiesta(smsPresaInCaricoInfoSqsMessageWrapper.getMessageContent())))
                  .flatMap(smsPresaInCaricoInfoSqsMessageWrapper -> sqsService.deleteMessageFromQueue(smsPresaInCaricoInfoSqsMessageWrapper.getT1(),
                                                                                                      smsSqsQueueName.batchName()))
                  .transform(pullFromMonoUntilIsEmpty())
                  .subscribe();
    }

    Mono<SendMessageResponse> lavorazioneRichiesta(final SmsPresaInCaricoInfo smsPresaInCaricoInfo) {
        log.info("<-- START LAVORAZIONE RICHIESTA SMS --> richiesta : {}", smsPresaInCaricoInfo.getRequestIdx());
//      Try to send SMS
        return snsService.send(smsPresaInCaricoInfo.getDigitalCourtesySmsRequest().getReceiverDigitalAddress(),
                               smsPresaInCaricoInfo.getDigitalCourtesySmsRequest().getMessageText())

//                       Retry to send SMS if fails
                         .retryWhen(DEFAULT_RETRY_STRATEGY)

//                        Set message id after send
                         .map(this::createGeneratedMessageDto)

//                       The SMS in sent, publish to Notification Tracker with next status -> SENT
                .flatMap(generatedMessageDto -> sendNotificationOnStatusQueue(smsPresaInCaricoInfo,
                        SENT.getStatusTransactionTableCompliant(),
                        new DigitalProgressStatusDto().generatedMessage(
                                generatedMessageDto))

//                                                                An error occurred during SQS publishing to the Notification Tracker ->
//                                                                Publish to Errori SMS queue and notify to retry update status only
                                                                   .onErrorResume(SqsClientException.class,
                                                                                  sqsPublishException -> {
                                                                       var stepError = new StepError();
                                                                       smsPresaInCaricoInfo.setStepError(stepError);
                                                                       smsPresaInCaricoInfo.getStepError().setNotificationTrackerError(NOTIFICATION_TRACKER_STEP);
                                                                       smsPresaInCaricoInfo.getStepError().setGeneratedMessageDto(generatedMessageDto);
                                                                       return sendNotificationOnErrorQueue(smsPresaInCaricoInfo);
                                                                   }))

//                       The maximum number of retries has ended
                .onErrorResume(SnsSendException.SnsMaxRetriesExceededException.class,
                        snsMaxRetriesExceeded -> sendNotificationOnStatusQueue(smsPresaInCaricoInfo,
                                RETRY.getStatusTransactionTableCompliant(),
                                new DigitalProgressStatusDto())

//                               Publish to ERRORI SMS queue
                                .then(sendNotificationOnErrorQueue(smsPresaInCaricoInfo)));
    }

    private GeneratedMessageDto createGeneratedMessageDto(PublishResponse publishResponse) {
        return new GeneratedMessageDto().id(publishResponse.messageId()).system("toBeDefined");
    }

    @Scheduled(cron = "${cron.value.gestione-retry-sms}")
    public void gestioneRetrySmsScheduler() {

        idSaved = null;
        sqsService.getOneMessage(smsSqsQueueName.errorName(), SmsPresaInCaricoInfo.class)
                  .doOnNext(smsPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(smsSqsQueueName.errorName(),
                                                                                        smsPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                  .flatMap(smsPresaInCaricoInfoSqsMessageWrapper -> gestioneRetrySms(smsPresaInCaricoInfoSqsMessageWrapper.getMessageContent(),
                                                                                     smsPresaInCaricoInfoSqsMessageWrapper.getMessage()))
                  .map(MonoResultWrapper::new)

                  .defaultIfEmpty(new MonoResultWrapper<>(null))
                  .repeat()
                  .takeWhile(MonoResultWrapper::isNotEmpty)
                  .subscribe();
    }

    private Mono<RequestDto> filterRequestSms(final SmsPresaInCaricoInfo smsPresaInCaricoInfo) {
        log.info("<-- START GESTIONE RETRY SMS--> richiesta : {}", smsPresaInCaricoInfo.getRequestIdx());
        Policy retryPolicies = new Policy();

        String toDelete = "toDelete";

        var requestId = smsPresaInCaricoInfo.getRequestIdx();
        var clientId = smsPresaInCaricoInfo.getXPagopaExtchCxId();

        return gestoreRepositoryCall.getRichiesta(clientId, requestId)
//              check status toDelete
                .filter(requestDto -> !Objects.equals(requestDto.getStatusRequest(), toDelete))
//              se status toDelete throw Error
                .switchIfEmpty(Mono.error(new StatusToDeleteException(requestId)))
//              check Id per evitare loop
                .filter(requestDto -> !Objects.equals(requestDto.getRequestIdx(), idSaved))
//              se il primo step, inizializza l'attributo retry
                .flatMap(requestDto -> {
                    if (requestDto.getRequestMetadata().getRetry() == null) {
                        log.debug("Primo tentativo di Retry");
                        RetryDto retryDto = new RetryDto();
                        log.debug("policy" + retryPolicies.getPolicy().get("SMS"));
                        retryDto.setRetryPolicy(retryPolicies.getPolicy().get("SMS"));
                        retryDto.setRetryStep(BigDecimal.ZERO);
                        retryDto.setLastRetryTimestamp(OffsetDateTime.now());
                        requestDto.getRequestMetadata().setRetry(retryDto);
                        PatchDto patchDto = new PatchDto();
                        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());
                        return gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto);

                    }

                    var retryNumber = requestDto.getRequestMetadata().getRetry().getRetryStep();
                    log.debug(retryNumber + " tentativo di Retry");
                    return Mono.just(requestDto);

                })
//              check retry policies
                .filter(requestDto -> {

                    var dateTime1 = requestDto.getRequestMetadata().getRetry().getLastRetryTimestamp();
                    var dateTime2 = OffsetDateTime.now();
                    Duration duration = Duration.between(dateTime1, dateTime2);
                    int step = requestDto.getRequestMetadata().getRetry().getRetryStep().intValueExact();
                    long minutes = duration.toMinutes();
                    long minutesToCheck =
                            requestDto.getRequestMetadata().getRetry().getRetryPolicy().get(step).longValue();
                    return minutes >= minutesToCheck;
                })
//              patch con orario attuale e dello step retry
                .flatMap(requestDto -> {
                    requestDto.getRequestMetadata().getRetry().setLastRetryTimestamp(OffsetDateTime.now());
                    requestDto.getRequestMetadata()
                            .getRetry()
                            .setRetryStep(requestDto.getRequestMetadata()
                                    .getRetry()
                                    .getRetryStep()
                                    .add(BigDecimal.ONE));
                    PatchDto patchDto = new PatchDto();
                    patchDto.setRetry(requestDto.getRequestMetadata().getRetry());
                    return gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto);
                });
    }

    private Mono<DeleteMessageResponse> checkTentativiEccessiviSms(String requestId, RequestDto requestDto, final SmsPresaInCaricoInfo smsPresaInCaricoInfo, Message message) {
        if (idSaved == null) {
            idSaved = requestId;
        }
        if (requestDto.getRequestMetadata()
                .getRetry()
                .getRetryStep()
                .compareTo(BigDecimal.valueOf(3)) > 0) {
            // operazioni per la rimozione del messaggio
            log.debug("Il messaggio è stato rimosso dalla coda d'errore per eccessivi " +
                    "tentativi: {}", smsSqsQueueName.errorName());
            return sendNotificationOnStatusQueue(smsPresaInCaricoInfo,
                    ERROR.getStatusTransactionTableCompliant(),
                    new DigitalProgressStatusDto().generatedMessage(
                            new GeneratedMessageDto()))
                    .flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(message));
        }
        return Mono.empty();
    }

    public Mono<DeleteMessageResponse> gestioneRetrySms(final SmsPresaInCaricoInfo smsPresaInCaricoInfo, Message message) {
            var requestId = smsPresaInCaricoInfo.getRequestIdx();
            return filterRequestSms(smsPresaInCaricoInfo)
//              Tentativo invio sms
                                    .flatMap(requestDto -> {
//              check step error per evitare null pointer
                                        if(smsPresaInCaricoInfo.getStepError() == null) {
                                            var stepError = new StepError();
                                            smsPresaInCaricoInfo.setStepError(stepError);
                                        }
//              check step error per evitare nuova chiamata verso sns
//              caso in cui è avvenuto un errore nella pubblicazione sul notification tracker,  The SMS in sent, publish to Notification Tracker with next status -> SENT
                                        if(Objects.equals(smsPresaInCaricoInfo.getStepError().getNotificationTrackerError(), NOTIFICATION_TRACKER_STEP)) {
                                            log.debug("requestDto Value: {}", requestDto.getRequestMetadata().getRetry());
                                            return sendNotificationOnStatusQueue(smsPresaInCaricoInfo,
                                                    SENT.getStatusTransactionTableCompliant(),
                                                    new DigitalProgressStatusDto().generatedMessage(smsPresaInCaricoInfo.getStepError().getGeneratedMessageDto()))
                                                    .flatMap(sendMessageResponse -> {
                                                log.debug("Il messaggio è stato gestito correttamente e rimosso dalla coda d'errore: {}",
                                                        smsSqsQueueName.errorName());
                                                return deleteMessageFromErrorQueue(message);
                                            }).onErrorResume(sqsPublishException -> checkTentativiEccessiviSms(requestId, requestDto, smsPresaInCaricoInfo, message));
                                        } else {
//                gestisco il caso retry a partire dall'invio a sns
                                        log.debug("requestDto Value: {}", requestDto.getRequestMetadata().getRetry());
                                        return snsService.send(smsPresaInCaricoInfo.getDigitalCourtesySmsRequest()
                                                                .getReceiverDigitalAddress(),
                                                        smsPresaInCaricoInfo.getDigitalCourtesySmsRequest().getMessageText())

                                                //                       Retry to send SMS if fails
                                                .retryWhen(DEFAULT_RETRY_STRATEGY)

                                                //                        Set message id after send
                                                .map(this::createGeneratedMessageDto)

                                                //                       The SMS in sent, publish to Notification Tracker with next status -> SENT
                                                .flatMap(generatedMessageDto -> sendNotificationOnStatusQueue(smsPresaInCaricoInfo,
                                                        SENT.getStatusTransactionTableCompliant(),
                                                        new DigitalProgressStatusDto().generatedMessage(
                                                                generatedMessageDto))
                                                ).flatMap(sendMessageResponse -> {
                                                    log.debug("Il messaggio è stato gestito correttamente e rimosso dalla coda d'errore: {}",
                                                            smsSqsQueueName.errorName());
                                                    return deleteMessageFromErrorQueue(message);
                                                }).onErrorResume(sqsPublishException -> checkTentativiEccessiviSms(requestId, requestDto, smsPresaInCaricoInfo, message));
                                        }
                                    })
//                                   Catch errore tirato per lo stato toDelete
                                    .onErrorResume(StatusToDeleteException.class, exception -> {
                                        log.debug("Il messaggio è stato rimosso dalla coda d'errore per status toDelete: {}",
                                                  smsSqsQueueName.errorName());
                                        return sendNotificationOnStatusQueue(smsPresaInCaricoInfo,
                                                DELETED.getStatusTransactionTableCompliant(),
                                                new DigitalProgressStatusDto().generatedMessage(
                                                        new GeneratedMessageDto()))
                                                         .flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(message));


                                    })
                    .onErrorResume(internalError -> sendNotificationOnStatusQueue(smsPresaInCaricoInfo, INTERNAL_ERROR.getStatusTransactionTableCompliant(), new DigitalProgressStatusDto())
                            .then(deleteMessageFromErrorQueue(message)));
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnStatusQueue(PresaInCaricoInfo presaInCaricoInfo, String status, DigitalProgressStatusDto digitalProgressStatusDto) {
        return sqsService.send(notificationTrackerSqsName.statoSmsName(),
                createNotificationTrackerQueueDtoDigital(presaInCaricoInfo,
                        status,
                        digitalProgressStatusDto));
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnErrorQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(smsSqsQueueName.errorName(), presaInCaricoInfo);
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnBatchQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(smsSqsQueueName.batchName(),
                presaInCaricoInfo);
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnInteractiveQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(smsSqsQueueName.interactiveName(),
                presaInCaricoInfo);
    }

    @Override
    public Mono<DeleteMessageResponse> deleteMessageFromErrorQueue(Message message) {
        return sqsService.deleteMessageFromQueue(message, smsSqsQueueName.errorName());
    }

}
