package it.pagopa.pn.ec.pec.service.impl;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.aruba.ArubaCallMaxRetriesExceededException;
import it.pagopa.pn.ec.commons.exception.aruba.ArubaSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.StatusToDeleteException;
import it.pagopa.pn.ec.commons.model.pojo.MonoResultWrapper;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
import it.pagopa.pn.ec.commons.policy.Policy;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.QueueOperationsService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.commons.utils.EmailUtils;
import it.pagopa.pn.ec.pec.configurationproperties.PecSqsQueueName;
import it.pagopa.pn.ec.pec.model.pojo.ArubaSecretValue;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.library.pec.service.PnPecService;
import it.pec.bridgews.SendMail;
import it.pec.bridgews.SendMailResponse;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital;
import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.*;
import static it.pagopa.pn.ec.commons.utils.EmailUtils.getDomainFromAddress;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.ReactorUtils.pullFromFluxUntilIsEmpty;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.encodeMessageId;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.PEC;

@Service
@CustomLog
public class PecService extends PresaInCaricoService implements QueueOperationsService {

    private final SqsService sqsService;
    private final PnPecService pnPecService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final AttachmentServiceImpl attachmentService;
    private final DownloadCall downloadCall;
    private final ArubaSecretValue arubaSecretValue;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final PecSqsQueueName pecSqsQueueName;
    private final Semaphore semaphore;
    private String idSaved;

    protected PecService(AuthService authService, PnPecService pnPecService, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService
            , AttachmentServiceImpl attachmentService, DownloadCall downloadCall, ArubaSecretValue arubaSecretValue,
                         NotificationTrackerSqsName notificationTrackerSqsName, PecSqsQueueName pecSqsQueueName, @Value("${lavorazione-pec.max-thread-pool-size}") Integer maxThreadPoolSize) {
        super(authService);
        this.pnPecService = pnPecService;
        this.sqsService = sqsService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.attachmentService = attachmentService;
        this.downloadCall = downloadCall;
        this.arubaSecretValue = arubaSecretValue;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.pecSqsQueueName = pecSqsQueueName;
        this.semaphore = new Semaphore(maxThreadPoolSize);
    }

    private final Retry PRESA_IN_CARICO_RETRY_STRATEGY = Retry.backoff(3, Duration.ofMillis(500))
            .doBeforeRetry(retrySignal -> log.debug("Retry number {}, caused by : {}", retrySignal.totalRetries(), retrySignal.failure().getMessage(), retrySignal.failure()));


    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {

//      Cast PresaInCaricoInfo to specific PecPresaInCaricoInfo
        var pecPresaInCaricoInfo = (PecPresaInCaricoInfo) presaInCaricoInfo;
        var requestIdx = pecPresaInCaricoInfo.getRequestIdx();
        var xPagopaExtchCxId = pecPresaInCaricoInfo.getXPagopaExtchCxId();
        var digitalNotificationRequest = pecPresaInCaricoInfo.getDigitalNotificationRequest();

        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PRESA_IN_CARICO_PEC, presaInCaricoInfo);

        digitalNotificationRequest.setRequestId(requestIdx);

