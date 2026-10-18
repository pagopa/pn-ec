package it.pagopa.pn.ec.whatsapp.service;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.commons.exception.whatsapp.WhatsappSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.StatusToDeleteException;
import it.pagopa.pn.ec.commons.model.pojo.MonoResultWrapper;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
import it.pagopa.pn.ec.commons.policy.Policy;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.*;
import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.whatsapp.model.pojo.WhatsappPresaInCaricoInfo;
import it.pagopa.pn.ec.util.LogSanitizer;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
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
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyWhatsappRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyWhatsappRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.WHATSAPP;

@Service
@CustomLog
public class WhatsappService extends PresaInCaricoService implements QueueOperationsService {

    private final SqsService sqsService;
    private final EumSocialService eumSocialService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final PnEcConfig.Whatsapp.SqsQueue whatsappSqsQueueName;
    private final PnEcConfig.NotificationTracker.SqsQueue notificationTrackerSqsName;
    private final Semaphore semaphore;
    static final String GENERATED_MESSAGE_SYSTEM = "aws-end-user-messaging-social";
    private String idSaved;
    private LogSanitizer logSanitizer;

    protected WhatsappService(AuthService authService, SqsService sqsService, EumSocialService eumSocialService,
                         GestoreRepositoryCall gestoreRepositoryCall,
                         PnEcConfig pnEcConfig, LogSanitizer logSanitizer) {
        super(authService);
        this.sqsService = sqsService;
        this.eumSocialService = eumSocialService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.notificationTrackerSqsName = pnEcConfig.getNotificationTracker().getSqsQueue();
        this.whatsappSqsQueueName = pnEcConfig.getWhatsapp().getSqsQueue();
        this.semaphore=new Semaphore(pnEcConfig.getWhatsapp().getMaxThreadPoolSize());
        this.logSanitizer = logSanitizer;
    }

    private static final Retry PRESA_IN_CARICO_RETRY_STRATEGY = Retry.backoff(3, Duration.ofMillis(500))
            .doBeforeRetry(retrySignal -> log.info("Retry number {}, caused by : {}", retrySignal.totalRetries(), retrySignal.failure().getMessage(), retrySignal.failure()));

    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {

        var whatsappPresaInCaricoInfo = (WhatsappPresaInCaricoInfo) presaInCaricoInfo;
        var requestIdx = whatsappPresaInCaricoInfo.getRequestIdx();
        var xPagopaExtchCxId = whatsappPresaInCaricoInfo.getXPagopaExtchCxId();
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PRESA_IN_CARICO_WHATSAPP, presaInCaricoInfo);

        var digitalCourtesyWhatsappRequest = whatsappPresaInCaricoInfo.getDigitalCourtesyWhatsappRequest();

        digitalCourtesyWhatsappRequest.setRequestId(requestIdx);

