package it.pagopa.pn.ec.cartaceo.service;


import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.mapper.CartaceoMapper;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.SemaphoreException;
import it.pagopa.pn.ec.commons.exception.StatusToDeleteException;
import it.pagopa.pn.ec.commons.exception.cartaceo.CartaceoSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.model.pojo.MonoResultWrapper;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
import it.pagopa.pn.ec.commons.policy.Policy;
import it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.QueueOperationsService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoPaper;
import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.NOTIFICATION_TRACKER_STEP;
import static it.pagopa.pn.ec.commons.utils.ReactorUtils.pullFromFluxUntilIsEmpty;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.CODE_TO_STATUS_MAP;

@Service
@Slf4j
public class CartaceoService extends PresaInCaricoService implements QueueOperationsService {


    private final SqsService sqsService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final AttachmentServiceImpl attachmentService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final CartaceoSqsQueueName cartaceoSqsQueueName;
    private final PaperMessageCall paperMessageCall;
    private final CartaceoMapper cartaceoMapper;
    private String idSaved;
    private final Semaphore semaphore;

    protected CartaceoService(AuthService authService, SqsService sqsService, GestoreRepositoryCall gestoreRepositoryCall,
                              AttachmentServiceImpl attachmentService, NotificationTrackerSqsName notificationTrackerSqsName,
                              CartaceoSqsQueueName cartaceoSqsQueueName, PaperMessageCall paperMessageCall, CartaceoMapper cartaceoMapper, @Value("${lavorazione-cartaceo.max-thread-pool-size}") Integer maxThreadPoolSize) {
        super(authService);
        this.sqsService = sqsService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.attachmentService = attachmentService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.cartaceoSqsQueueName = cartaceoSqsQueueName;
        this.paperMessageCall = paperMessageCall;
        this.cartaceoMapper = cartaceoMapper;
        this.semaphore = new Semaphore(maxThreadPoolSize);
    }

    private final Retry PRESA_IN_CARICO_RETRY_STRATEGY = Retry.backoff(3, Duration.ofMillis(500))
            .doBeforeRetry(retrySignal -> log.debug("Retry number {}, caused by : {}", retrySignal.totalRetries(), retrySignal.failure().getMessage(), retrySignal.failure()));

