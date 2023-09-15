package it.pagopa.pn.ec.sms.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.SemaphoreException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital;
import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.NOTIFICATION_TRACKER_STEP;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.ReactorUtils.pullFromFluxUntilIsEmpty;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;
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
    private final Semaphore semaphore;
    private String idSaved;

    protected SmsService(AuthService authService, SqsService sqsService, SnsService snsService,
                         GestoreRepositoryCall gestoreRepositoryCall, NotificationTrackerSqsName notificationTrackerSqsName,
                         SmsSqsQueueName smsSqsQueueName, @Value("${lavorazione-sms.max-thread-pool-size}") Integer maxThreadPoolSize) {
        super(authService);
        this.sqsService = sqsService;
        this.snsService = snsService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.smsSqsQueueName = smsSqsQueueName;
        this.semaphore=new Semaphore(maxThreadPoolSize);
    }

    private final Retry PRESA_IN_CARICO_RETRY_STRATEGY = Retry.backoff(3, Duration.ofMillis(500))
            .doBeforeRetry(retrySignal -> log.debug("Retry number {}, caused by : {}", retrySignal.totalRetries(), retrySignal.failure().getMessage(), retrySignal.failure()));

    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {

        var smsPresaInCaricoInfo = (SmsPresaInCaricoInfo) presaInCaricoInfo;
        var requestIdx = smsPresaInCaricoInfo.getRequestIdx();
        var xPagopaExtchCxId = smsPresaInCaricoInfo.getXPagopaExtchCxId();
        String concatRequestId = concatRequestId(xPagopaExtchCxId, requestIdx);
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PRESA_IN_CARICO_SMS, presaInCaricoInfo);

        var digitalCourtesySmsRequest = smsPresaInCaricoInfo.getDigitalCourtesySmsRequest();

        digitalCourtesySmsRequest.setRequestId(requestIdx);

        return insertRequestFromSms(digitalCourtesySmsRequest, xPagopaExtchCxId).flatMap(requestDto -> sendNotificationOnStatusQueue(
                                                                                        smsPresaInCaricoInfo,
                                                                                        BOOKED.getStatusTransactionTableCompliant(),
                                                                                        new DigitalProgressStatusDto()).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY))
                                                                                .flatMap(sendMessageResponse -> {
                                                                                    DigitalCourtesySmsRequest.QosEnum qos =
                                                                                            smsPresaInCaricoInfo.getDigitalCourtesySmsRequest()
                                                                                                                .getQos();
                                                                                    if (qos == INTERACTIVE) {
                                                                                        return sendNotificationOnInteractiveQueue(
                                                                                                smsPresaInCaricoInfo).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY);
                                                                                    } else if (qos == BATCH) {
                                                                                        return sendNotificationOnBatchQueue(
                                                                                                smsPresaInCaricoInfo).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY);
                                                                                    } else {
                                                                                        return Mono.empty();
                                                                                    }
                                                                                })
                                                                                .onErrorResume(SqsClientException.class,
                                                                                               sqsClientException -> sendNotificationOnStatusQueue(
                                                                                                       smsPresaInCaricoInfo,
                                                                                                       INTERNAL_ERROR.getStatusTransactionTableCompliant(),
                                                                                                       new DigitalProgressStatusDto()).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY)
                                                                                                       .then(Mono.error(sqsClientException)))
                                                                                .then()
                                                                                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, PRESA_IN_CARICO_SMS, result));
    }

    @SuppressWarnings("Duplicates")
    private Mono<RequestDto> insertRequestFromSms(final DigitalCourtesySmsRequest digitalCourtesySmsRequest, String xPagopaExtchCxId) {
        String concatRequestId = concatRequestId(xPagopaExtchCxId, digitalCourtesySmsRequest.getRequestId());
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, INSERT_REQUEST_FROM_SMS, digitalCourtesySmsRequest);
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
        }).flatMap(gestoreRepositoryCall::insertRichiesta).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY)
        .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, INSERT_REQUEST_FROM_SMS, result));
    }

    @SqsListener(value = "${sqs.queue.sms.interactive-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    void lavorazioneRichiestaInteractive(final SmsPresaInCaricoInfo smsPresaInCaricoInfo, final Acknowledgment acknowledgment) {

        logIncomingMessage(smsSqsQueueName.interactiveName(), smsPresaInCaricoInfo);
        lavorazioneRichiesta(smsPresaInCaricoInfo).doOnNext(result -> acknowledgment.acknowledge()).subscribe();
    }

    @Scheduled(cron = "${PnEcCronLavorazioneBatchSms ?:0 */5 * * * *}")
    void lavorazioneRichiestaBatch() {

        sqsService.getMessages(smsSqsQueueName.batchName(), SmsPresaInCaricoInfo.class)
                  .doOnNext(smsPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(smsSqsQueueName.batchName(),
                                                                                        smsPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                  .flatMap(smsPresaInCaricoInfoSqsMessageWrapper -> Mono.zip(Mono.just(smsPresaInCaricoInfoSqsMessageWrapper.getMessage()),
                                                                             lavorazioneRichiesta(smsPresaInCaricoInfoSqsMessageWrapper.getMessageContent())))
                  .flatMap(smsPresaInCaricoInfoSqsMessageWrapper -> sqsService.deleteMessageFromQueue(smsPresaInCaricoInfoSqsMessageWrapper.getT1(),
                                                                                                      smsSqsQueueName.batchName()))
                  .transform(pullFromFluxUntilIsEmpty())
                  .subscribe();
    }

    Retry LAVORAZIONE_RICHIESTA_RETRY_STRATEGY = Retry.backoff(3, Duration.ofSeconds(2))
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                throw new SnsSendException.SnsMaxRetriesExceededException();
            })
            .doBeforeRetry(retrySignal -> log.info("Retry number {}, caused by : {}", retrySignal.totalRetries(), retrySignal.failure().getMessage(), retrySignal.failure()));

    Mono<SendMessageResponse> lavorazioneRichiesta(final SmsPresaInCaricoInfo smsPresaInCaricoInfo) {
        String concatRequestId = concatRequestId(smsPresaInCaricoInfo.getXPagopaExtchCxId(), smsPresaInCaricoInfo.getRequestIdx());
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, LAVORAZIONE_RICHIESTA_SMS, smsPresaInCaricoInfo);

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