        return insertRequestFromWhatsapp(digitalCourtesyWhatsappRequest, xPagopaExtchCxId).flatMap(requestDto -> sendNotificationOnStatusQueue(
                                                                                        whatsappPresaInCaricoInfo,
                                                                                        BOOKED.getStatusTransactionTableCompliant(),
                                                                                        new DigitalProgressStatusDto()).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY))
                                                                                .flatMap(sendMessageResponse -> {
                                                                                    DigitalCourtesyWhatsappRequest.QosEnum qos =
                                                                                            whatsappPresaInCaricoInfo.getDigitalCourtesyWhatsappRequest()
                                                                                                                .getQos();
                                                                                    if (qos == INTERACTIVE) {
                                                                                        return sendNotificationOnInteractiveQueue(
                                                                                                whatsappPresaInCaricoInfo).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY);
                                                                                    } else if (qos == BATCH) {
                                                                                        return sendNotificationOnBatchQueue(
                                                                                                whatsappPresaInCaricoInfo).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY);
                                                                                    } else {
                                                                                        return Mono.empty();
                                                                                    }
                                                                                })
                                                                                .onErrorResume(SqsClientException.class,
                                                                                               sqsClientException -> sendNotificationOnStatusQueue(
                                                                                                       whatsappPresaInCaricoInfo,
                                                                                                       INTERNAL_ERROR.getStatusTransactionTableCompliant(),
                                                                                                       new DigitalProgressStatusDto()).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY)
                                                                                                       .then(Mono.error(sqsClientException)))
                                                                                .then()
                                                                                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PRESA_IN_CARICO_WHATSAPP, result));
    }

    @SuppressWarnings("Duplicates")
    private Mono<RequestDto> insertRequestFromWhatsapp(final DigitalCourtesyWhatsappRequest digitalCourtesyWhatsappRequest, String xPagopaExtchCxId) {
        return Mono.fromCallable(() -> {
            var requestDto = new RequestDto();

            requestDto.setRequestIdx(digitalCourtesyWhatsappRequest.getRequestId());
            requestDto.setClientRequestTimeStamp(digitalCourtesyWhatsappRequest.getClientRequestTimeStamp());
            requestDto.setxPagopaExtchCxId(xPagopaExtchCxId);

            var requestPersonalDto = new RequestPersonalDto();
            var digitalRequestPersonalDto = new DigitalRequestPersonalDto();
            digitalRequestPersonalDto.setQos(DigitalRequestPersonalDto.QosEnum.valueOf(digitalCourtesyWhatsappRequest.getQos().name()));
            digitalRequestPersonalDto.setReceiverDigitalAddress(digitalCourtesyWhatsappRequest.getReceiverDigitalAddress());
            digitalRequestPersonalDto.setMessageText(digitalCourtesyWhatsappRequest.getMessageText());
            digitalRequestPersonalDto.setSenderDigitalAddress(digitalCourtesyWhatsappRequest.getSenderDigitalAddress());
            digitalRequestPersonalDto.setSubjectText("");
            requestPersonalDto.setDigitalRequestPersonal(digitalRequestPersonalDto);

            var requestMetadataDto = new RequestMetadataDto();
            var digitalRequestMetadataDto = new DigitalRequestMetadataDto();
            digitalRequestMetadataDto.setCorrelationId(digitalCourtesyWhatsappRequest.getCorrelationId());
            digitalRequestMetadataDto.setEventType(digitalCourtesyWhatsappRequest.getEventType());
            digitalRequestMetadataDto.setTags(digitalCourtesyWhatsappRequest.getTags());
            digitalRequestMetadataDto.setMessageContentType(DigitalRequestMetadataDto.MessageContentTypeEnum.TEXT_PLAIN);
            digitalRequestMetadataDto.setChannel(WHATSAPP);
            requestMetadataDto.setDigitalRequestMetadata(digitalRequestMetadataDto);

            requestDto.setRequestPersonal(requestPersonalDto);
            requestDto.setRequestMetadata(requestMetadataDto);
            return requestDto;
        }).flatMap(gestoreRepositoryCall::insertRichiesta).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY)
        .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, INSERT_REQUEST_FROM_WHATSAPP, result));
    }

    @SqsListener(value = "${pn.ec.whatsapp.sqs-queue.interactive-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    void lavorazioneRichiestaInteractive(final WhatsappPresaInCaricoInfo whatsappPresaInCaricoInfo, final Acknowledgement acknowledgment) {
        MDC.clear();
        logIncomingMessage(whatsappSqsQueueName.getInteractiveName(), whatsappPresaInCaricoInfo);
        lavorazioneRichiesta(whatsappPresaInCaricoInfo).then(Mono.defer(() -> Mono.fromFuture(acknowledgment.acknowledgeAsync()))).block();
    }

    @Scheduled(fixedDelayString = "${pn.ec.delay.lavorazione-batch-whatsapp}")
    void lavorazioneRichiestaBatch() {
    	log.logStartingProcess(LAVORAZIONE_BATCH_WHATSAPP);
        MDC.clear();
        sqsService.getMessages(whatsappSqsQueueName.getBatchName(), WhatsappPresaInCaricoInfo.class)
                  .doOnNext(whatsappPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(whatsappSqsQueueName.getBatchName(),
                                                                                        whatsappPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                  .flatMap(whatsappPresaInCaricoInfoSqsMessageWrapper -> Mono.zip(Mono.just(whatsappPresaInCaricoInfoSqsMessageWrapper.getMessage()),
                                                                             lavorazioneRichiesta(whatsappPresaInCaricoInfoSqsMessageWrapper.getMessageContent())))
                  .flatMap(whatsappPresaInCaricoInfoSqsMessageWrapper -> sqsService.deleteMessageFromQueue(whatsappPresaInCaricoInfoSqsMessageWrapper.getT1(),
                                                                                                      whatsappSqsQueueName.getBatchName()))
                  .transform(pullFromFluxUntilIsEmpty())
                  .doOnError(e -> log.logEndingProcess(LAVORAZIONE_BATCH_WHATSAPP, false, e.getMessage(), e))
                  .doOnComplete(() -> log.logEndingProcess(LAVORAZIONE_BATCH_WHATSAPP))
                  .blockLast();
        
    }

    private static final Retry LAVORAZIONE_RICHIESTA_RETRY_STRATEGY = Retry.backoff(3, Duration.ofSeconds(2))
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                throw new WhatsappSendException.WhatsappMaxRetriesExceededException();
            })
            .doBeforeRetry(retrySignal -> log.info("Retry number {}, caused by : {}", retrySignal.totalRetries(), retrySignal.failure().getMessage(), retrySignal.failure()));

    Mono<SendMessageResponse> lavorazioneRichiesta(final WhatsappPresaInCaricoInfo whatsappPresaInCaricoInfo) {
        MDC.put(MDC_CORR_ID_KEY, concatRequestId(whatsappPresaInCaricoInfo.getXPagopaExtchCxId(), whatsappPresaInCaricoInfo.getRequestIdx()));
        log.logStartingProcess(LAVORAZIONE_RICHIESTA_WHATSAPP);

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

//      Try to send WhatsApp message
        return MDCUtils.addMDCToContextAndExecute(sendWhatsapp(whatsappPresaInCaricoInfo)

//                       Retry to send WhatsApp message if fails
                         .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)

//                        Set message id after send
                         .map(this::createGeneratedMessageDto)

//                       The WhatsApp message is sent, publish to Notification Tracker with next status -> SENT
                         .flatMap(generatedMessageDto -> sendNotificationOnStatusQueue(whatsappPresaInCaricoInfo,
                                                                                       SENT.getStatusTransactionTableCompliant(),
                                                                                       new DigitalProgressStatusDto().generatedMessage(
                                                                                               generatedMessageDto))

//                                                                An error occurred during SQS publishing to the Notification Tracker ->
//                                                                Publish to Errori WhatsApp queue and notify to retry update status only
.onErrorResume(SqsClientException.class, sqsPublishException -> {
    var stepError = new StepError();
    whatsappPresaInCaricoInfo.setStepError(stepError);
    whatsappPresaInCaricoInfo.getStepError().setStep(NOTIFICATION_TRACKER_STEP);
    whatsappPresaInCaricoInfo.getStepError().setGeneratedMessageDto(generatedMessageDto);
    return sendNotificationOnErrorQueue(whatsappPresaInCaricoInfo);
}))

//                       The maximum number of retries has ended
                         .onErrorResume(WhatsappSendException.WhatsappMaxRetriesExceededException.class,
                                        whatsappMaxRetriesExceeded -> sendNotificationOnStatusQueue(whatsappPresaInCaricoInfo,
                                                                                               RETRY.getStatusTransactionTableCompliant(),
                                                                                               new DigitalProgressStatusDto())

//                               Publish to ERRORI WhatsApp queue
                .then(sendNotificationOnErrorQueue(whatsappPresaInCaricoInfo)))
                .doOnError(throwable -> log.logEndingProcess(LAVORAZIONE_RICHIESTA_WHATSAPP, false,logSanitizer.sanitize(throwable.getMessage()), throwable))
                .doOnSuccess(result -> log.logEndingProcess(LAVORAZIONE_RICHIESTA_WHATSAPP))
                .doFinally(signalType -> semaphore.release()));
    }

    private GeneratedMessageDto createGeneratedMessageDto(String messageId) {
        return new GeneratedMessageDto().id(messageId).system(GENERATED_MESSAGE_SYSTEM);
    }

    /**
     * Valore restituito da Meta negli eventi di stato WhatsApp (biz_opaque_callback_data),
     * usato per correlare le ricevute alla richiesta (fase 2: gestione eventi di consegna).
     */
    static String callbackData(final WhatsappPresaInCaricoInfo whatsappPresaInCaricoInfo) {
        return concatRequestId(whatsappPresaInCaricoInfo.getXPagopaExtchCxId(), whatsappPresaInCaricoInfo.getRequestIdx());
    }

    private Mono<String> sendWhatsapp(final WhatsappPresaInCaricoInfo whatsappPresaInCaricoInfo) {
        var request = whatsappPresaInCaricoInfo.getDigitalCourtesyWhatsappRequest();
        return eumSocialService.sendTemplateMessage(request.getReceiverDigitalAddress(), request.getWhatsappTemplate(),
                callbackData(whatsappPresaInCaricoInfo));
    }

    @Scheduled(fixedDelayString = "${pn.ec.delay.gestione-retry-whatsapp}")
    public void gestioneRetryWhatsappScheduler() {
    	log.logStartingProcess(LAVORAZIONE_ERRORI_WHATSAPP);

        MDC.clear();
        idSaved = null;
        sqsService.getOneMessage(whatsappSqsQueueName.getErrorName(), WhatsappPresaInCaricoInfo.class)
                  .doOnNext(whatsappPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(whatsappSqsQueueName.getErrorName(),
                                                                                        whatsappPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                  .flatMap(whatsappPresaInCaricoInfoSqsMessageWrapper -> gestioneRetryWhatsapp(whatsappPresaInCaricoInfoSqsMessageWrapper.getMessageContent(),
                                                                                     whatsappPresaInCaricoInfoSqsMessageWrapper.getMessage()))
                  .map(MonoResultWrapper::new)
                  .doOnError(throwable -> log.error(GENERIC_ERROR, throwable, logSanitizer.sanitize(String.valueOf(throwable.getMessage()))))
                  // Restituiamo una DeleteMessageResponse vuota per non bloccare lo scaricamento dalla coda
                  .onErrorResume(throwable -> Mono.just(new MonoResultWrapper<>(DeleteMessageResponse.builder().build())))
                  .defaultIfEmpty(new MonoResultWrapper<>(null))
                  .repeat()
                  .takeWhile(MonoResultWrapper::isNotEmpty)
                  .doOnError(e -> log.logEndingProcess(LAVORAZIONE_ERRORI_WHATSAPP, false, e.getMessage(), e))
                  .doOnComplete(() -> log.logEndingProcess(LAVORAZIONE_ERRORI_WHATSAPP))
                  .blockLast();
    }

    private Mono<RequestDto> filterRequestWhatsapp(final WhatsappPresaInCaricoInfo whatsappPresaInCaricoInfo) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, FILTER_REQUEST_WHATSAPP, whatsappPresaInCaricoInfo);

        Policy retryPolicies = new Policy();

        String toDelete = "toDelete";

        var requestId = whatsappPresaInCaricoInfo.getRequestIdx();
        var clientId = whatsappPresaInCaricoInfo.getXPagopaExtchCxId();

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
                                            log.info(RETRY_ATTEMPT, FILTER_REQUEST_WHATSAPP, 0);
                                            RetryDto retryDto = new RetryDto();
                                            retryDto.setRetryPolicy(retryPolicies.getPolicy().get("WHATSAPP"));
                                            retryDto.setRetryStep(BigDecimal.ZERO);
                                            retryDto.setLastRetryTimestamp(OffsetDateTime.now());
                                            requestDto.getRequestMetadata().setRetry(retryDto);
                                            PatchDto patchDto = new PatchDto();
                                            patchDto.setRetry(requestDto.getRequestMetadata().getRetry());
                                            return gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto);

                                        }

                                        var retryNumber = requestDto.getRequestMetadata().getRetry().getRetryStep();
                                        log.info(RETRY_ATTEMPT, FILTER_REQUEST_WHATSAPP, retryNumber);
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
                                    .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, FILTER_REQUEST_WHATSAPP, result));
    }

    private Mono<DeleteMessageResponse> checkTentativiEccessiviWhatsapp(String requestId, RequestDto requestDto,
                                                                   final WhatsappPresaInCaricoInfo whatsappPresaInCaricoInfo, Message message) {
        if (idSaved == null) {
            idSaved = requestId;
        }
        var retry = requestDto.getRequestMetadata().getRetry();
        if (retry.getRetryStep().compareTo(BigDecimal.valueOf(retry.getRetryPolicy().size())) > 0) {
            // operazioni per la rimozione del messaggio
            log.debug(MESSAGE_REMOVED_FROM_ERROR_QUEUE, whatsappSqsQueueName.getErrorName());
            return sendNotificationOnStatusQueue(whatsappPresaInCaricoInfo,
                                                 ERROR.getStatusTransactionTableCompliant(),
                                                 new DigitalProgressStatusDto()).flatMap(
                    sendMessageResponse -> deleteMessageFromErrorQueue(message));
        }
        return Mono.empty();
    }

    public Mono<DeleteMessageResponse> gestioneRetryWhatsapp(final WhatsappPresaInCaricoInfo whatsappPresaInCaricoInfo, Message message) {
        var requestId = whatsappPresaInCaricoInfo.getRequestIdx();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId(whatsappPresaInCaricoInfo.getXPagopaExtchCxId(), requestId));
        log.logStartingProcess(GESTIONE_RETRY_WHATSAPP);

        return MDCUtils.addMDCToContextAndExecute(filterRequestWhatsapp(whatsappPresaInCaricoInfo)
//              Tentativo invio WhatsApp
.flatMap(requestDto -> {
//              check step error per evitare null pointer
    if (whatsappPresaInCaricoInfo.getStepError() == null) {
        var stepError = new StepError();
        whatsappPresaInCaricoInfo.setStepError(stepError);
    }
//              check step error per evitare nuova chiamata verso End User Messaging Social
//              caso in cui è avvenuto un errore nella pubblicazione sul notification tracker,  The WhatsApp message is sent, publish to Notification
//              Tracker with next status -> SENT
    if (Objects.equals(whatsappPresaInCaricoInfo.getStepError().getStep(), NOTIFICATION_TRACKER_STEP)) {
        return sendNotificationOnStatusQueue(whatsappPresaInCaricoInfo,
                                             SENT.getStatusTransactionTableCompliant(),
                                             new DigitalProgressStatusDto().generatedMessage(whatsappPresaInCaricoInfo.getStepError()
                                                                                                                 .getGeneratedMessageDto()))
                .flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(message)
                        .doOnSuccess(result->log.debug(MESSAGE_REMOVED_FROM_ERROR_QUEUE, whatsappSqsQueueName.getErrorName())))

                .onErrorResume(sqsPublishException -> {
                    log.warn(EXCEPTION_IN_PROCESS, GESTIONE_RETRY_WHATSAPP, sqsPublishException,
                             logSanitizer.sanitize(String.valueOf(sqsPublishException.getMessage())));
                    return checkTentativiEccessiviWhatsapp(requestId, requestDto, whatsappPresaInCaricoInfo, message);
                });
    } else {
//                gestisco il caso retry a partire dall'invio a End User Messaging Social
        return sendWhatsapp(whatsappPresaInCaricoInfo)

                         //                       Retry to send WhatsApp message if fails
                         .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)

                         //                        Set message id after send
                         .map(this::createGeneratedMessageDto)

                         //                       The WhatsApp message is sent, publish to Notification Tracker with next status -> SENT
                         .flatMap(generatedMessageDto -> sendNotificationOnStatusQueue(whatsappPresaInCaricoInfo,
                                                                                       SENT.getStatusTransactionTableCompliant(),
                                                                                       new DigitalProgressStatusDto().generatedMessage(
                                                                                               generatedMessageDto)))
                         .flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(message))
                         .doOnSuccess(result->log.debug(MESSAGE_REMOVED_FROM_ERROR_QUEUE, whatsappSqsQueueName.getErrorName()))
                         .onErrorResume(sqsPublishException -> {
                             log.warn(EXCEPTION_IN_PROCESS, GESTIONE_RETRY_WHATSAPP, sqsPublishException,
                                      logSanitizer.sanitize(String.valueOf(sqsPublishException.getMessage())));
                             return checkTentativiEccessiviWhatsapp(requestId,
                                     requestDto,
                                     whatsappPresaInCaricoInfo,
                                     message);
                         });
    }
})
// Se riceviamo un Mono.empty(), ritorniamo una DeleteMessageResponse vuota per evitare che
// lo schedulatore annulli lo scaricamento di messaggi dalla coda
.defaultIfEmpty(DeleteMessageResponse.builder().build())
//                                   Catch errore tirato per lo stato toDelete
.onErrorResume(StatusToDeleteException.class, exception -> sendNotificationOnStatusQueue(whatsappPresaInCaricoInfo,
                                     DELETED.getStatusTransactionTableCompliant(),
                                     new DigitalProgressStatusDto())
        .flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(message))
        .doOnSuccess(result->log.debug(MESSAGE_REMOVED_FROM_ERROR_QUEUE, whatsappSqsQueueName.getErrorName())))