    @Override
    protected Mono<Void> specificPresaInCarico(PresaInCaricoInfo presaInCaricoInfo) {

        var cartaceoPresaInCaricoInfo = (CartaceoPresaInCaricoInfo) presaInCaricoInfo;
        var requestIdx = cartaceoPresaInCaricoInfo.getRequestIdx();
        var xPagopaExtchCxId = cartaceoPresaInCaricoInfo.getXPagopaExtchCxId();
        var attachmentsUri = getPaperUri(cartaceoPresaInCaricoInfo.getPaperEngageRequest().getAttachments());
        var paperNotificationRequest = cartaceoPresaInCaricoInfo.getPaperEngageRequest();

        log.info("<-- START PRESA IN CARICO CARTACEO --> Request ID: {}, Client ID: {}", requestIdx, xPagopaExtchCxId);

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
                .then();
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
        log.info("<-- START INSERT REQUEST FROM CARTACEO --> Request ID: {}, Client ID: {}",
                paperNotificationRequest.getRequestId(),
                xPagopaExtchCxId);
        return Mono.fromCallable(() -> {
            var requestDto = new RequestDto();
            requestDto.setRequestIdx(paperNotificationRequest.getRequestId());
            requestDto.setClientRequestTimeStamp(paperNotificationRequest.getClientRequestTimeStamp());
            requestDto.setxPagopaExtchCxId(xPagopaExtchCxId);
            var requestPersonalDto = new RequestPersonalDto();
            var digitalRequestPersonalDto = new PaperRequestPersonalDto();

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

            digitalRequestPersonalDto.setAttachments(attachmentsEngageRequestDto);

            digitalRequestPersonalDto.setReceiverName(paperNotificationRequest.getReceiverName());
            digitalRequestPersonalDto.setReceiverNameRow2(paperNotificationRequest.getReceiverNameRow2());
            digitalRequestPersonalDto.setReceiverAddress(paperNotificationRequest.getReceiverAddress());
            digitalRequestPersonalDto.setReceiverAddressRow2(paperNotificationRequest.getReceiverAddressRow2());
            digitalRequestPersonalDto.setReceiverCap(paperNotificationRequest.getReceiverCap());
            digitalRequestPersonalDto.setReceiverCity(paperNotificationRequest.getReceiverCity());
            digitalRequestPersonalDto.setReceiverCity2(paperNotificationRequest.getReceiverCity2());
            digitalRequestPersonalDto.setReceiverPr(paperNotificationRequest.getReceiverPr());
            digitalRequestPersonalDto.setReceiverCountry(paperNotificationRequest.getReceiverCountry());
            digitalRequestPersonalDto.setReceiverFiscalCode(paperNotificationRequest.getReceiverFiscalCode());
            digitalRequestPersonalDto.setSenderName(paperNotificationRequest.getSenderName());
            digitalRequestPersonalDto.setSenderAddress(paperNotificationRequest.getSenderAddress());
            digitalRequestPersonalDto.setSenderCity(paperNotificationRequest.getSenderCity());
            digitalRequestPersonalDto.setSenderPr(paperNotificationRequest.getSenderPr());
            digitalRequestPersonalDto.setSenderDigitalAddress(paperNotificationRequest.getSenderDigitalAddress());
            digitalRequestPersonalDto.setArName(paperNotificationRequest.getArName());
            digitalRequestPersonalDto.setArAddress(paperNotificationRequest.getArAddress());
            digitalRequestPersonalDto.setArCap(paperNotificationRequest.getArCap());
            digitalRequestPersonalDto.setArCity(paperNotificationRequest.getArCity());
            requestPersonalDto.setPaperRequestPersonal(digitalRequestPersonalDto);

            var requestMetadataDto = new RequestMetadataDto();
            var digitalRequestMetadataDto = new PaperRequestMetadataDto();
            digitalRequestMetadataDto.setRequestPaId(paperNotificationRequest.getRequestPaId());
            digitalRequestMetadataDto.setIun(paperNotificationRequest.getIun());
            digitalRequestMetadataDto.setVas(paperNotificationRequest.getVas());
            digitalRequestMetadataDto.setPrintType(paperNotificationRequest.getPrintType());
            digitalRequestMetadataDto.setProductType(paperNotificationRequest.getProductType());
            requestMetadataDto.setPaperRequestMetadata(digitalRequestMetadataDto);

            requestDto.setRequestPersonal(requestPersonalDto);
            requestDto.setRequestMetadata(requestMetadataDto);
            return requestDto;
        }).flatMap(gestoreRepositoryCall::insertRichiesta).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY);
    }

    @Scheduled(cron = "${PnEcCronLavorazioneBatchPec ?:0 */5 * * * *}")
    public void lavorazioneRichiestaBatch() {
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

    Retry LAVORAZIONE_RICHIESTA_RETRY_STRATEGY = Retry.backoff(3, Duration.ofSeconds(2))
            //                                        .jitter(0.75)
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                throw new CartaceoSendException.CartaceoMaxRetriesExceededException();
            });

    Mono<SendMessageResponse> lavorazioneRichiesta(final CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo) {

        log.info("<-- START LAVORAZIONE RICHIESTA CARTACEO --> Request ID : {}, Client ID : {}",
                cartaceoPresaInCaricoInfo.getRequestIdx(),
                cartaceoPresaInCaricoInfo.getXPagopaExtchCxId());

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        var paperEngageRequestSrc = cartaceoPresaInCaricoInfo.getPaperEngageRequest();
        var paperEngageRequestDst = cartaceoMapper.convert(paperEngageRequestSrc);

        return gestoreRepositoryCall.getRichiesta(cartaceoPresaInCaricoInfo.getXPagopaExtchCxId(),
                        cartaceoPresaInCaricoInfo.getRequestIdx())
                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                .flatMap(requestDto ->
                        // Try to send PAPER
                        paperMessageCall.putRequest(
                                        paperEngageRequestDst)
                                .doOnError(exception -> {
                                    log.warn("Exception in lavorazioneRichiesta {} {}", exception, exception.getMessage());
                                })
                                .retryWhen(
                                        LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                                // The PAPER
                                // in sent,
                                // publish
                                // to
                                // Notification Tracker with next status ->
                                // SENT
                                .flatMap(
                                        operationResultCodeResponse -> sendNotificationOnStatusQueue(
                                                cartaceoPresaInCaricoInfo,
                                                CODE_TO_STATUS_MAP.get(
                                                        operationResultCodeResponse.getResultCode()),
                                                new PaperProgressStatusDto())

                                                // An error occurred
                                                // during PAPER send,
                                                // start retries
                                                .retryWhen(
                                                        LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)

                                                // An error occurred
                                                // during SQS publishing
                                                // to the Notification
                                                // Tracker -> Publish to
                                                // ERRORI PAPER queue and
                                                // notify to retry
                                                // update status only
                                                .doOnError(exception -> {
                                                    log.warn("Exception in lavorazioneRichiesta {} {}", exception, exception.getMessage());
                                                })
                                                .onErrorResume(
                                                        SqsClientException.class,
                                                        sqsPublishException -> {
                                                            var
                                                                    stepError =
                                                                    new StepError();
                                                            cartaceoPresaInCaricoInfo.setStepError(
                                                                    stepError);
                                                            cartaceoPresaInCaricoInfo.getStepError()
                                                                    .setStep(
                                                                            NOTIFICATION_TRACKER_STEP);
                                                            cartaceoPresaInCaricoInfo.getStepError()
                                                                    .setOperationResultCodeResponse(
                                                                            operationResultCodeResponse);
                                                            return sendNotificationOnErrorQueue(
                                                                    cartaceoPresaInCaricoInfo);
                                                        })

                                ))
                // The maximum number of retries has ended
                .onErrorResume(CartaceoSendException.CartaceoMaxRetriesExceededException.class//
                        , cartaceoMaxRetriesExceeded ->

                                sendNotificationOnStatusQueue(cartaceoPresaInCaricoInfo,
                                        RETRY.getStatusTransactionTableCompliant(),
                                        new PaperProgressStatusDto())

                                        // Publish to ERRORI PAPER queue
                                        .then(sendNotificationOnErrorQueue(cartaceoPresaInCaricoInfo)))
                .doOnError(exception -> {
                    log.warn("lavorazioneRichiesta {} {}", exception, exception.getMessage());
                })
                .doFinally(signalType -> semaphore.release());
    }

    @Scheduled(cron = "${PnEcCronGestioneRetryCartaceo ?:0 */5 * * * *}")
    void gestioneRetryCartaceoScheduler() {
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
        log.info("<-- START GESTIONE RETRY CARTACEO--> Request ID : {}, Client ID : {}",
                cartaceoPresaInCaricoInfo.getRequestIdx(),
                cartaceoPresaInCaricoInfo.getXPagopaExtchCxId());
        String toDelete = "toDelete";
        var requestId = cartaceoPresaInCaricoInfo.getRequestIdx();
        var clientId = cartaceoPresaInCaricoInfo.getXPagopaExtchCxId();
        Policy retryPolicies = new Policy();

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
                        retryDto.setRetryPolicy(retryPolicies.getPolicy().get("PAPER"));
                        retryDto.setRetryStep(BigDecimal.ZERO);
                        retryDto.setLastRetryTimestamp(OffsetDateTime.now());
                        requestDto.getRequestMetadata().setRetry(retryDto);
                        PatchDto patchDto = new PatchDto();
                        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());
                        return gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto);

                    } else {
                        var retryNumber = requestDto.getRequestMetadata().getRetry().getRetryStep();
                        log.debug(retryNumber + " tentativo di Retry");
                        return Mono.just(requestDto);
                    }
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

    private Mono<DeleteMessageResponse> checkTentativiEccessiviCartaceo(String requestId, RequestDto requestDto,
                                                                        final CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo,
                                                                        Message message) {
        if (idSaved == null) {
            idSaved = requestId;
        }
        var retry = requestDto.getRequestMetadata().getRetry();
        if (retry.getRetryStep().compareTo(BigDecimal.valueOf(retry.getRetryPolicy().size())) > 0) {
            // operazioni per la rimozione del
            // messaggio
            log.debug("Il messaggio è stato rimosso " + "dalla coda d'errore per " + "eccessivi tentativi: {}",
                    cartaceoSqsQueueName.errorName());
            return sendNotificationOnStatusQueue(cartaceoPresaInCaricoInfo,
                    ERROR.getStatusTransactionTableCompliant(),
                    new PaperProgressStatusDto()).flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(
                    message));

        }
        return Mono.empty();
    }

    public Mono<DeleteMessageResponse> gestioneRetryCartaceo(final CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo//
            , Message message) {
        var requestId = cartaceoPresaInCaricoInfo.getRequestIdx();
        var paperEngageRequestSrc = cartaceoPresaInCaricoInfo.getPaperEngageRequest();
        var paperEngageRequestDst = cartaceoMapper.convert(paperEngageRequestSrc);
        return filterRequestCartaceo(cartaceoPresaInCaricoInfo)
//              Tentativo invio cartaceo
                .flatMap(requestDto -> {
//           check step error per evitare null pointer
                    if (cartaceoPresaInCaricoInfo.getStepError() == null) {
                        var stepError = new StepError();
                        cartaceoPresaInCaricoInfo.setStepError(stepError);
                    }
//              check step error per evitare nuova chiamata verso consolidatore
//              caso in cui è avvenuto un errore nella pubblicazione sul notification tracker,  The PAPER in sent, publish to
//              Notification Tracker with next status
                    if (Objects.equals(cartaceoPresaInCaricoInfo.getStepError().getStep(), NOTIFICATION_TRACKER_STEP)) {
                        return sendNotificationOnStatusQueue(cartaceoPresaInCaricoInfo,
                                CODE_TO_STATUS_MAP.get(cartaceoPresaInCaricoInfo.getStepError()
                                        .getOperationResultCodeResponse()
                                        .getResultCode()),
                                new PaperProgressStatusDto()).flatMap(sendMessageResponse -> {
                            log.debug("Il messaggio è stato gestito " + "correttamente e rimosso dalla " + "coda" + " d'errore {}",
                                    cartaceoSqsQueueName.errorName());
                            return deleteMessageFromErrorQueue(message);
                        }).onErrorResume(sqsPublishException -> {
                            log.warn("Exception in gestioneRetryCartaceo {}, {}", sqsPublishException, sqsPublishException.getMessage());
                            return checkTentativiEccessiviCartaceo(requestId, requestDto, cartaceoPresaInCaricoInfo, message);
                        });
                    } else {
                        log.debug("requestDto Value: {}", requestDto.getRequestMetadata().getRetry());
                        // Tentativo invio
                        return paperMessageCall.putRequest(paperEngageRequestDst)
                                // The PAPER in sent, publish to Notification Tracker with next status ->
                                // SENT
                                .flatMap(operationResultCodeResponse -> sendNotificationOnStatusQueue(cartaceoPresaInCaricoInfo,
                                        CODE_TO_STATUS_MAP.get(
                                                operationResultCodeResponse.getResultCode()),
                                        new PaperProgressStatusDto()).flatMap(
                                                sendMessageResponse -> {
                                                    log.debug("Il messaggio è stato gestito " + "correttamente e rimosso dalla " + "coda" +
                                                            " d'errore {}", cartaceoSqsQueueName.errorName());
                                                    return deleteMessageFromErrorQueue(message);
                                                })
                                        .onErrorResume(
                                                sqsPublishException -> {
                                                    log.warn("Exception in gestioneRetryCartaceo {}, {}", sqsPublishException, sqsPublishException.getMessage());
                                                    return checkTentativiEccessiviCartaceo(
                                                            requestId,
                                                            requestDto,
                                                            cartaceoPresaInCaricoInfo,
                                                            message);
                                                }));
                    }
                })
//              Catch errore tirato per lo stato toDelete
                .onErrorResume(StatusToDeleteException.class, exception -> {
                    log.debug("Il messaggio è stato rimosso dalla coda d'errore per status toDelete: {}", cartaceoSqsQueueName.errorName());
                    return sendNotificationOnStatusQueue(cartaceoPresaInCaricoInfo,
                            ERROR.getStatusTransactionTableCompliant(),
                            new PaperProgressStatusDto()).flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(message));
                })
//Catch errore interno, pubblicazione sul notification tracker ed eliminazione dalla coda di errore.
                .onErrorResume(throwable -> {
                    log.error("Internal Error -> {}", throwable.getMessage());
                    return sendNotificationOnStatusQueue(cartaceoPresaInCaricoInfo,
                            INTERNAL_ERROR.getStatusTransactionTableCompliant(),
                            new PaperProgressStatusDto()).flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(message));
                })
                .doOnError(exception -> {
                    log.warn("gestioneRetryCartaceo {} {}", exception, exception.getMessage());
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
    public Mono<DeleteMessageResponse> deleteMessageFromErrorQueue(Message message) {
        return sqsService.deleteMessageFromQueue(message, cartaceoSqsQueueName.errorName());
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnBatchQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(cartaceoSqsQueueName.batchName(), presaInCaricoInfo);
    }

}