//      Try to send SMS
        return snsService.send(smsPresaInCaricoInfo.getDigitalCourtesySmsRequest().getReceiverDigitalAddress(),
                               smsPresaInCaricoInfo.getDigitalCourtesySmsRequest().getMessageText())

//                       Retry to send SMS if fails
                         .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)

//                        Set message id after send
                         .map(this::createGeneratedMessageDto)

//                       The SMS in sent, publish to Notification Tracker with next status -> SENT
                         .flatMap(generatedMessageDto -> sendNotificationOnStatusQueue(smsPresaInCaricoInfo,
                                                                                       SENT.getStatusTransactionTableCompliant(),
                                                                                       new DigitalProgressStatusDto().generatedMessage(
                                                                                               generatedMessageDto))

//                                                                An error occurred during SQS publishing to the Notification Tracker ->
//                                                                Publish to Errori SMS queue and notify to retry update status only
.onErrorResume(SqsClientException.class, sqsPublishException -> {
    var stepError = new StepError();
    smsPresaInCaricoInfo.setStepError(stepError);
    smsPresaInCaricoInfo.getStepError().setStep(NOTIFICATION_TRACKER_STEP);
    smsPresaInCaricoInfo.getStepError().setGeneratedMessageDto(generatedMessageDto);
    return sendNotificationOnErrorQueue(smsPresaInCaricoInfo);
}))