.onErrorResume(internalError -> sendNotificationOnStatusQueue(whatsappPresaInCaricoInfo,
                                                              INTERNAL_ERROR.getStatusTransactionTableCompliant(),
                                                              new DigitalProgressStatusDto()).then(deleteMessageFromErrorQueue(message)))
                .doOnError(throwable -> log.logEndingProcess(GESTIONE_RETRY_WHATSAPP, false, logSanitizer.sanitize(throwable.getMessage()), throwable))
                .doOnSuccess(result -> log.logEndingProcess(GESTIONE_RETRY_WHATSAPP)));
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnStatusQueue(PresaInCaricoInfo presaInCaricoInfo, String status,
                                                                   DigitalProgressStatusDto digitalProgressStatusDto) {
        return sqsService.send(notificationTrackerSqsName.getStatoWhatsappName(),
                               createNotificationTrackerQueueDtoDigital(presaInCaricoInfo, status, digitalProgressStatusDto));
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnErrorQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(whatsappSqsQueueName.getErrorName(), presaInCaricoInfo);
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnBatchQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(whatsappSqsQueueName.getBatchName(), presaInCaricoInfo);
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnInteractiveQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(whatsappSqsQueueName.getInteractiveName(), presaInCaricoInfo);
    }

    @Override
    public Mono<DeleteMessageResponse> deleteMessageFromErrorQueue(Message message) {
        return sqsService.deleteMessageFromQueue(message, whatsappSqsQueueName.getErrorName());
    }

}
