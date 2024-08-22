package it.pagopa.pn.ec.cartaceo.service;


import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.mapper.CartaceoMapper;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.MaxRetriesExceededException;
import it.pagopa.pn.ec.commons.exception.StatusToDeleteException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.model.pojo.MonoResultWrapper;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
import it.pagopa.pn.ec.commons.policy.Policy;
import it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.rest.call.upload.UploadCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.QueueOperationsService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.pdfraster.service.impl.DynamoPdfRasterServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsResponse;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoPaper;
import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.*;
import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.EXCEPTION;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.ReactorUtils.pullFromFluxUntilIsEmpty;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.CODE_TO_STATUS_MAP;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;

@Service
@CustomLog
public class CartaceoService extends PresaInCaricoService implements QueueOperationsService {


    private final SqsService sqsService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final AttachmentServiceImpl attachmentService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final CartaceoSqsQueueName cartaceoSqsQueueName;
    private final PaperMessageCall paperMessageCall;
    private final FileCall fileCall;
    private final DownloadCall downloadCall;
    private final UploadCall uploadCall;
    private final DynamoPdfRasterServiceImpl dynamoPdfRasterService;
    private final CartaceoMapper cartaceoMapper;
    private String idSaved;
    private final Semaphore semaphore;
    private final Retry LAVORAZIONE_RICHIESTA_RETRY_STRATEGY;

    protected CartaceoService(AuthService authService, SqsService sqsService, GestoreRepositoryCall gestoreRepositoryCall,
                              AttachmentServiceImpl attachmentService, NotificationTrackerSqsName notificationTrackerSqsName,
                              CartaceoSqsQueueName cartaceoSqsQueueName, PaperMessageCall paperMessageCall, FileCall fileCall, DownloadCall downloadCall, UploadCall uplpadCall, DynamoPdfRasterServiceImpl dynamoPdfRasterService, CartaceoMapper cartaceoMapper,
                              @Value("${lavorazione-cartaceo.max-thread-pool-size}") Integer maxThreadPoolSize,
                              @Value("${lavorazione-cartaceo.max-retry-attempts}") Long maxRetryAttempts,
                              @Value("${lavorazione-cartaceo.min-retry-backoff}") Long minRetryBackoff) {
        super(authService);
        this.sqsService = sqsService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.attachmentService = attachmentService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.cartaceoSqsQueueName = cartaceoSqsQueueName;
        this.paperMessageCall = paperMessageCall;
        this.fileCall = fileCall;
        this.downloadCall = downloadCall;
        this.uploadCall = uplpadCall;
        this.dynamoPdfRasterService = dynamoPdfRasterService;
        this.cartaceoMapper = cartaceoMapper;
        this.semaphore = new Semaphore(maxThreadPoolSize);
        this.LAVORAZIONE_RICHIESTA_RETRY_STRATEGY = Retry.backoff(maxRetryAttempts, Duration.ofSeconds(minRetryBackoff))
                .doBeforeRetry(retrySignal -> log.debug(SHORT_RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    throw new MaxRetriesExceededException();
                });
    }

    private final Retry PRESA_IN_CARICO_RETRY_STRATEGY = Retry.backoff(3, Duration.ofMillis(500))
            .doBeforeRetry(retrySignal -> log.debug(SHORT_RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()));

    @Override
    protected Mono<Void> specificPresaInCarico(PresaInCaricoInfo presaInCaricoInfo) {

        var cartaceoPresaInCaricoInfo = (CartaceoPresaInCaricoInfo) presaInCaricoInfo;
        var requestIdx = cartaceoPresaInCaricoInfo.getRequestIdx();
        var xPagopaExtchCxId = cartaceoPresaInCaricoInfo.getXPagopaExtchCxId();
        var attachmentsUri = getPaperUri(cartaceoPresaInCaricoInfo.getPaperEngageRequest().getAttachments());
        var paperNotificationRequest = cartaceoPresaInCaricoInfo.getPaperEngageRequest();
        String concatRequestId = concatRequestId(xPagopaExtchCxId, requestIdx);

        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PRESA_IN_CARICO_CARTACEO, presaInCaricoInfo);