//                       The maximum number of retries has ended
                         .onErrorResume(SnsSendException.SnsMaxRetriesExceededException.class,
                                        snsMaxRetriesExceeded -> sendNotificationOnStatusQueue(smsPresaInCaricoInfo,
                                                                                               RETRY.getStatusTransactionTableCompliant(),
                                                                                               new DigitalProgressStatusDto())

//                               Publish to ERRORI SMS queue
                .then(sendNotificationOnErrorQueue(smsPresaInCaricoInfo)))
                .doOnError(throwable -> log.warn(EXCEPTION_IN_PROCESS_FOR, LAVORAZIONE_RICHIESTA_SMS, concatRequestId, throwable, throwable.getMessage()))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, LAVORAZIONE_RICHIESTA_SMS   , result))
                .doFinally(signalType -> semaphore.release());
    }

    private GeneratedMessageDto createGeneratedMessageDto(PublishResponse publishResponse) {
        return new GeneratedMessageDto().id(publishResponse.messageId()).system("toBeDefined");
    }

    @Scheduled(cron = "${PnEcCronGestioneRetrySms ?:0 */5 * * * *}")
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
        String concatRequestId = concatRequestId(smsPresaInCaricoInfo.getXPagopaExtchCxId(), smsPresaInCaricoInfo.getRequestIdx());
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, FILTER_REQUEST_SMS, smsPresaInCaricoInfo);
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
                                            log.debug(RETRY_ATTEMPT, FILTER_REQUEST_SMS, 0, concatRequestId);
                                            RetryDto retryDto = new RetryDto();
                                            retryDto.setRetryPolicy(retryPolicies.getPolicy().get("SMS"));
                                            retryDto.setRetryStep(BigDecimal.ZERO);
                                            retryDto.setLastRetryTimestamp(OffsetDateTime.now());
                                            requestDto.getRequestMetadata().setRetry(retryDto);
                                            PatchDto patchDto = new PatchDto();
                                            patchDto.setRetry(requestDto.getRequestMetadata().getRetry());
                                            return gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto);

                                        }

                                        var retryNumber = requestDto.getRequestMetadata().getRetry().getRetryStep();
                                        log.debug(RETRY_ATTEMPT, FILTER_REQUEST_SMS, retryNumber, concatRequestId);
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
                                    })
                                    .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, FILTER_REQUEST_SMS, result));
    }

    private Mono<DeleteMessageResponse> checkTentativiEccessiviSms(String requestId, RequestDto requestDto,
                                                                   final SmsPresaInCaricoInfo smsPresaInCaricoInfo, Message message) {
        String concatRequestId = concatRequestId(smsPresaInCaricoInfo.getXPagopaExtchCxId(), smsPresaInCaricoInfo.getRequestIdx());
        if (idSaved == null) {
            idSaved = requestId;
        }
        var retry = requestDto.getRequestMetadata().getRetry();
        if (retry.getRetryStep().compareTo(BigDecimal.valueOf(retry.getRetryPolicy().size())) > 0) {
            // operazioni per la rimozione del messaggio
            log.debug(MESSAGE_REMOVED_FROM_ERROR_QUEUE, concatRequestId, smsSqsQueueName.errorName());
            return sendNotificationOnStatusQueue(smsPresaInCaricoInfo,
                                                 ERROR.getStatusTransactionTableCompliant(),
                                                 new DigitalProgressStatusDto().generatedMessage(new GeneratedMessageDto())).flatMap(
                    sendMessageResponse -> deleteMessageFromErrorQueue(message));
        }
        return Mono.empty();
    }

    public Mono<DeleteMessageResponse> gestioneRetrySms(final SmsPresaInCaricoInfo smsPresaInCaricoInfo, Message message) {
        var requestId = smsPresaInCaricoInfo.getRequestIdx();
        String concatRequestId = concatRequestId(smsPresaInCaricoInfo.getXPagopaExtchCxId(), requestId);
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, GESTIONE_RETRY_SMS, smsPresaInCaricoInfo);

        return filterRequestSms(smsPresaInCaricoInfo)