        return attachmentService.getAllegatiPresignedUrlOrMetadata(pecPresaInCaricoInfo.getDigitalNotificationRequest()
                        .getAttachmentUrls(), xPagopaExtchCxId, true)
                .retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY)
                .then(insertRequestFromPec(digitalNotificationRequest, xPagopaExtchCxId))

                .flatMap(requestDto -> sendNotificationOnStatusQueue(pecPresaInCaricoInfo,
                        BOOKED.getStatusTransactionTableCompliant(),
                        new DigitalProgressStatusDto()).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY))

                .flatMap(sendMessageResponse -> {
                    DigitalNotificationRequest.QosEnum qos = pecPresaInCaricoInfo.getDigitalNotificationRequest().getQos();
                    if (qos == INTERACTIVE) {
                        return sendNotificationOnInteractiveQueue(pecPresaInCaricoInfo);
                    } else if (qos == BATCH) {
                        return sendNotificationOnBatchQueue(pecPresaInCaricoInfo);
                    } else {
                        return Mono.empty();
                    }
                })
                .onErrorResume(SqsClientException.class,
                        sqsClientException -> sendNotificationOnStatusQueue(pecPresaInCaricoInfo,
                                INTERNAL_ERROR.getStatusTransactionTableCompliant(),
                                new DigitalProgressStatusDto()).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY).then(Mono.error(
                                sqsClientException)))
                .then()
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PRESA_IN_CARICO_PEC, result));
    }

    @SuppressWarnings("Duplicates")
    private Mono<RequestDto> insertRequestFromPec(final DigitalNotificationRequest digitalNotificationRequest, String xPagopaExtchCxId) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, INSERT_REQUEST_FROM_PEC, digitalNotificationRequest);
        return Mono.fromCallable(() -> {
            var requestDto = new RequestDto();
            requestDto.setRequestIdx(digitalNotificationRequest.getRequestId());
            requestDto.setClientRequestTimeStamp(digitalNotificationRequest.getClientRequestTimeStamp());
            requestDto.setxPagopaExtchCxId(xPagopaExtchCxId);

            var requestPersonalDto = new RequestPersonalDto();
            var digitalRequestPersonalDto = new DigitalRequestPersonalDto();
            digitalRequestPersonalDto.setQos(DigitalRequestPersonalDto.QosEnum.valueOf(digitalNotificationRequest.getQos().name()));
            digitalRequestPersonalDto.setReceiverDigitalAddress(digitalNotificationRequest.getReceiverDigitalAddress());
            digitalRequestPersonalDto.setMessageText(digitalNotificationRequest.getMessageText());
            digitalRequestPersonalDto.setSenderDigitalAddress(digitalNotificationRequest.getSenderDigitalAddress());
            digitalRequestPersonalDto.setSubjectText(digitalNotificationRequest.getSubjectText());
            digitalRequestPersonalDto.setAttachmentsUrls(digitalNotificationRequest.getAttachmentUrls());
            requestPersonalDto.setDigitalRequestPersonal(digitalRequestPersonalDto);

            var requestMetadataDto = new RequestMetadataDto();
            var digitalRequestMetadataDto = new DigitalRequestMetadataDto();
            digitalRequestMetadataDto.setCorrelationId(digitalNotificationRequest.getCorrelationId());
            digitalRequestMetadataDto.setEventType(digitalNotificationRequest.getEventType());
            digitalRequestMetadataDto.setTags(digitalNotificationRequest.getTags());
            digitalRequestMetadataDto.setChannel(PEC);
            digitalRequestMetadataDto.setMessageContentType(DigitalRequestMetadataDto.MessageContentTypeEnum.PLAIN);
            requestMetadataDto.setDigitalRequestMetadata(digitalRequestMetadataDto);

            requestDto.setRequestPersonal(requestPersonalDto);
            requestDto.setRequestMetadata(requestMetadataDto);
            return requestDto;
        }).flatMap(gestoreRepositoryCall::insertRichiesta).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY)
        .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, INSERT_REQUEST_FROM_PEC, result));
    }

    @SqsListener(value = "${sqs.queue.pec.interactive-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void lavorazioneRichiestaInteractive(final PecPresaInCaricoInfo pecPresaInCaricoInfo, final Acknowledgment acknowledgment) {
        MDC.clear();
        logIncomingMessage(pecSqsQueueName.interactiveName(), pecPresaInCaricoInfo);
        lavorazioneRichiesta(pecPresaInCaricoInfo).doOnNext(result -> acknowledgment.acknowledge()).subscribe();
    }

    @Scheduled(cron = "${PnEcCronLavorazioneBatchPec ?:0 */5 * * * *}")
    public void lavorazioneRichiestaBatch() {
        MDC.clear();
        sqsService.getMessages(pecSqsQueueName.batchName(), PecPresaInCaricoInfo.class)
                .doOnNext(pecPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(pecSqsQueueName.batchName(),
                        pecPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                .flatMap(pecPresaInCaricoInfoSqsMessageWrapper -> Mono.zip(Mono.just(pecPresaInCaricoInfoSqsMessageWrapper.getMessage()),
                        lavorazioneRichiesta(pecPresaInCaricoInfoSqsMessageWrapper.getMessageContent())))
                .flatMap(pecPresaInCaricoInfoSqsMessageWrapper -> sqsService.deleteMessageFromQueue(pecPresaInCaricoInfoSqsMessageWrapper.getT1(),
                        pecSqsQueueName.batchName()))
                .transform(pullFromFluxUntilIsEmpty())
                .subscribe();
    }

    private static final Retry LAVORAZIONE_RICHIESTA_RETRY_STRATEGY = Retry.backoff(3, Duration.ofSeconds(2))
            .doBeforeRetry(retrySignal -> log.debug(SHORT_RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()));

    Mono<SendMessageResponse> lavorazioneRichiesta(final PecPresaInCaricoInfo pecPresaInCaricoInfo) {
        var requestIdx = pecPresaInCaricoInfo.getRequestIdx();
        var xPagopaExtchCxId = pecPresaInCaricoInfo.getXPagopaExtchCxId();
        var digitalNotificationRequest = pecPresaInCaricoInfo.getDigitalNotificationRequest();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId(xPagopaExtchCxId, requestIdx));
        log.logStartingProcess(LAVORAZIONE_RICHIESTA_PEC);

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

//      Get attachment presigned url Flux
        return MDCUtils.addMDCToContextAndExecute(getAttachments(xPagopaExtchCxId, digitalNotificationRequest)

                .flatMap(this::downloadAttachment)

//                              Convert to Mono<List>
                .collectList()

//                              Create EmailField object with request info and attachments

                .flatMap(emailAttachments -> sendMail(xPagopaExtchCxId, requestIdx, digitalNotificationRequest, emailAttachments)
                        .onErrorResume(ArubaCallMaxRetriesExceededException.class, throwable -> {
                            var stepError = new StepError();
                            pecPresaInCaricoInfo.setStepError(stepError);
                            pecPresaInCaricoInfo.getStepError().setStep(ARUBA_SEND_MAIL_STEP);
                            return Mono.error(throwable);
                        }))

                .flatMap(generatedMessageDto -> gestoreRepositoryCall.setMessageIdInRequestMetadata(xPagopaExtchCxId, requestIdx)
                        .onErrorResume(throwable -> {
                            var stepError = new StepError();
                            pecPresaInCaricoInfo.setStepError(stepError);
                            pecPresaInCaricoInfo.getStepError().setStep(SET_MESSAGE_ID_STEP);
                            pecPresaInCaricoInfo.getStepError().setGeneratedMessageDto(generatedMessageDto);
                            return Mono.error(throwable);
                        })
                        .map(requestDto -> generatedMessageDto))

                .flatMap(generatedMessageDto -> sendMessage(generatedMessageDto, pecPresaInCaricoInfo)

//                                    An error occurred during SQS publishing to the Notification Tracker ->
//                                    Publish to Errori PEC queue and notify to retry update status only
                        .onErrorResume(SqsClientException.class, sqsPublishException -> {
                            var stepError = new StepError();
                            pecPresaInCaricoInfo.setStepError(stepError);
                            pecPresaInCaricoInfo.getStepError().setStep(NOTIFICATION_TRACKER_STEP);
                            pecPresaInCaricoInfo.getStepError().setGeneratedMessageDto(generatedMessageDto);
                            return sendNotificationOnErrorQueue(pecPresaInCaricoInfo);
                        }))

                .doOnError(throwable -> log.error(EXCEPTION_IN_PROCESS, LAVORAZIONE_RICHIESTA_PEC, throwable, throwable.getMessage()))

                .onErrorResume(throwable -> sendNotificationOnStatusQueue(pecPresaInCaricoInfo,
                        RETRY.getStatusTransactionTableCompliant(),
                        new DigitalProgressStatusDto())

                        .then(sendNotificationOnErrorQueue(pecPresaInCaricoInfo)))
                .doOnSuccess(result -> log.logEndingProcess(LAVORAZIONE_RICHIESTA_PEC))
                .doOnError(throwable -> log.logEndingProcess(LAVORAZIONE_RICHIESTA_PEC, false, throwable.getMessage()))
                .doFinally(signalType -> semaphore.release()));
    }


    private Flux<FileDownloadResponse> getAttachments(String xPagopaExtchCxId, DigitalNotificationRequest digitalNotificationRequest) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PEC_GET_ATTACHMENTS, digitalNotificationRequest);
        return attachmentService.getAllegatiPresignedUrlOrMetadata(digitalNotificationRequest.getAttachmentUrls(), xPagopaExtchCxId, false)
                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                .filter(fileDownloadResponse -> fileDownloadResponse.getDownload() != null)
                .doOnComplete(() -> log.info(SUCCESSFUL_OPERATION_ON_NO_RESULT_LABEL, digitalNotificationRequest.getRequestId(), PEC_GET_ATTACHMENTS));
    }

    private Mono<EmailAttachment> downloadAttachment(FileDownloadResponse fileDownloadResponse) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PEC_DOWNLOAD_ATTACHMENT, fileDownloadResponse);
        return downloadCall.downloadFile(fileDownloadResponse.getDownload().getUrl())
                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                .map(outputStream -> EmailAttachment.builder()
                        .nameWithExtension(
                                fileDownloadResponse.getKey())
                        .content(outputStream)
                        .build())
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PEC_DOWNLOAD_ATTACHMENT, result));
    }

    private Mono<GeneratedMessageDto> sendMail(String xPagopaExtchCxId, String requestIdx, DigitalNotificationRequest digitalNotificationRequest, List<EmailAttachment> attachments) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS + " - {}", PEC_SEND_MAIL, digitalNotificationRequest, attachments);
        return Mono.just(attachments).map(fileDownloadResponses -> EmailField.builder()
                        .msgId(encodeMessageId(xPagopaExtchCxId, requestIdx))
                        .from(arubaSecretValue.getPecUsername())
                        .to(digitalNotificationRequest.getReceiverDigitalAddress())
                        .subject(digitalNotificationRequest.getSubjectText())
                        .text(digitalNotificationRequest.getMessageText())
                        .contentType(digitalNotificationRequest.getMessageContentType()
                                .getValue())
                        .emailAttachments(fileDownloadResponses)
                        .build())
                .map(EmailUtils::getMimeMessageByteArray)
                .flatMap(pnPecService::sendMail)
                .map(this::createGeneratedMessageDto)
                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PEC_SEND_MAIL, result));
    }

    private Mono<GeneratedMessageDto> setMessageIdInRequestMetadata(String xPagopaExtchCxId, String requestIdx, GeneratedMessageDto generatedMessageDto) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS + " - {}", PEC_SET_MESSAGE_ID_IN_REQUEST_METADATA, requestIdx, generatedMessageDto);
        return gestoreRepositoryCall.setMessageIdInRequestMetadata(xPagopaExtchCxId, requestIdx)
                .map(requestDto -> generatedMessageDto)
                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PEC_SET_MESSAGE_ID_IN_REQUEST_METADATA, result));
    }

    private Mono<SendMessageResponse> sendMessage(GeneratedMessageDto generatedMessageDto, PecPresaInCaricoInfo pecPresaInCaricoInfo) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PEC_SEND_MESSAGE, pecPresaInCaricoInfo);
        return sendNotificationOnStatusQueue(pecPresaInCaricoInfo,
                SENT.getStatusTransactionTableCompliant(),
                new DigitalProgressStatusDto().generatedMessage(generatedMessageDto))
                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PEC_SEND_MESSAGE, result));
    }

    private GeneratedMessageDto createGeneratedMessageDto(String messageID) {
        return new GeneratedMessageDto().id(messageID).system(getDomainFromAddress(arubaSecretValue.getPecUsername()));
    }

    @Scheduled(cron = "${PnEcCronGestioneRetryPec ?:0 */5 * * * *}")
    void gestioneRetryPecScheduler() {
        MDC.clear();
        idSaved = null;
        sqsService.getOneMessage(pecSqsQueueName.errorName(), PecPresaInCaricoInfo.class)
                .doOnNext(pecPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(pecSqsQueueName.errorName(),
                        pecPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                .flatMap(pecPresaInCaricoInfoSqsMessageWrapper -> gestioneRetryPec(pecPresaInCaricoInfoSqsMessageWrapper.getMessageContent(),
                        pecPresaInCaricoInfoSqsMessageWrapper.getMessage()))
                .map(MonoResultWrapper::new)
                .defaultIfEmpty(new MonoResultWrapper<>(null))
                .repeat()
                .takeWhile(MonoResultWrapper::isNotEmpty)
                .subscribe();
    }

    private Mono<RequestDto> filterRequestPec(final PecPresaInCaricoInfo pecPresaInCaricoInfo) {

        Policy retryPolicies = new Policy();
        var requestIdx = pecPresaInCaricoInfo.getRequestIdx();
        var clientId=pecPresaInCaricoInfo.getXPagopaExtchCxId();
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, FILTER_REQUEST_PEC, pecPresaInCaricoInfo);

        var xPagopaExtchCxId = pecPresaInCaricoInfo.getXPagopaExtchCxId();
        String toDelete = "toDelete";
        return gestoreRepositoryCall.getRichiesta(xPagopaExtchCxId, requestIdx)
//              check status toDelete
                .filter(requestDto -> !Objects.equals(requestDto.getStatusRequest(), toDelete))
//              se status toDelete throw Error
                .switchIfEmpty(Mono.error(new StatusToDeleteException(requestIdx)))
//              check Id per evitare loop
                .filter(requestDto -> !Objects.equals(requestDto.getRequestIdx(), idSaved))
//              se il primo step, inizializza l'attributo retry
                .flatMap(requestDto -> {
                    if (requestDto.getRequestMetadata().getRetry() == null) {
                        log.debug(RETRY_ATTEMPT, FILTER_REQUEST_PEC, 0);
                        RetryDto retryDto = new RetryDto();
                        retryDto.setRetryPolicy(retryPolicies.getPolicy().get("PEC"));
                        retryDto.setRetryStep(BigDecimal.ZERO);
                        retryDto.setLastRetryTimestamp(OffsetDateTime.now());
                        requestDto.getRequestMetadata().setRetry(retryDto);
                        PatchDto patchDto = new PatchDto();
                        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());
                        return gestoreRepositoryCall.patchRichiesta(xPagopaExtchCxId, requestIdx, patchDto);

                    } else {
                        var retryNumber = requestDto.getRequestMetadata().getRetry().getRetryStep();
                        log.debug(RETRY_ATTEMPT, FILTER_REQUEST_PEC, retryNumber);
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
                    return gestoreRepositoryCall.patchRichiesta(xPagopaExtchCxId, requestIdx, patchDto);
                })
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, FILTER_REQUEST_PEC, result));
    }

    private Mono<DeleteMessageResponse> checkTentativiEccessiviPec(String requestIdx, RequestDto requestDto,
                                                                   final PecPresaInCaricoInfo pecPresaInCaricoInfo, Message message) {
        if (idSaved == null) {
            idSaved = requestIdx;
        }
        var retry = requestDto.getRequestMetadata().getRetry();
        if (retry.getRetryStep().compareTo(BigDecimal.valueOf(retry.getRetryPolicy().size())) > 0) {
            // operazioni per la rimozione del messaggio
            return sendNotificationOnStatusQueue(pecPresaInCaricoInfo,
                    ERROR.getStatusTransactionTableCompliant(),
                    new DigitalProgressStatusDto().generatedMessage(new GeneratedMessageDto())).flatMap(
                    sendMessageResponse -> deleteMessageFromErrorQueue(message)
                            .doOnSuccess(result -> log.debug(MESSAGE_REMOVED_FROM_ERROR_QUEUE, pecSqsQueueName.errorName())));

        }
        return Mono.empty();
    }

    public Mono<DeleteMessageResponse> gestioneRetryPec(final PecPresaInCaricoInfo pecPresaInCaricoInfo, Message message) {

        var requestIdx = pecPresaInCaricoInfo.getRequestIdx();
        var xPagopaExtchCxId = pecPresaInCaricoInfo.getXPagopaExtchCxId();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId(xPagopaExtchCxId, requestIdx));
        log.logStartingProcess(GESTIONE_RETRY_PEC);

        return MDCUtils.addMDCToContextAndExecute(filterRequestPec(pecPresaInCaricoInfo).flatMap(requestDto -> {
//            check step error per evitare null pointer
                    if (pecPresaInCaricoInfo.getStepError() == null) {
                        var stepError = new StepError();
                        pecPresaInCaricoInfo.setStepError(stepError);
                    }

                    var step = pecPresaInCaricoInfo.getStepError().getStep();

//            check step error per evitare nuova chiamata verso aruba
//              caso in cui è avvenuto un errore nella pubblicazione sul notification tracker,  The PEC in sent, publish to Notification
//              Tracker with next status -> SENT
                    if (Objects.equals(pecPresaInCaricoInfo.getStepError().getStep(), NOTIFICATION_TRACKER_STEP)) {
                        return sendNotificationOnStatusQueue(pecPresaInCaricoInfo,
                                SENT.getStatusTransactionTableCompliant(),
                                new DigitalProgressStatusDto().generatedMessage(pecPresaInCaricoInfo.getStepError()
                                        .getGeneratedMessageDto())).flatMap(
                                        sendMessageResponse -> {
                                            log.debug(MESSAGE_REMOVED_FROM_ERROR_QUEUE, pecSqsQueueName.errorName());
                                            return deleteMessageFromErrorQueue(message);
                                        })
                                .onErrorResume(
                                        sqsPublishException -> {
                                            log.warn(EXCEPTION_IN_PROCESS, GESTIONE_RETRY_PEC, sqsPublishException, sqsPublishException.getMessage());
                                            return checkTentativiEccessiviPec(
                                                    requestIdx,
                                                    requestDto,
                                                    pecPresaInCaricoInfo,
                                                    message);
                                        });
                    } else {
                        //Gestisco il caso retry a partire dalla gestione allegati
                        //Get attachment presigned url Flux
                        return attachmentService.getAllegatiPresignedUrlOrMetadata(pecPresaInCaricoInfo.getDigitalNotificationRequest()
                                                .getAttachmentUrls(),
                                        xPagopaExtchCxId,
                                        false)
                                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)

                                .filter(fileDownloadResponse -> fileDownloadResponse.getDownload() != null)

                                .flatMap(fileDownloadResponse -> downloadCall.downloadFile(fileDownloadResponse.getDownload()
                                                .getUrl())
                                        .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                                        .map(outputStream -> EmailAttachment.builder()
                                                .nameWithExtension(
                                                        fileDownloadResponse.getKey())
                                                .content(
                                                        outputStream)
                                                .build()))

//                              Convert to Mono<List>
                                .collectList()

//                              Create EmailField object with request info and attachments
                                .flatMap(attachments -> {
                                    if (step == null || Objects.equals(step, ARUBA_SEND_MAIL_STEP))
                                        return sendMail(xPagopaExtchCxId, requestIdx, pecPresaInCaricoInfo.getDigitalNotificationRequest(), attachments);
                                    else return Mono.just(pecPresaInCaricoInfo.getStepError().getGeneratedMessageDto());
                                })

                                .zipWhen(generatedMessageDto -> {
                                    if (step == null || Objects.equals(step, SET_MESSAGE_ID_STEP))
                                        return gestoreRepositoryCall.setMessageIdInRequestMetadata(xPagopaExtchCxId, requestIdx);
                                    else return Mono.just(pecPresaInCaricoInfo.getStepError().getGeneratedMessageDto());
                                })

                                .flatMap(objects -> sendNotificationOnStatusQueue(pecPresaInCaricoInfo,
                                        SENT.getStatusTransactionTableCompliant(),
                                        new DigitalProgressStatusDto().generatedMessage(
                                                objects.getT1())))

                                .flatMap(sendMessageResponse -> {
                                    log.debug(MESSAGE_REMOVED_FROM_ERROR_QUEUE, pecSqsQueueName.errorName());
                                    return deleteMessageFromErrorQueue(message);
                                })
                                .onErrorResume(sqsPublishException -> {
                                    log.warn(EXCEPTION_IN_PROCESS, GESTIONE_RETRY_PEC, sqsPublishException, sqsPublishException.getMessage());
                                    return checkTentativiEccessiviPec(requestIdx,
                                            requestDto,
                                            pecPresaInCaricoInfo,
                                            message);
                                });
                    }

                })
                //              Catch errore tirato per lo stato toDelete
                .onErrorResume(it.pagopa.pn.ec.commons.exception.StatusToDeleteException.class, statusToDeleteException -> {
                    log.debug(MESSAGE_REMOVED_FROM_ERROR_QUEUE, pecSqsQueueName.errorName());
                    return sendNotificationOnStatusQueue(pecPresaInCaricoInfo,
                            DELETED.getStatusTransactionTableCompliant(),
                            new DigitalProgressStatusDto().generatedMessage(
                                    new GeneratedMessageDto())).flatMap(
                            sendMessageResponse -> deleteMessageFromErrorQueue(message));

                }).onErrorResume(internalError -> {
                    log.warn(EXCEPTION_IN_PROCESS, GESTIONE_RETRY_PEC, internalError, internalError.getMessage());
                    return sendNotificationOnStatusQueue(pecPresaInCaricoInfo,
                            INTERNAL_ERROR.getStatusTransactionTableCompliant(),
                            new DigitalProgressStatusDto()).flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(
                            message));
                })
                .doOnError(throwable -> log.logEndingProcess(GESTIONE_RETRY_PEC, false, throwable.getMessage()))
                .doOnSuccess(result -> log.logEndingProcess(GESTIONE_RETRY_PEC)));
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnStatusQueue(PresaInCaricoInfo presaInCaricoInfo, String status,
                                                                   DigitalProgressStatusDto digitalProgressStatusDto) {
        return sqsService.send(notificationTrackerSqsName.statoPecName(),
                createNotificationTrackerQueueDtoDigital(presaInCaricoInfo, status, digitalProgressStatusDto));
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnErrorQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(pecSqsQueueName.errorName(), presaInCaricoInfo);
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnBatchQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(pecSqsQueueName.batchName(), presaInCaricoInfo);
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnInteractiveQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(pecSqsQueueName.interactiveName(), presaInCaricoInfo);
    }

    @Override
    public Mono<DeleteMessageResponse> deleteMessageFromErrorQueue(Message message) {
        return sqsService.deleteMessageFromQueue(message, pecSqsQueueName.errorName());
    }

}
