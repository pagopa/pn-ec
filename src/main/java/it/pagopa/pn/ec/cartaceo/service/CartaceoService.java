package it.pagopa.pn.ec.cartaceo.service;


import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.mapper.CartaceoMapper;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.MaxRetriesExceededException;
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
import software.amazon.awssdk.services.sqs.model.SqsResponse;

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
import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.NOTIFICATION_TRACKER_STEP;
import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.PUT_REQUEST_STEP;
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
        }).flatMap(gestoreRepositoryCall::insertRichiesta).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY);
    }

    @Scheduled(cron = "${PnEcCronLavorazioneBatchCartaceo ?:0 */5 * * * *}")
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
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                throw new MaxRetriesExceededException();
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

        return putRequestStep(cartaceoPresaInCaricoInfo, paperEngageRequestDst)
                .doOnError(MaxRetriesExceededException.class, throwable -> {
                    StepError stepError = new StepError();
                    stepError.setStep(PUT_REQUEST_STEP);
                    cartaceoPresaInCaricoInfo.setStepError(stepError);
                })
                .flatMap(operationResultCodeResponse -> notificationTrackerStep(cartaceoPresaInCaricoInfo, operationResultCodeResponse)
                        .doOnError(MaxRetriesExceededException.class, throwable -> {
                            StepError stepError = new StepError();
                            stepError.setStep(NOTIFICATION_TRACKER_STEP);
                            stepError.setOperationResultCodeResponse(operationResultCodeResponse);
                            cartaceoPresaInCaricoInfo.setStepError(stepError);
                        }))
                // The maximum number of retries has ended
                .onErrorResume(MaxRetriesExceededException.class, cartaceoMaxRetriesExceeded ->
                        sendNotificationOnStatusQueue(cartaceoPresaInCaricoInfo,
                                RETRY.getStatusTransactionTableCompliant(),
                                new PaperProgressStatusDto())
                                // Publish to ERRORI PAPER queue
                                .then(sendNotificationOnErrorQueue(cartaceoPresaInCaricoInfo)))
                .doOnError(exception -> {log.warn("lavorazioneRichiesta {} {}", exception, exception.getMessage());})
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
                .map(requestDto -> {
                    if (requestDto.getRequestMetadata().getRetry() == null) {
                        log.debug("Primo tentativo di Retry");
                        RetryDto retryDto = new RetryDto();
                        retryDto.setRetryPolicy(retryPolicies.getPolicy().get("PAPER"));
                        retryDto.setRetryStep(BigDecimal.ZERO);
                        var eventsList = requestDto.getRequestMetadata().getEventsList();
                        var lastRetryTimestamp = eventsList.stream()
                                .max(Comparator.comparing(eventsDto -> eventsDto.getPaperProgrStatus().getStatusDateTime()))
                                .map(eventsDto -> eventsDto.getPaperProgrStatus().getStatusDateTime()).get();
                        retryDto.setLastRetryTimestamp(lastRetryTimestamp);
                        requestDto.getRequestMetadata().setRetry(retryDto);

                    } else
                        requestDto.getRequestMetadata().getRetry()
                                .setRetryStep(requestDto.getRequestMetadata()
                                        .getRetry()
                                        .getRetryStep()
                                        .add(BigDecimal.ONE));
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
                });
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
            log.debug("Il messaggio è stato rimosso " + "dalla coda d'errore per " + "eccessivi tentativi: {}",
                    cartaceoSqsQueueName.errorName());
            return sendNotificationOnDlqErrorQueue(cartaceoPresaInCaricoInfo).flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(message));

        }
        return sendNotificationOnErrorQueue(cartaceoPresaInCaricoInfo).then(deleteMessageFromErrorQueue(message));
    }

    public Mono<SqsResponse> gestioneRetryCartaceo(final CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo//
            , Message message) {
        var paperEngageRequestSrc = cartaceoPresaInCaricoInfo.getPaperEngageRequest();
        var paperEngageRequestDst = cartaceoMapper.convert(paperEngageRequestSrc);

        Policy retryPolicies = new Policy();

        if (cartaceoPresaInCaricoInfo.getStepError() == null) {
            var stepError = new StepError();
            stepError.setStep(PUT_REQUEST_STEP);
            cartaceoPresaInCaricoInfo.setStepError(stepError);
        }

        return filterRequestCartaceo(cartaceoPresaInCaricoInfo)
                .flatMap(requestDto -> chooseStep(cartaceoPresaInCaricoInfo, paperEngageRequestDst)
                        .repeatWhenEmpty(o -> o.doOnNext(iteration -> log.debug("Step repeated {} times for request {}", iteration, cartaceoPresaInCaricoInfo.getRequestIdx())))
                        .then(deleteMessageFromErrorQueue(message))
                        .onErrorResume(MaxRetriesExceededException.class, throwable -> checkTentativiEccessiviCartaceo(cartaceoPresaInCaricoInfo.getRequestIdx(), requestDto, cartaceoPresaInCaricoInfo, message)))
                .cast(SqsResponse.class)
                .switchIfEmpty(sqsService.changeMessageVisibility(cartaceoSqsQueueName.errorName(), retryPolicies.getPolicy().get("PAPER").get(0).intValueExact() * 54, message.receiptHandle()))
                .onErrorResume(StatusToDeleteException.class, exception -> {
                    log.debug("Il messaggio è stato rimosso dalla coda d'errore per status toDelete: {}", cartaceoSqsQueueName.errorName());
                    return sendNotificationOnStatusQueue(cartaceoPresaInCaricoInfo,
                            ERROR.getStatusTransactionTableCompliant(),
                            new PaperProgressStatusDto()).flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(message));
                })
                .onErrorResume(throwable -> {
                    log.error("Internal Error -> {}", throwable.getMessage());
                    return sendNotificationOnDlqErrorQueue(cartaceoPresaInCaricoInfo).flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(message));
                })
                .doOnError(exception -> log.warn("gestioneRetryCartaceo {} {}", exception, exception.getMessage()));
    }

    private Mono<SendMessageResponse> chooseStep(final CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo, it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest paperEngageRequest) {
        return Mono.just(cartaceoPresaInCaricoInfo.getStepError().getStep())
                .flatMap(step -> {
                    if (cartaceoPresaInCaricoInfo.getStepError().getStep().equals(NOTIFICATION_TRACKER_STEP)) {
                        log.debug("Retrying NotificationTracker step for request {}", cartaceoPresaInCaricoInfo.getRequestIdx());
                        return notificationTrackerStep(cartaceoPresaInCaricoInfo, cartaceoPresaInCaricoInfo.getStepError().getOperationResultCodeResponse());
                    } else {
                        log.debug("Retrying all steps for request {}", cartaceoPresaInCaricoInfo.getRequestIdx());
                        return putRequestStep(cartaceoPresaInCaricoInfo, paperEngageRequest).flatMap(operationResultCodeResponse -> {
                            cartaceoPresaInCaricoInfo.getStepError().setOperationResultCodeResponse(operationResultCodeResponse);
                            cartaceoPresaInCaricoInfo.getStepError().setStep(NOTIFICATION_TRACKER_STEP);
                            return Mono.empty();
                        });
                    }
                });
    }

    private Mono<OperationResultCodeResponse> putRequestStep(CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo, it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest paperEngageRequestDst) {
        return gestoreRepositoryCall.getRichiesta(cartaceoPresaInCaricoInfo.getXPagopaExtchCxId(), cartaceoPresaInCaricoInfo.getRequestIdx())
                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                .flatMap(requestDto -> paperMessageCall.putRequest(paperEngageRequestDst).retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY));
    }

    private Mono<SendMessageResponse> notificationTrackerStep(CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo, OperationResultCodeResponse operationResultCodeResponse) {
        return sendNotificationOnStatusQueue(cartaceoPresaInCaricoInfo,
                CODE_TO_STATUS_MAP.get(operationResultCodeResponse.getResultCode()), new PaperProgressStatusDto())
                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY);
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