//              Tentativo invio sms
.flatMap(requestDto -> {
//              check step error per evitare null pointer
    if (smsPresaInCaricoInfo.getStepError() == null) {
        var stepError = new StepError();
        smsPresaInCaricoInfo.setStepError(stepError);
    }
//              check step error per evitare nuova chiamata verso sns
//              caso in cui è avvenuto un errore nella pubblicazione sul notification tracker,  The SMS in sent, publish to Notification
//              Tracker with next status -> SENT
    if (Objects.equals(smsPresaInCaricoInfo.getStepError().getStep(), NOTIFICATION_TRACKER_STEP)) {
        return sendNotificationOnStatusQueue(smsPresaInCaricoInfo,
                                             SENT.getStatusTransactionTableCompliant(),
                                             new DigitalProgressStatusDto().generatedMessage(smsPresaInCaricoInfo.getStepError()
                                                                                                                 .getGeneratedMessageDto()))
                .flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(message)
                        .doOnNext(result->log.debug(MESSAGE_REMOVED_FROM_ERROR_QUEUE, concatRequestId, smsSqsQueueName.errorName())))

                .onErrorResume(sqsPublishException -> {
                    log.warn(EXCEPTION_IN_PROCESS_FOR, GESTIONE_RETRY_SMS, concatRequestId, sqsPublishException, sqsPublishException.getMessage());
                    return checkTentativiEccessiviSms(requestId, requestDto, smsPresaInCaricoInfo, message);
                });
    } else {
//                gestisco il caso retry a partire dall'invio a sns
        return snsService.send(smsPresaInCaricoInfo.getDigitalCourtesySmsRequest().getReceiverDigitalAddress(),
                               smsPresaInCaricoInfo.getDigitalCourtesySmsRequest().getMessageText())

                         //                       Retry to send SMS if fails
                         .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)

                         //                        Set message id after send
                         .map(this::createGeneratedMessageDto)

                         //                       The SMS in sent, publish to Notification Tracker with next status -> SENT
                         .flatMap(generatedMessageDto -> sendNotificationOnStatusQueue(smsPresaInCaricoInfo,
                                                                                       SENT.getStatusTransactionTableCompliant(),
                                                                                       new DigitalProgressStatusDto().generatedMessage(
                                                                                               generatedMessageDto)))
                         .flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(message))
                         .doOnNext(result->log.debug(MESSAGE_REMOVED_FROM_ERROR_QUEUE, concatRequestId, smsSqsQueueName.errorName()))
                         .onErrorResume(sqsPublishException -> {
                             log.warn(EXCEPTION_IN_PROCESS_FOR, GESTIONE_RETRY_SMS, concatRequestId, sqsPublishException, sqsPublishException.getMessage());
                             return checkTentativiEccessiviSms(requestId,
                                     requestDto,
                                     smsPresaInCaricoInfo,
                                     message);
                         });
    }
})
//                                   Catch errore tirato per lo stato toDelete
.onErrorResume(StatusToDeleteException.class, exception -> sendNotificationOnStatusQueue(smsPresaInCaricoInfo,
                                     DELETED.getStatusTransactionTableCompliant(),
                                     new DigitalProgressStatusDto().generatedMessage(new GeneratedMessageDto()))
        .flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(message))
        .doOnNext(result->log.debug(MESSAGE_REMOVED_FROM_ERROR_QUEUE, concatRequestId, smsSqsQueueName.errorName())))
.onErrorResume(internalError -> sendNotificationOnStatusQueue(smsPresaInCaricoInfo,
                                                              INTERNAL_ERROR.getStatusTransactionTableCompliant(),
                                                              new DigitalProgressStatusDto()).then(deleteMessageFromErrorQueue(message)))
                .doOnError(throwable -> log.warn(EXCEPTION_IN_PROCESS_FOR, GESTIONE_RETRY_SMS, concatRequestId, throwable, throwable.getMessage()))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, GESTIONE_RETRY_SMS, result));
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnStatusQueue(PresaInCaricoInfo presaInCaricoInfo, String status,
                                                                   DigitalProgressStatusDto digitalProgressStatusDto) {
        return sqsService.send(notificationTrackerSqsName.statoSmsName(),
                               createNotificationTrackerQueueDtoDigital(presaInCaricoInfo, status, digitalProgressStatusDto));
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnErrorQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(smsSqsQueueName.errorName(), presaInCaricoInfo);
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnBatchQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(smsSqsQueueName.batchName(), presaInCaricoInfo);
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnInteractiveQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(smsSqsQueueName.interactiveName(), presaInCaricoInfo);
    }

    @Override
    public Mono<DeleteMessageResponse> deleteMessageFromErrorQueue(Message message) {
        return sqsService.deleteMessageFromQueue(message, smsSqsQueueName.errorName());
    }

}