        paperNotificationRequest.setRequestId(requestIdx);

        return attachmentService.getAllegatiPresignedUrlOrMetadata(attachmentsUri, presaInCaricoInfo.getXPagopaExtchCxId(), true)
                .retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY)
                .then(insertRequestFromCartaceo(paperNotificationRequest, xPagopaExtchCxId))
                .flatMap(requestDto -> sendNotificationOnStatusQueue(cartaceoPresaInCaricoInfo,
                        BOOKED.getStatusTransactionTableCompliant(),
                        new PaperProgressStatusDto()).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY))
//                              Publish to CARTACEO BATCH
                .flatMap(sendMessageResponse -> sendNotificationOnBatchQueue(cartaceoPresaInCaricoInfo).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY))
                .onErrorResume(SqsClientException.class,
                        sqsClientException -> sendNotificationOnStatusQueue(cartaceoPresaInCaricoInfo,
                                INTERNAL_ERROR.getStatusTransactionTableCompliant(),
                                new PaperProgressStatusDto()).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY).then(Mono.error(
                                sqsClientException)))
                .then()
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, PRESA_IN_CARICO_CARTACEO, result));
    }

    private ArrayList<String> getPaperUri(List<PaperEngageRequestAttachments> paperEngageRequestAttachments) {
        ArrayList<String> list = new ArrayList<>();
        if (!paperEngageRequestAttachments.isEmpty()) {
            for (PaperEngageRequestAttachments attachment : paperEngageRequestAttachments) {
                list.add(attachment.getUri());
            }
        }
        return list;
    }

    private Mono<RequestDto> insertRequestFromCartaceo(PaperEngageRequest paperNotificationRequest, String xPagopaExtchCxId) {
        String concatRequestId = concatRequestId(xPagopaExtchCxId, paperNotificationRequest.getRequestId());
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, INSERT_REQUEST_FROM_CARTACEO, paperNotificationRequest);
        return Mono.fromCallable(() -> {
                    var requestDto = new RequestDto();
                    requestDto.setRequestIdx(paperNotificationRequest.getRequestId());
                    requestDto.setClientRequestTimeStamp(paperNotificationRequest.getClientRequestTimeStamp());
                    requestDto.setxPagopaExtchCxId(xPagopaExtchCxId);
                    var requestPersonalDto = new RequestPersonalDto();
                    var paperRequestPersonalDto = new PaperRequestPersonalDto();

                    List<AttachmentsEngageRequestDto> attachmentsEngageRequestDto = new ArrayList<>();
                    if (!paperNotificationRequest.getAttachments().isEmpty()) {
                        for (PaperEngageRequestAttachments attachment : paperNotificationRequest.getAttachments()) {
                            AttachmentsEngageRequestDto attachments = new AttachmentsEngageRequestDto();
                            attachments.setUri(attachment.getUri());
                            attachments.setOrder(attachment.getOrder());
                            attachments.setDocumentType(attachment.getDocumentType());
                            attachments.setSha256(attachment.getSha256());
                            attachmentsEngageRequestDto.add(attachments);

                        }
                    }

                    paperRequestPersonalDto.setAttachments(attachmentsEngageRequestDto);

                    paperRequestPersonalDto.setReceiverName(paperNotificationRequest.getReceiverName());
                    paperRequestPersonalDto.setReceiverNameRow2(paperNotificationRequest.getReceiverNameRow2());
                    paperRequestPersonalDto.setReceiverAddress(paperNotificationRequest.getReceiverAddress());
                    paperRequestPersonalDto.setReceiverAddressRow2(paperNotificationRequest.getReceiverAddressRow2());
                    paperRequestPersonalDto.setReceiverCap(paperNotificationRequest.getReceiverCap());
                    paperRequestPersonalDto.setReceiverCity(paperNotificationRequest.getReceiverCity());
                    paperRequestPersonalDto.setReceiverCity2(paperNotificationRequest.getReceiverCity2());
                    paperRequestPersonalDto.setReceiverPr(paperNotificationRequest.getReceiverPr());
                    paperRequestPersonalDto.setReceiverCountry(paperNotificationRequest.getReceiverCountry());
                    paperRequestPersonalDto.setReceiverFiscalCode(paperNotificationRequest.getReceiverFiscalCode());
                    paperRequestPersonalDto.setSenderName(paperNotificationRequest.getSenderName());
                    paperRequestPersonalDto.setSenderAddress(paperNotificationRequest.getSenderAddress());
                    paperRequestPersonalDto.setSenderCity(paperNotificationRequest.getSenderCity());
                    paperRequestPersonalDto.setSenderPr(paperNotificationRequest.getSenderPr());
                    paperRequestPersonalDto.setSenderDigitalAddress(paperNotificationRequest.getSenderDigitalAddress());
                    paperRequestPersonalDto.setArName(paperNotificationRequest.getArName());
                    paperRequestPersonalDto.setArAddress(paperNotificationRequest.getArAddress());
                    paperRequestPersonalDto.setArCap(paperNotificationRequest.getArCap());
                    paperRequestPersonalDto.setArCity(paperNotificationRequest.getArCity());
                    requestPersonalDto.setPaperRequestPersonal(paperRequestPersonalDto);

                    var requestMetadataDto = new RequestMetadataDto();
                    var paperRequestMetadataDto = new PaperRequestMetadataDto();
                    paperRequestMetadataDto.setRequestPaId(paperNotificationRequest.getRequestPaId());
                    paperRequestMetadataDto.setIun(paperNotificationRequest.getIun());
                    paperRequestMetadataDto.setVas(paperNotificationRequest.getVas());
                    paperRequestMetadataDto.setPrintType(paperNotificationRequest.getPrintType());
                    paperRequestMetadataDto.setProductType(paperNotificationRequest.getProductType());
                    requestMetadataDto.setPaperRequestMetadata(paperRequestMetadataDto);

                    requestDto.setRequestPersonal(requestPersonalDto);
                    requestDto.setRequestMetadata(requestMetadataDto);
                    return requestDto;
                }).flatMap(gestoreRepositoryCall::insertRichiesta).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, INSERT_REQUEST_FROM_CARTACEO, result));
    }

    @Scheduled(cron = "${PnEcCronLavorazioneBatchCartaceo ?:0 */5 * * * *}")
    public void lavorazioneRichiestaBatch() {
        MDC.clear();
        sqsService.getMessages(cartaceoSqsQueueName.batchName(), CartaceoPresaInCaricoInfo.class)//
                .doOnNext(cartaceoPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(cartaceoSqsQueueName.batchName()//
                        , cartaceoPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                .flatMap(cartaceoPresaInCaricoInfoSqsMessageWrapper -> Mono.zip(Mono.just(cartaceoPresaInCaricoInfoSqsMessageWrapper.getMessage())
//
                        , lavorazioneRichiesta(cartaceoPresaInCaricoInfoSqsMessageWrapper.getMessageContent())))
                .flatMap(cartaceoPresaInCaricoInfoSqsMessageWrapper -> sqsService.deleteMessageFromQueue(
                        cartaceoPresaInCaricoInfoSqsMessageWrapper.getT1()
//
                        ,
                        cartaceoSqsQueueName.batchName()))
                .transform(pullFromFluxUntilIsEmpty())//
                .subscribe();
    }

    Mono<SendMessageResponse> lavorazioneRichiesta(final CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo) {
        MDC.put(MDC_CORR_ID_KEY, concatRequestId(cartaceoPresaInCaricoInfo.getXPagopaExtchCxId(), cartaceoPresaInCaricoInfo.getRequestIdx()));
        log.logStartingProcess(LAVORAZIONE_RICHIESTA_CARTACEO);

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        var paperEngageRequestSrc = cartaceoPresaInCaricoInfo.getPaperEngageRequest();
        var paperEngageRequestDst = cartaceoMapper.convert(paperEngageRequestSrc);

        var stepError = new StepError();
        stepError.setStep(PDF_RASTER_STEP);
        cartaceoPresaInCaricoInfo.setStepError(stepError);

        return MDCUtils.addMDCToContextAndExecute(chooseStep(cartaceoPresaInCaricoInfo, paperEngageRequestDst, paperEngageRequestSrc)
                // The maximum number of retries has ended
                .onErrorResume(MaxRetriesExceededException.class, cartaceoMaxRetriesExceeded ->
                        sendNotificationOnStatusQueue(cartaceoPresaInCaricoInfo,
                                RETRY.getStatusTransactionTableCompliant(),
                                new PaperProgressStatusDto())
                                // Publish to ERRORI PAPER queue
                                .then(sendNotificationOnErrorQueue(cartaceoPresaInCaricoInfo)))
                .doOnError(exception -> log.logEndingProcess(LAVORAZIONE_RICHIESTA_CARTACEO, false, exception.getMessage()))
                .doOnSuccess(result -> log.logEndingProcess(LAVORAZIONE_RICHIESTA_CARTACEO))
                .doFinally(signalType -> semaphore.release()));
    }


    @Scheduled(cron = "${PnEcCronGestioneRetryCartaceo ?:0 */5 * * * *}")
    void gestioneRetryCartaceoScheduler() {
        MDC.clear();
        idSaved = null;
        sqsService.getOneMessage(cartaceoSqsQueueName.errorName(), CartaceoPresaInCaricoInfo.class)
                .doOnNext(cartaceoPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(cartaceoSqsQueueName.errorName(),
                        cartaceoPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                .flatMap(cartaceoPresaInCaricoInfoSqsMessageWrapper -> gestioneRetryCartaceo(cartaceoPresaInCaricoInfoSqsMessageWrapper.getMessageContent(),
                        cartaceoPresaInCaricoInfoSqsMessageWrapper.getMessage()))
                .map(MonoResultWrapper::new)
                .defaultIfEmpty(new MonoResultWrapper<>(null))
                .repeat()
                .takeWhile(MonoResultWrapper::isNotEmpty)
                .subscribe();
    }

    private Mono<RequestDto> filterRequestCartaceo(final CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo) {
        String toDelete = "toDelete";
        var requestId = cartaceoPresaInCaricoInfo.getRequestIdx();
        var clientId = cartaceoPresaInCaricoInfo.getXPagopaExtchCxId();
        String concatRequestId = concatRequestId(clientId, requestId);
        Policy retryPolicies = new Policy();

        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, FILTER_REQUEST_CARTACEO, cartaceoPresaInCaricoInfo);

        return gestoreRepositoryCall.getRichiesta(clientId, requestId)
//              check status toDelete
                .filter(requestDto -> !Objects.equals(requestDto.getStatusRequest(), toDelete))
//              se status toDelete throw Error
                .switchIfEmpty(Mono.error(new StatusToDeleteException(requestId)))
//              check Id per evitare loop
                .filter(requestDto -> !Objects.equals(requestDto.getRequestIdx(), idSaved))
//              se il primo step, inizializza l'attributo retry
                .map(requestDto -> {
                    if (requestDto.getRequestMetadata().getRetry() == null) {
                        log.debug(RETRY_ATTEMPT, FILTER_REQUEST_CARTACEO, 0);
                        RetryDto retryDto = new RetryDto();
                        retryDto.setRetryPolicy(retryPolicies.getPolicy().get("PAPER"));
                        retryDto.setRetryStep(BigDecimal.ZERO);
                        var eventsList = requestDto.getRequestMetadata().getEventsList();
                        var lastRetryTimestamp = eventsList.stream()
                                .max(Comparator.comparing(eventsDto -> eventsDto.getPaperProgrStatus().getStatusDateTime()))
                                .map(eventsDto -> eventsDto.getPaperProgrStatus().getStatusDateTime()).get();
                        retryDto.setLastRetryTimestamp(lastRetryTimestamp);
                        requestDto.getRequestMetadata().setRetry(retryDto);

                    } else {
                        requestDto.getRequestMetadata().getRetry()
                                .setRetryStep(requestDto.getRequestMetadata()
                                        .getRetry()
                                        .getRetryStep()
                                        .add(BigDecimal.ONE));
                        var retryNumber = requestDto.getRequestMetadata().getRetry().getRetryStep();
                        log.debug(RETRY_ATTEMPT, FILTER_REQUEST_CARTACEO, retryNumber);
                    }
                    return requestDto;
                })
//              check retry policies
                .filter(requestDto -> {
                    var dateTime1 = requestDto.getRequestMetadata().getRetry().getLastRetryTimestamp();
                    var dateTime2 = OffsetDateTime.now();
                    Duration duration = Duration.between(dateTime1, dateTime2);
                    int step = requestDto.getRequestMetadata().getRetry().getRetryStep().intValueExact();
                    long minutes = duration.toSecondsPart() > 30 ? duration.truncatedTo(ChronoUnit.SECONDS).plusMinutes(1).toMinutes() : duration.toMinutes();
                    long minutesToCheck = requestDto.getRequestMetadata().getRetry().getRetryPolicy().get(step).longValue();
                    return minutes >= minutesToCheck;
                })
//              patch con orario attuale e dello step retry
                .flatMap(requestDto -> {
                    requestDto.getRequestMetadata().getRetry().setLastRetryTimestamp(OffsetDateTime.now());
                    PatchDto patchDto = new PatchDto();
                    RetryDto retryDto = requestDto.getRequestMetadata().getRetry();
                    patchDto.setRetry(retryDto);
                    cartaceoPresaInCaricoInfo.getStepError().setRetryStep(retryDto.getRetryStep());
                    return gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto);
                })
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, FILTER_REQUEST_CARTACEO, result));
    }

    private Mono<DeleteMessageResponse> checkTentativiEccessiviCartaceo(String requestId, RequestDto requestDto,
                                                                        final CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo,
                                                                        Message message) {
        if (idSaved == null) {
            idSaved = requestId;
        }
        var retry = requestDto.getRequestMetadata().getRetry();
        if (retry.getRetryStep().compareTo(BigDecimal.valueOf(retry.getRetryPolicy().size() - 1)) >= 0) {
            // operazioni per la rimozione del
            // messaggio
            return sendNotificationOnStatusQueue(cartaceoPresaInCaricoInfo, ERROR.getStatusTransactionTableCompliant(), new PaperProgressStatusDto())
                    .then(sendNotificationOnDlqErrorQueue(cartaceoPresaInCaricoInfo))
                    .then(deleteMessageFromErrorQueue(message));

        }
        return sendNotificationOnErrorQueue(cartaceoPresaInCaricoInfo).then(deleteMessageFromErrorQueue(message)).doOnSuccess(result->log.debug(MESSAGE_REMOVED_FROM_ERROR_QUEUE, cartaceoSqsQueueName.errorName()));
    }

    public Mono<SqsResponse> gestioneRetryCartaceo(final CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo//
            , Message message) {

        var requestId = cartaceoPresaInCaricoInfo.getRequestIdx();
        var clientId=cartaceoPresaInCaricoInfo.getXPagopaExtchCxId();
        String concatRequestId = concatRequestId(clientId, requestId);
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(GESTIONE_RETRY_CARTACEO);

        var paperEngageRequestSrc = cartaceoPresaInCaricoInfo.getPaperEngageRequest();
        var paperEngageRequestDst = cartaceoMapper.convert(paperEngageRequestSrc);

        Policy retryPolicies = new Policy();

        if (cartaceoPresaInCaricoInfo.getStepError() == null) {
            var stepError = new StepError();
            stepError.setStep(PDF_RASTER_STEP);
            cartaceoPresaInCaricoInfo.setStepError(stepError);
        }

        return MDCUtils.addMDCToContextAndExecute(filterRequestCartaceo(cartaceoPresaInCaricoInfo)
                .flatMap(requestDto -> chooseStep(cartaceoPresaInCaricoInfo, paperEngageRequestDst, paperEngageRequestSrc)
                        .then(Mono.defer(() -> deleteMessageFromErrorQueue(message)))
                        .onErrorResume(MaxRetriesExceededException.class, throwable -> checkTentativiEccessiviCartaceo(cartaceoPresaInCaricoInfo.getRequestIdx(), requestDto, cartaceoPresaInCaricoInfo, message))
                        .cast(SqsResponse.class))
                .switchIfEmpty(sqsService.changeMessageVisibility(cartaceoSqsQueueName.errorName(), retryPolicies.getPolicy().get("PAPER").get(0).intValueExact() * 54, message.receiptHandle()))
                .onErrorResume(StatusToDeleteException.class, exception -> {
                    log.debug(MESSAGE_REMOVED_FROM_ERROR_QUEUE, cartaceoSqsQueueName.errorName());
                    return sendNotificationOnStatusQueue(cartaceoPresaInCaricoInfo,
                            ERROR.getStatusTransactionTableCompliant(),
                            new PaperProgressStatusDto()).flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(message));
                })
                .onErrorResume(throwable -> sendNotificationOnStatusQueue(cartaceoPresaInCaricoInfo, INTERNAL_ERROR.getStatusTransactionTableCompliant(), new PaperProgressStatusDto())
                        .then(sendNotificationOnDlqErrorQueue(cartaceoPresaInCaricoInfo))
                        .then(deleteMessageFromErrorQueue(message)))
                .doOnError(exception -> log.logEndingProcess(GESTIONE_RETRY_CARTACEO, false, exception.getMessage()))
                .doOnSuccess(result -> log.logEndingProcess(GESTIONE_RETRY_CARTACEO)));
    }

    /**
     * Execute the process steps one by one, basing on the stepError attribute of CartaceoPresaInCaricoInfo.
     * This method loops over the steps until it reaches the final step or a failure.
     *
     * @param cartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo
     * @param paperEngageRequestDst     converted PaperEngageRequest
     * @param paperEngageRequestSrc     original PaperEngageRequest
     * @return Mono<SendMessageResponse> a mono containing a SendMessageResponse
     */
    private Mono<SendMessageResponse> chooseStep(final CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo, it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest paperEngageRequestDst, PaperEngageRequest paperEngageRequestSrc) {
        return Mono.defer(() -> {
                    switch (cartaceoPresaInCaricoInfo.getStepError().getStep()) {
                        case PDF_RASTER_STEP -> {
                            return pdfRasterStep(cartaceoPresaInCaricoInfo, paperEngageRequestDst, paperEngageRequestSrc)
                                    .doOnSuccess(result -> {
                                        if (result != null) {
                                            cartaceoPresaInCaricoInfo.getStepError().setStep(END);
                                        } else cartaceoPresaInCaricoInfo.getStepError().setStep(PUT_REQUEST_STEP);
                                    });
                        }
                        case PUT_REQUEST_STEP -> {
                            return putRequestStep(cartaceoPresaInCaricoInfo, paperEngageRequestDst).doOnNext(operationResultCodeResponse -> {
                                cartaceoPresaInCaricoInfo.getStepError().setOperationResultCodeResponse(operationResultCodeResponse);
                                cartaceoPresaInCaricoInfo.getStepError().setStep(NOTIFICATION_TRACKER_STEP);
                            });
                        }
                        case NOTIFICATION_TRACKER_STEP -> {
                            return notificationTrackerStep(cartaceoPresaInCaricoInfo, cartaceoPresaInCaricoInfo.getStepError().getOperationResultCodeResponse())
                                    .doOnNext(requestConversionDto -> cartaceoPresaInCaricoInfo.getStepError().setStep(END));
                        }
                        default -> {
                            cartaceoPresaInCaricoInfo.getStepError().setStep(EXCEPTION);
                            return Mono.just(SendMessageResponse.builder().build());
                        }
                    }
                })
                // Max tentativi: numero di step + 1
                .repeat(3 + 1, () -> cartaceoPresaInCaricoInfo.getStepError().getStep().ordinal() < END.ordinal())
                .then(Mono.just(SendMessageResponse.builder().build()));
    }

    /**
     * Check the existence of the request in the repository and submit the PaperEngageRequest to the Consolidatore service.
     *
     * @param cartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo
     * @param paperEngageRequestDst     the converted PaperEngageRequest
     * @return Mono<OperationResultCodeResponse> the result of the request submission to the Consolidatore service
     */
    private Mono<OperationResultCodeResponse> putRequestStep(CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo, it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest paperEngageRequestDst) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, CARTACEO_PUT_REQUEST_STEP, cartaceoPresaInCaricoInfo);
        return gestoreRepositoryCall.getRichiesta(cartaceoPresaInCaricoInfo.getXPagopaExtchCxId(), cartaceoPresaInCaricoInfo.getRequestIdx())
                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                .flatMap(requestDto -> paperMessageCall.putRequest(paperEngageRequestDst).retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, cartaceoPresaInCaricoInfo.getRequestIdx(), CARTACEO_PUT_REQUEST_STEP, result))
                .doOnError(MaxRetriesExceededException.class, throwable -> {
                    StepError stepError = new StepError();
                    stepError.setStep(PUT_REQUEST_STEP);
                    cartaceoPresaInCaricoInfo.setStepError(stepError);
                });
    }

    /**
     * Send the request to the NotificationTracker service, basing on the operationResultCodeResponse.
     *
     * @param cartaceoPresaInCaricoInfo   cartaceoPresaInCaricoInfo
     * @param operationResultCodeResponse the response containing the result code of the putRequest() operation
     * @return Mono<SendMessageResponse> a mono containing a SendMessageResponse
     */
    private Mono<SendMessageResponse> notificationTrackerStep(CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo, OperationResultCodeResponse operationResultCodeResponse) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, NOTIFICATION_TRACKER_STEP_CARTACEO, cartaceoPresaInCaricoInfo);
        return sendNotificationOnStatusQueue(cartaceoPresaInCaricoInfo, CODE_TO_STATUS_MAP.get(operationResultCodeResponse.getResultCode()), new PaperProgressStatusDto())
                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, cartaceoPresaInCaricoInfo.getRequestIdx(), NOTIFICATION_TRACKER_STEP_CARTACEO, result))
                .doOnError(MaxRetriesExceededException.class, throwable -> {
                    StepError stepError = new StepError();
                    stepError.setStep(NOTIFICATION_TRACKER_STEP);
                    stepError.setOperationResultCodeResponse(operationResultCodeResponse);
                    cartaceoPresaInCaricoInfo.setStepError(stepError);
                });
    }

    /**
     * Check if there are attachments to convert and process them.
     * If there is no attachment to convert, this method returns an empty Mono.
     *
     * @param cartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo
     * @param paperEngageRequestDst converted PaperEngageRequest
     * @param paperEngageRequestSrc original PaperEngageRequest
     * @return Mono<RequestConversionDto> a Mono containing the conversion request just submitted
     */
    private Mono<RequestConversionDto> pdfRasterStep(CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo, it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest paperEngageRequestDst, PaperEngageRequest paperEngageRequestSrc) {
        return Mono.justOrEmpty(paperEngageRequestDst.getAttachments())
                .flatMapMany(Flux::fromIterable)
                .filter(attachment -> attachment.getUri().contains("PN_NOTIFICATION_ATTACHMENTS"))
                .flatMap(attachment -> uploadAttachmentToConvert(cartaceoPresaInCaricoInfo, attachment))
                .collectList()
                .filter(attachmentsToConvert -> !attachmentsToConvert.isEmpty())
                .doOnDiscard(List.class, list -> log.debug("No attachments to convert were processed."))
                .flatMap(attachmentsToConvert -> {
                    RequestConversionDto requestConversionDto = new RequestConversionDto();
                    requestConversionDto.setRequestId(cartaceoPresaInCaricoInfo.getRequestIdx());
                    requestConversionDto.setOriginalRequest(paperEngageRequestSrc);
                    requestConversionDto.setAttachments(attachmentsToConvert);
                    requestConversionDto.setRequestTimestamp(OffsetDateTime.now());
                    return dynamoPdfRasterService.insertRequestConversion(requestConversionDto);
                })
                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                .doOnError(MaxRetriesExceededException.class, throwable -> {
                    StepError stepError = new StepError();
                    stepError.setStep(PDF_RASTER_STEP);
                    cartaceoPresaInCaricoInfo.setStepError(stepError);
                });
    }

    /**
     * Download an attachment from SafeStorage service and upload it with a new fileKey and new documentType.
     * @param cartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo
     * @param attachment the original attachment
     * @return Mono<AttachmentToConvertDto> the attachment to convert related info
     */
    private Mono<AttachmentToConvertDto> uploadAttachmentToConvert(CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo, it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequestAttachments attachment) {
        String originalFileKey = attachment.getUri().replace("safestorage://", "");
        log.debug("Processing attachment with originalFileKey: {}", originalFileKey);
        return fileCall.getFile(originalFileKey, cartaceoPresaInCaricoInfo.getXPagopaExtchCxId(), false)
                .zipWhen(fileDownloadResponse -> downloadCall.downloadFile(fileDownloadResponse.getDownload().getUrl()))
                .flatMap(tuple -> {
                    FileDownloadResponse fileDownloadResponse = tuple.getT1();
                    ByteArrayOutputStream downloadedFileStream = (ByteArrayOutputStream) tuple.getT2();
                    FileCreationRequest fileCreationRequest = new FileCreationRequest();
                    fileCreationRequest.setStatus(fileDownloadResponse.getDocumentStatus());
                    fileCreationRequest.setDocumentType(fileDownloadResponse.getDocumentType());
                    fileCreationRequest.setContentType(fileDownloadResponse.getContentType());
                    log.debug("Posting file for originalFileKey: {}", originalFileKey);
                    return Mono.zip(Mono.just(downloadedFileStream), fileCall.postFile(cartaceoPresaInCaricoInfo.getXPagopaExtchCxId(), fileDownloadResponse.getChecksum(), fileCreationRequest));
                })
                .flatMap(tuple -> {
                    ByteArrayOutputStream downloadedFileStream = tuple.getT1();
                    FileCreationResponse fileCreationResponse = tuple.getT2();
                    log.debug("Uploading file to new URL for originalFileKey: {}", originalFileKey);
                    return uploadCall.uploadFile(fileCreationResponse.getKey(), fileCreationResponse.getUploadUrl(), fileCreationResponse.getSecret(), "application/pdf", DocumentTypeConfiguration.ChecksumEnum.SHA256, attachment.getSha256(), downloadedFileStream.toByteArray())
                            .thenReturn(fileCreationResponse.getKey());
                })
                .flatMap(newFileKey -> {
                    log.debug("File uploaded successfully, creating AttachmentToConvertDto for originalFileKey: {}", originalFileKey);
                    AttachmentToConvertDto attachmentToConvertDto = new AttachmentToConvertDto()
                            .originalFileKey(originalFileKey)
                            .newFileKey(newFileKey);
                    return Mono.just(attachmentToConvertDto);
                });
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnErrorQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(cartaceoSqsQueueName.errorName(), presaInCaricoInfo);
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnStatusQueue(PresaInCaricoInfo presaInCaricoInfo, String status,
                                                                   PaperProgressStatusDto paperProgressStatusDto) {
        return sqsService.send(notificationTrackerSqsName.statoCartaceoName(),
                createNotificationTrackerQueueDtoPaper(presaInCaricoInfo, status, paperProgressStatusDto));
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnDlqErrorQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(cartaceoSqsQueueName.dlqErrorName(), presaInCaricoInfo);
    }

    @Override
    public Mono<DeleteMessageResponse> deleteMessageFromErrorQueue(Message message) {
        return sqsService.deleteMessageFromQueue(message, cartaceoSqsQueueName.errorName());
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnBatchQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(cartaceoSqsQueueName.batchName(), presaInCaricoInfo);
    }

}