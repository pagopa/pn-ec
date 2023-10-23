package it.pagopa.pn.ec.pec.service.impl;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.ChooseStepException;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.MaxRetriesExceededException;
import it.pagopa.pn.ec.commons.exception.aruba.ArubaSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.StatusToDeleteException;
import it.pagopa.pn.ec.commons.model.pojo.MonoResultWrapper;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
import it.pagopa.pn.ec.commons.policy.Policy;
import it.pagopa.pn.ec.commons.rest.call.aruba.ArubaCall;
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
import it.pec.bridgews.SendMail;
import it.pec.bridgews.SendMailResponse;
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
import java.util.Comparator;
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
@Slf4j
public class PecService extends PresaInCaricoService implements QueueOperationsService {

    private final SqsService sqsService;
    private final ArubaCall arubaCall;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final AttachmentServiceImpl attachmentService;
    private final DownloadCall downloadCall;
    private final ArubaSecretValue arubaSecretValue;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final PecSqsQueueName pecSqsQueueName;
    private final Semaphore semaphore;
    private String idSaved;

    protected PecService(AuthService authService, ArubaCall arubaCall, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService
            , AttachmentServiceImpl attachmentService, DownloadCall downloadCall, ArubaSecretValue arubaSecretValue,
                         NotificationTrackerSqsName notificationTrackerSqsName, PecSqsQueueName pecSqsQueueName, @Value("${lavorazione-pec.max-thread-pool-size}") Integer maxThreadPoolSize) {
        super(authService);
        this.arubaCall = arubaCall;
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
            .doBeforeRetry(retrySignal -> log.debug(SHORT_RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()));


    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {

//      Cast PresaInCaricoInfo to specific PecPresaInCaricoInfo
        var pecPresaInCaricoInfo = (PecPresaInCaricoInfo) presaInCaricoInfo;
        var requestIdx = pecPresaInCaricoInfo.getRequestIdx();
        var xPagopaExtchCxId = pecPresaInCaricoInfo.getXPagopaExtchCxId();
        var digitalNotificationRequest = pecPresaInCaricoInfo.getDigitalNotificationRequest();
        String concatRequestId = concatRequestId(xPagopaExtchCxId, requestIdx);

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
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, PRESA_IN_CARICO_PEC, result));
    }

    @SuppressWarnings("Duplicates")
    private Mono<RequestDto> insertRequestFromPec(final DigitalNotificationRequest digitalNotificationRequest, String xPagopaExtchCxId) {
        String concatRequestId = concatRequestId(xPagopaExtchCxId, digitalNotificationRequest.getRequestId());
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
        .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, INSERT_REQUEST_FROM_PEC, result));
    }

    @SqsListener(value = "${sqs.queue.pec.interactive-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void lavorazioneRichiestaInteractive(final PecPresaInCaricoInfo pecPresaInCaricoInfo, final Acknowledgment acknowledgment) {
        logIncomingMessage(pecSqsQueueName.interactiveName(), pecPresaInCaricoInfo);
        lavorazioneRichiesta(pecPresaInCaricoInfo).doOnNext(result -> acknowledgment.acknowledge()).subscribe();
    }

    @Scheduled(cron = "${PnEcCronLavorazioneBatchPec ?:0 */5 * * * *}")
    public void lavorazioneRichiestaBatch() {

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
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                throw new MaxRetriesExceededException();
            })
            .doBeforeRetry(retrySignal -> log.debug(SHORT_RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()));

    Mono<SendMessageResponse> lavorazioneRichiesta(final PecPresaInCaricoInfo pecPresaInCaricoInfo) {
        var requestIdx = pecPresaInCaricoInfo.getRequestIdx();
        var xPagopaExtchCxId = pecPresaInCaricoInfo.getXPagopaExtchCxId();
        var digitalNotificationRequest = pecPresaInCaricoInfo.getDigitalNotificationRequest();
        String concatRequestId = concatRequestId(xPagopaExtchCxId, requestIdx);
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, LAVORAZIONE_RICHIESTA_PEC, pecPresaInCaricoInfo);

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return arubaSendMailStep(xPagopaExtchCxId, digitalNotificationRequest)
                .doOnError(MaxRetriesExceededException.class, throwable -> {
                    var stepError = new StepError();
                    stepError.setStep(ARUBA_SEND_MAIL_STEP);
                    pecPresaInCaricoInfo.setStepError(stepError);
                })
                .flatMap(generatedMessageDto -> setMessageIdInRequestMetadataStep(xPagopaExtchCxId, requestIdx, generatedMessageDto)
                        .doOnError(MaxRetriesExceededException.class, throwable -> {
                            var stepError = new StepError();
                            stepError.setStep(SET_MESSAGE_ID_STEP);
                            stepError.setGeneratedMessageDto(generatedMessageDto);
                            pecPresaInCaricoInfo.setStepError(stepError);
                        }))
                .flatMap(generatedMessageDto -> notificationTrackerStep(generatedMessageDto, pecPresaInCaricoInfo)
                        .doOnError(MaxRetriesExceededException.class, sqsPublishException -> {
                            var stepError = new StepError();
                            stepError.setStep(NOTIFICATION_TRACKER_STEP);
                            stepError.setGeneratedMessageDto(generatedMessageDto);
                            pecPresaInCaricoInfo.setStepError(stepError);
                        }))
                .onErrorResume(MaxRetriesExceededException.class, throwable -> sendNotificationOnStatusQueue(pecPresaInCaricoInfo,
                        RETRY.getStatusTransactionTableCompliant(), new DigitalProgressStatusDto())
                        .then(sendNotificationOnErrorQueue(pecPresaInCaricoInfo)))
                .doOnError(throwable -> log.error(EXCEPTION_IN_PROCESS_FOR, LAVORAZIONE_RICHIESTA_PEC, concatRequestId, throwable, throwable.getMessage()))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, LAVORAZIONE_RICHIESTA_PEC, result))
                .doFinally(signalType -> semaphore.release());
    }

    private GeneratedMessageDto createGeneratedMessageDto(SendMailResponse sendMailResponse) {
        var errstr = sendMailResponse.getErrstr();
//      Remove the last 2 char '\r\n'
        return new GeneratedMessageDto().id(errstr.substring(0, errstr.length() - 2))
                .system(getDomainFromAddress(arubaSecretValue.getPecUsername()));
    }

    @Scheduled(cron = "${PnEcCronGestioneRetryPec ?:0 */5 * * * *}")
    void gestioneRetryPecScheduler() {

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
        String concatRequestId = concatRequestId(clientId, requestIdx);
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
                .map(requestDto -> {
                    if (requestDto.getRequestMetadata().getRetry() == null) {
                        log.debug(RETRY_ATTEMPT, FILTER_REQUEST_PEC, 0, concatRequestId);
                        RetryDto retryDto = new RetryDto();
                        retryDto.setRetryPolicy(retryPolicies.getPolicy().get("PEC"));
                        retryDto.setRetryStep(BigDecimal.ZERO);
                        var eventsList = requestDto.getRequestMetadata().getEventsList();
                        var lastRetryTimestamp = eventsList.stream()
                                .max(Comparator.comparing(eventsDto -> eventsDto.getDigProgrStatus().getEventTimestamp()))
                                .map(eventsDto -> eventsDto.getDigProgrStatus().getEventTimestamp()).get();
                        retryDto.setLastRetryTimestamp(lastRetryTimestamp);
                        requestDto.getRequestMetadata().setRetry(retryDto);

                    } else {
                        requestDto.getRequestMetadata().getRetry()
                                .setRetryStep(requestDto.getRequestMetadata()
                                        .getRetry()
                                        .getRetryStep()
                                        .add(BigDecimal.ONE));
                        var retryNumber = requestDto.getRequestMetadata().getRetry().getRetryStep();
                        log.debug(RETRY_ATTEMPT, FILTER_REQUEST_PEC, retryNumber, concatRequestId);
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
                    pecPresaInCaricoInfo.getStepError().setRetryStep(retryDto.getRetryStep());
                    return gestoreRepositoryCall.patchRichiesta(xPagopaExtchCxId, requestIdx, patchDto);
                })
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, FILTER_REQUEST_PEC, result));
    }

    private Mono<DeleteMessageResponse> checkTentativiEccessiviPec(String requestIdx, RequestDto requestDto,
                                                                   final PecPresaInCaricoInfo pecPresaInCaricoInfo, Message message) {

        if (idSaved == null) {
            idSaved = requestIdx;
        }
        var retry = requestDto.getRequestMetadata().getRetry();
        if (retry.getRetryStep().compareTo(BigDecimal.valueOf(retry.getRetryPolicy().size() - 1)) >= 0) {
            // operazioni per la rimozione del messaggio
            return sendNotificationOnStatusQueue(pecPresaInCaricoInfo, ERROR.getStatusTransactionTableCompliant(), new DigitalProgressStatusDto())
                    .then(sendNotificationOnDlqErrorQueue(pecPresaInCaricoInfo))
                    .then(deleteMessageFromErrorQueue(message));
        }
        return sendNotificationOnErrorQueue(pecPresaInCaricoInfo).then(deleteMessageFromErrorQueue(message));
    }

    public Mono<SqsResponse> gestioneRetryPec(final PecPresaInCaricoInfo pecPresaInCaricoInfo, Message message) {

        var requestIdx = pecPresaInCaricoInfo.getRequestIdx();
        var xPagopaExtchCxId = pecPresaInCaricoInfo.getXPagopaExtchCxId();
        String concatRequestId = concatRequestId(xPagopaExtchCxId, requestIdx);
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, GESTIONE_RETRY_PEC, pecPresaInCaricoInfo);


        Policy retryPolicies = new Policy();

        if (pecPresaInCaricoInfo.getStepError() == null) {
            var stepError = new StepError();
            stepError.setStep(ARUBA_SEND_MAIL_STEP);
            pecPresaInCaricoInfo.setStepError(stepError);
        }

        return filterRequestPec(pecPresaInCaricoInfo).flatMap(requestDto -> chooseStep(pecPresaInCaricoInfo)
                        .repeatWhenEmpty(o -> o.doOnNext(iteration -> log.debug("gestioneRetryPec() - Repeating step {} for request {} - iteration number {}", pecPresaInCaricoInfo.getStepError().getStep().getValue(), requestIdx, iteration)))
                        .then(deleteMessageFromErrorQueue(message))
                        .onErrorResume(MaxRetriesExceededException.class, throwable -> checkTentativiEccessiviPec(pecPresaInCaricoInfo.getRequestIdx(), requestDto, pecPresaInCaricoInfo, message)))
                .cast(SqsResponse.class)
                .switchIfEmpty(sqsService.changeMessageVisibility(pecSqsQueueName.errorName(), retryPolicies.getPolicy().get("PEC").get(0).intValueExact() * 54, message.receiptHandle()))
                .onErrorResume(it.pagopa.pn.ec.commons.exception.StatusToDeleteException.class, statusToDeleteException -> {
                    log.debug(MESSAGE_REMOVED_FROM_ERROR_QUEUE, concatRequestId, pecSqsQueueName.errorName());
                    return sendNotificationOnStatusQueue(pecPresaInCaricoInfo,
                            DELETED.getStatusTransactionTableCompliant(),
                            new DigitalProgressStatusDto())
                            .flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(message));
                }).onErrorResume(internalError -> {
                    log.warn(EXCEPTION_IN_PROCESS_FOR, GESTIONE_RETRY_PEC, concatRequestId, internalError, internalError.getMessage());
                    return sendNotificationOnStatusQueue(pecPresaInCaricoInfo, INTERNAL_ERROR.getStatusTransactionTableCompliant(), new DigitalProgressStatusDto())
                            .then(sendNotificationOnDlqErrorQueue(pecPresaInCaricoInfo))
                            .then(deleteMessageFromErrorQueue(message));
                })
                .doOnError(throwable -> log.warn(EXCEPTION_IN_PROCESS_FOR, GESTIONE_RETRY_PEC, concatRequestId, throwable, throwable.getMessage()))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, GESTIONE_RETRY_PEC, result));
    }


    private Mono<SendMessageResponse> chooseStep(final PecPresaInCaricoInfo pecPresaInCaricoInfo) {

        var digitalNotificationRequest = pecPresaInCaricoInfo.getDigitalNotificationRequest();
        var xPagopaExtchCxId = pecPresaInCaricoInfo.getXPagopaExtchCxId();

        return Mono.just(pecPresaInCaricoInfo.getStepError().getStep())
                .flatMap(unused -> {
                    if (pecPresaInCaricoInfo.getStepError().getStep().equals(NOTIFICATION_TRACKER_STEP))
                        return notificationTrackerStep(pecPresaInCaricoInfo.getStepError().getGeneratedMessageDto(), pecPresaInCaricoInfo);
                    else if (pecPresaInCaricoInfo.getStepError().getStep().equals(SET_MESSAGE_ID_STEP))
                        return setMessageIdInRequestMetadataStep(xPagopaExtchCxId, digitalNotificationRequest.getRequestId(), pecPresaInCaricoInfo.getStepError().getGeneratedMessageDto())
                                .flatMap(generatedMessageDto -> {
                                    pecPresaInCaricoInfo.getStepError().setGeneratedMessageDto(generatedMessageDto);
                                    pecPresaInCaricoInfo.getStepError().setStep(NOTIFICATION_TRACKER_STEP);
                                    return Mono.empty();
                                });
                    else if (pecPresaInCaricoInfo.getStepError().getStep().equals(ARUBA_SEND_MAIL_STEP))
                        return arubaSendMailStep(xPagopaExtchCxId, digitalNotificationRequest).flatMap(generatedMessageDto -> {
                            pecPresaInCaricoInfo.getStepError().setGeneratedMessageDto(generatedMessageDto);
                            pecPresaInCaricoInfo.getStepError().setStep(SET_MESSAGE_ID_STEP);
                            return Mono.empty();
                        });
                    else return Mono.error(new ChooseStepException(pecPresaInCaricoInfo.getStepError().getStep().getValue()));
                });
    }

    private Mono<GeneratedMessageDto> arubaSendMailStep(String xPagopaExtchCxId, DigitalNotificationRequest digitalNotificationRequest) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, ARUBA_SEND_MAIL_STEP_PEC, digitalNotificationRequest);
        return attachmentService.getAllegatiPresignedUrlOrMetadata(digitalNotificationRequest.getAttachmentUrls(), xPagopaExtchCxId, false)
                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                .filter(fileDownloadResponse -> fileDownloadResponse.getDownload() != null)
                .flatMap(fileDownloadResponse -> downloadCall.downloadFile(fileDownloadResponse.getDownload().getUrl())
                        .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                        .map(outputStream -> EmailAttachment.builder()
                                .nameWithExtension(
                                        fileDownloadResponse.getKey())
                                .content(outputStream)
                                .build()))
                .collectList()
                .flatMap(attachments -> sendMail(xPagopaExtchCxId, digitalNotificationRequest.getRequestId(), digitalNotificationRequest, attachments))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, digitalNotificationRequest.getRequestId(), ARUBA_SEND_MAIL_STEP_PEC, result));
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

                .map(EmailUtils::getMimeMessageInCDATATag)

                .flatMap(mimeMessageInCdata -> {
                    var sendMail = new SendMail();
                    sendMail.setData(mimeMessageInCdata);
                    return arubaCall.sendMail(sendMail);
                })

                .handle((sendMailResponse, sink) -> {
                    if (sendMailResponse.getErrcode() != 0) {
                        log.error(ARUBA_SEND_EXCEPTION, requestIdx, sendMailResponse.getErrcode(), sendMailResponse.getErrstr(), sendMailResponse.getErrblock());
                        sink.error(new ArubaSendException());
                    } else {
                        sink.next(sendMailResponse);
                    }
                })

                .cast(SendMailResponse.class)

                .map(this::createGeneratedMessageDto)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, requestIdx, PEC_SEND_MAIL, result));
    }

    private Mono<GeneratedMessageDto> setMessageIdInRequestMetadataStep(String xPagopaExtchCxId, String requestIdx, GeneratedMessageDto generatedMessageDto) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS + " - {}", PEC_SET_MESSAGE_ID_IN_REQUEST_METADATA_STEP, requestIdx, generatedMessageDto);
        return gestoreRepositoryCall.setMessageIdInRequestMetadata(xPagopaExtchCxId, requestIdx)
                .map(requestDto -> generatedMessageDto)
                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, requestIdx, PEC_SET_MESSAGE_ID_IN_REQUEST_METADATA_STEP, result));
    }

    private Mono<SendMessageResponse> notificationTrackerStep(GeneratedMessageDto generatedMessageDto, PecPresaInCaricoInfo pecPresaInCaricoInfo) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, NOTIFICATION_TRACKER_STEP_PEC, pecPresaInCaricoInfo);
        return sendNotificationOnStatusQueue(pecPresaInCaricoInfo,
                SENT.getStatusTransactionTableCompliant(),
                new DigitalProgressStatusDto().generatedMessage(generatedMessageDto))
                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, pecPresaInCaricoInfo.getRequestIdx(), NOTIFICATION_TRACKER_STEP_PEC, result));
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
    public Mono<SendMessageResponse> sendNotificationOnDlqErrorQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(pecSqsQueueName.dlqErrorName(), presaInCaricoInfo);
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
