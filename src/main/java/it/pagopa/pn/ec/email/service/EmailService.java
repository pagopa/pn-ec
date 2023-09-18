package it.pagopa.pn.ec.email.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.MaxRetriesExceededException;
import it.pagopa.pn.ec.commons.exception.email.EmailException;
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
import it.pagopa.pn.ec.commons.service.*;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.email.configurationproperties.EmailDefault;
import it.pagopa.pn.ec.email.configurationproperties.EmailSqsQueueName;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;
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
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital;
import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.*;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.ReactorUtils.pullFromFluxUntilIsEmpty;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.MessageContentTypeEnum.HTML;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.EMAIL;

@Service
@Slf4j
public class EmailService extends PresaInCaricoService implements QueueOperationsService {

    private final SqsService sqsService;
    private final SesService sesService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final AttachmentServiceImpl attachmentService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final EmailSqsQueueName emailSqsQueueName;
    private final EmailDefault emailDefault;
    private final DownloadCall downloadCall;
    private final Semaphore semaphore;
    private String idSaved;

    private static final String GENERIC_ERROR = "Errore generico";

    protected EmailService(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService,
                           SesService sesService, AttachmentServiceImpl attachmentService,
                           NotificationTrackerSqsName notificationTrackerSqsName, EmailSqsQueueName emailSqsQueueName,
                           DownloadCall downloadCall, EmailDefault emailDefault, @Value("${lavorazione-email.max-thread-pool-size}") Integer maxThreadPoolSize) {
        super(authService);
        this.sqsService = sqsService;
        this.sesService = sesService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.attachmentService = attachmentService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.emailSqsQueueName = emailSqsQueueName;
        this.emailDefault = emailDefault;
        this.downloadCall = downloadCall;
        this.semaphore=new Semaphore(maxThreadPoolSize);
    }

    private final Retry PRESA_IN_CARICO_RETRY_STRATEGY = Retry.backoff(3, Duration.ofMillis(500))
            .doBeforeRetry(retrySignal -> log.debug(SHORT_RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()));

    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {

        var emailPresaInCaricoInfo = (EmailPresaInCaricoInfo) presaInCaricoInfo;
        var clientId = emailPresaInCaricoInfo.getXPagopaExtchCxId();
        var requestIdx = emailPresaInCaricoInfo.getRequestIdx();
        String concatRequestId = concatRequestId(clientId, requestIdx);

        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PRESA_IN_CARICO_EMAIL, presaInCaricoInfo);

        var xPagopaExtchCxId = emailPresaInCaricoInfo.getXPagopaExtchCxId();
        var digitalNotificationRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
        var senderAddress= digitalNotificationRequest.getSenderDigitalAddress();
        if(Objects.isNull(senderAddress) || senderAddress.isEmpty()) {
            digitalNotificationRequest.setSenderDigitalAddress(emailDefault.defaultSenderAddress());
        }

        digitalNotificationRequest.setRequestId(requestIdx);

        return attachmentService.getAllegatiPresignedUrlOrMetadata(emailPresaInCaricoInfo.getDigitalCourtesyMailRequest()
                        .getAttachmentUrls(), xPagopaExtchCxId, true)
                .retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY)
                .then(insertRequestFromEmail(digitalNotificationRequest, emailPresaInCaricoInfo.getXPagopaExtchCxId()))

                .flatMap(requestDto -> sendNotificationOnStatusQueue(emailPresaInCaricoInfo,
                        BOOKED.getStatusTransactionTableCompliant(),
                        new DigitalProgressStatusDto()).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY))

                .flatMap(sendMessageResponse -> {
                    DigitalCourtesyMailRequest.QosEnum qos =
                            emailPresaInCaricoInfo.getDigitalCourtesyMailRequest().getQos();
                    if (qos == INTERACTIVE) {
                        return sendNotificationOnInteractiveQueue(emailPresaInCaricoInfo).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY);
                    } else if (qos == BATCH) {
                        return sendNotificationOnBatchQueue(emailPresaInCaricoInfo).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY);
                    } else {
                        return Mono.empty();
                    }
                })
                .onErrorResume(SqsClientException.class,
                        sqsClientException -> sendNotificationOnStatusQueue(emailPresaInCaricoInfo,
                                INTERNAL_ERROR.getStatusTransactionTableCompliant(),
                                new DigitalProgressStatusDto())
                                .retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY)
                                .then(Mono.error(sqsClientException)))
                .then()
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, PRESA_IN_CARICO_EMAIL, result));
    }

    @SuppressWarnings("Duplicates")
    private Mono<RequestDto> insertRequestFromEmail(final DigitalCourtesyMailRequest digitalCourtesyMailRequest, String xPagopaExtchCxId) {
        String concatRequestId = concatRequestId(xPagopaExtchCxId, digitalCourtesyMailRequest.getRequestId());
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, INSERT_REQUEST_FROM_EMAIL, digitalCourtesyMailRequest);
        return Mono.fromCallable(() -> {
            var requestDto = new RequestDto();
            requestDto.setRequestIdx(digitalCourtesyMailRequest.getRequestId());
            requestDto.setClientRequestTimeStamp(digitalCourtesyMailRequest.getClientRequestTimeStamp());
            requestDto.setxPagopaExtchCxId(xPagopaExtchCxId);

            var requestPersonalDto = new RequestPersonalDto();
            var digitalRequestPersonalDto = new DigitalRequestPersonalDto();
            digitalRequestPersonalDto.setQos(DigitalRequestPersonalDto.QosEnum.valueOf(digitalCourtesyMailRequest.getQos().name()));
            digitalRequestPersonalDto.setReceiverDigitalAddress(digitalCourtesyMailRequest.getReceiverDigitalAddress());
            digitalRequestPersonalDto.setMessageText(digitalCourtesyMailRequest.getMessageText());
            digitalRequestPersonalDto.setSenderDigitalAddress(digitalCourtesyMailRequest.getSenderDigitalAddress());
            digitalRequestPersonalDto.setSubjectText(digitalCourtesyMailRequest.getSubjectText());
            digitalRequestPersonalDto.setAttachmentsUrls(digitalCourtesyMailRequest.getAttachmentUrls());
            requestPersonalDto.setDigitalRequestPersonal(digitalRequestPersonalDto);

            var requestMetadataDto = new RequestMetadataDto();
            var digitalRequestMetadataDto = new DigitalRequestMetadataDto();
            digitalRequestMetadataDto.setCorrelationId(digitalCourtesyMailRequest.getCorrelationId());
            digitalRequestMetadataDto.setEventType(digitalCourtesyMailRequest.getEventType());
            digitalRequestMetadataDto.setTags(digitalCourtesyMailRequest.getTags());
            digitalRequestMetadataDto.setChannel(EMAIL);
            digitalRequestMetadataDto.setMessageContentType(DigitalRequestMetadataDto.MessageContentTypeEnum.PLAIN);
            requestMetadataDto.setDigitalRequestMetadata(digitalRequestMetadataDto);

            requestDto.setRequestPersonal(requestPersonalDto);
            requestDto.setRequestMetadata(requestMetadataDto);
            return requestDto;
        }).flatMap(gestoreRepositoryCall::insertRichiesta).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY)
        .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, INSERT_REQUEST_FROM_EMAIL, result));
    }

    @SqsListener(value = "${sqs.queue.email.interactive-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void lavorazioneRichiestaInteractive(final EmailPresaInCaricoInfo emailPresaInCaricoInfo, final Acknowledgment acknowledgment) {
        logIncomingMessage(emailSqsQueueName.interactiveName(), emailPresaInCaricoInfo);
        lavorazioneRichiesta(emailPresaInCaricoInfo).doOnSuccess(result -> acknowledgment.acknowledge()).subscribe();
    }

    @Scheduled(cron = "${PnEcCronLavorazioneBatchEmail ?:0 */5 * * * *}")
    public void lavorazioneRichiestaBatch() {
        sqsService.getMessages(emailSqsQueueName.batchName(), EmailPresaInCaricoInfo.class)
                  .doOnNext(emailPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(emailSqsQueueName.batchName(),
                                                                                          emailPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                  .flatMap(emailPresaInCaricoInfoSqsMessageWrapper -> Mono.zip(Mono.just(emailPresaInCaricoInfoSqsMessageWrapper.getMessage()),
                                                                               lavorazioneRichiesta(emailPresaInCaricoInfoSqsMessageWrapper.getMessageContent())))
                  .flatMap(emailPresaInCaricoInfoSqsMessageWrapper -> sqsService.deleteMessageFromQueue(
                          emailPresaInCaricoInfoSqsMessageWrapper.getT1(),
                          emailSqsQueueName.batchName()))
                  .transform(pullFromFluxUntilIsEmpty())
                  .subscribe();
    }

    private final Retry LAVORAZIONE_RICHIESTA_RETRY_STRATEGY = Retry.backoff(3, Duration.ofSeconds(2))
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                throw new MaxRetriesExceededException();
            })
            .doBeforeRetry(retrySignal -> log.debug(SHORT_RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()));;

    Mono<SendMessageResponse> lavorazioneRichiesta(final EmailPresaInCaricoInfo emailPresaInCaricoInfo) {

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        var digitalCourtesyMailRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
        var clientId = emailPresaInCaricoInfo.getXPagopaExtchCxId();
        var requestIdx = emailPresaInCaricoInfo.getRequestIdx();
        String concatRequestId = concatRequestId(clientId, requestIdx);

        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, LAVORAZIONE_RICHIESTA_EMAIL, concatRequestId);


        return sesSendStep(emailPresaInCaricoInfo)
                .doOnError(MaxRetriesExceededException.class, throwable -> {
                    var stepError = new StepError();
                    emailPresaInCaricoInfo.setStepError(stepError);
                    emailPresaInCaricoInfo.getStepError().setStep(SNS_SEND_STEP);
                })
                .flatMap(generatedMessageDto -> notificationTrackerStep(generatedMessageDto, emailPresaInCaricoInfo)
                        .doOnError(MaxRetriesExceededException.class, throwable -> {
                            var stepError = new StepError();
                            emailPresaInCaricoInfo.setStepError(stepError);
                            emailPresaInCaricoInfo.getStepError().setStep(NOTIFICATION_TRACKER_STEP);
                            emailPresaInCaricoInfo.getStepError().setGeneratedMessageDto(generatedMessageDto);
                        }))
                .onErrorResume(MaxRetriesExceededException.class, throwable ->
                        sendNotificationOnStatusQueue(emailPresaInCaricoInfo, RETRY.getStatusTransactionTableCompliant(), new DigitalProgressStatusDto())
                                .then(sendNotificationOnErrorQueue(emailPresaInCaricoInfo)))
                .doOnError(throwable -> log.error(EXCEPTION_IN_PROCESS_FOR, LAVORAZIONE_RICHIESTA_EMAIL, concatRequestId, throwable, throwable.getMessage()))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, LAVORAZIONE_RICHIESTA_EMAIL, result))
                .doFinally(signalType -> semaphore.release());
    }

    private EmailField compilaMail(DigitalCourtesyMailRequest req) {
        var ret = EmailField.builder()
                            .from(req.getSenderDigitalAddress())
                            .to(req.getReceiverDigitalAddress())
                            .subject(req.getSubjectText())
                            .text(req.getMessageText())
                            .emailAttachments(new ArrayList<>())
                            .build();
        if (req.getMessageContentType() == PLAIN) {
            ret.setContentType("text/plain; charset=UTF-8");
        } else if (req.getMessageContentType() == HTML) {
            ret.setContentType("text/html; charset=UTF-8");
        }
        return ret;
    }

    @Scheduled(cron = "${PnEcCronGestioneRetryEmail ?:0 */5 * * * *}")
    void gestioneRetryEmailScheduler() {
        idSaved = null;
        sqsService.getOneMessage(emailSqsQueueName.errorName(), EmailPresaInCaricoInfo.class)
                  .doOnNext(emailPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(emailSqsQueueName.errorName(),
                                                                                          emailPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                  .flatMap(emailPresaInCaricoInfoSqsMessageWrapper -> Mono.zip(Mono.just(emailPresaInCaricoInfoSqsMessageWrapper.getMessage()),
                                                                               gestioneRetryEmail(emailPresaInCaricoInfoSqsMessageWrapper.getMessageContent(),
                                                                                                  emailPresaInCaricoInfoSqsMessageWrapper.getMessage())))
                  .map(MonoResultWrapper::new)
                  .doOnError(throwable -> log.error(GENERIC_ERROR, throwable))
                  .defaultIfEmpty(new MonoResultWrapper<>(null))
                  .repeat()
                  .takeWhile(MonoResultWrapper::isNotEmpty)
                  .subscribe();
    }

    private Mono<RequestDto> filterRequestEmail(final EmailPresaInCaricoInfo emailPresaInCaricoInfo) {
        var requestId = emailPresaInCaricoInfo.getRequestIdx();
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, FILTER_REQUEST_EMAIL, requestId);
        var clientId = emailPresaInCaricoInfo.getXPagopaExtchCxId();
        Policy retryPolicies = new Policy();
        String toDelete = "toDelete";

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
                        log.debug(RETRY_ATTEMPT, FILTER_REQUEST_EMAIL, 0, requestId);
                        RetryDto retryDto = new RetryDto();
                        retryDto.setRetryPolicy(retryPolicies.getPolicy().get("EMAIL"));
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
                        log.debug(RETRY_ATTEMPT, FILTER_REQUEST_EMAIL, retryNumber, requestId);
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
                    emailPresaInCaricoInfo.getStepError().setRetryStep(retryDto.getRetryStep());
                    return gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto);
                })
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, requestId, FILTER_REQUEST_EMAIL, result));
    }

    private Mono<DeleteMessageResponse> checkTentativiEccessiviEmail(String requestId, RequestDto requestDto,
                                                                     final EmailPresaInCaricoInfo emailPresaInCaricoInfo, Message message) {
        if (idSaved == null) {
            idSaved = requestId;
        }
        var retry = requestDto.getRequestMetadata().getRetry();
        if (retry.getRetryStep().compareTo(BigDecimal.valueOf(retry.getRetryPolicy().size() - 1)) >= 0) {
            // operazioni per la rimozione del messaggio
            return sendNotificationOnStatusQueue(emailPresaInCaricoInfo, ERROR.getStatusTransactionTableCompliant(), new DigitalProgressStatusDto())
                    .then(sendNotificationOnDlqErrorQueue(emailPresaInCaricoInfo))
                    .then(deleteMessageFromErrorQueue(message));
        }
        return sendNotificationOnErrorQueue(emailPresaInCaricoInfo).then(deleteMessageFromErrorQueue(message));
    }

    public Mono<SqsResponse> gestioneRetryEmail(final EmailPresaInCaricoInfo emailPresaInCaricoInfo, Message message) {

        var requestIdx = emailPresaInCaricoInfo.getRequestIdx();
        var xPagopaExtchCxId = emailPresaInCaricoInfo.getXPagopaExtchCxId();
        String concatRequestId = concatRequestId(xPagopaExtchCxId, requestIdx);
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, GESTIONE_RETRY_EMAIL, emailPresaInCaricoInfo);

        Policy retryPolicies = new Policy();

        if (emailPresaInCaricoInfo.getStepError() == null) {
            var stepError = new StepError();
            stepError.setStep(SES_SEND_STEP);
            emailPresaInCaricoInfo.setStepError(stepError);
        }

        return filterRequestEmail(emailPresaInCaricoInfo)
                .flatMap(requestDto -> chooseStep(emailPresaInCaricoInfo)
                        .repeatWhenEmpty(o -> o.doOnNext(iteration -> log.debug("Step repeated {} times for request {}", iteration, emailPresaInCaricoInfo.getRequestIdx())))
                        .then(deleteMessageFromErrorQueue(message))
                        .onErrorResume(MaxRetriesExceededException.class, throwable -> checkTentativiEccessiviEmail(emailPresaInCaricoInfo.getRequestIdx(), requestDto, emailPresaInCaricoInfo, message)))
                .cast(SqsResponse.class)
                .switchIfEmpty(sqsService.changeMessageVisibility(emailSqsQueueName.errorName(), retryPolicies.getPolicy().get("EMAIL").get(0).intValueExact() * 54, message.receiptHandle()))
                .onErrorResume(StatusToDeleteException.class, statusToDeleteException -> {
                    log.debug(MESSAGE_REMOVED_FROM_ERROR_QUEUE, concatRequestId, emailSqsQueueName.errorName());
                    return sendNotificationOnStatusQueue(emailPresaInCaricoInfo, DELETED.getStatusTransactionTableCompliant(), new DigitalProgressStatusDto())
                            .flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(message));
                })
                .onErrorResume(internalError -> sendNotificationOnStatusQueue(emailPresaInCaricoInfo, INTERNAL_ERROR.getStatusTransactionTableCompliant(), new DigitalProgressStatusDto())
                        .then(sendNotificationOnDlqErrorQueue(emailPresaInCaricoInfo))
                        .then(deleteMessageFromErrorQueue(message)))
                .doOnError(throwable -> log.warn(EXCEPTION_IN_PROCESS_FOR, GESTIONE_RETRY_EMAIL, concatRequestId, throwable, throwable.getMessage()))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, GESTIONE_RETRY_EMAIL, result));
    }

    private Mono<SendMessageResponse> chooseStep(final EmailPresaInCaricoInfo emailPresaInCaricoInfo) {
        return Mono.just(emailPresaInCaricoInfo.getStepError().getStep())
                .flatMap(step -> {
                    if (emailPresaInCaricoInfo.getStepError().getStep().equals(NOTIFICATION_TRACKER_STEP)) {
                        return notificationTrackerStep(emailPresaInCaricoInfo.getStepError().getGeneratedMessageDto(), emailPresaInCaricoInfo);
                    } else {
                        return sesSendStep(emailPresaInCaricoInfo).flatMap(generatedMessageDto -> {
                            emailPresaInCaricoInfo.getStepError().setGeneratedMessageDto(generatedMessageDto);
                            emailPresaInCaricoInfo.getStepError().setStep(NOTIFICATION_TRACKER_STEP);
                            return Mono.empty();
                        });
                    }
                });
    }

    private Mono<GeneratedMessageDto> sesSendStep(EmailPresaInCaricoInfo emailPresaInCaricoInfo) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, EMAIL_SES_SEND_STEP, emailPresaInCaricoInfo);
        DigitalCourtesyMailRequest digitalCourtesyMailRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
        return Mono.justOrEmpty(digitalCourtesyMailRequest.getAttachmentUrls())
                .flatMap(attachmentUrls ->
                        attachmentService.getAllegatiPresignedUrlOrMetadata(digitalCourtesyMailRequest.getAttachmentUrls(), emailPresaInCaricoInfo.getXPagopaExtchCxId(), false)
                                .filter(fileDownloadResponse -> fileDownloadResponse.getDownload() != null)
                                .flatMap(fileDownloadResponse -> downloadCall.downloadFile(fileDownloadResponse.getDownload().getUrl())
                                        .map(outputStream -> EmailAttachment.builder()
                                                .nameWithExtension(fileDownloadResponse.getKey())
                                                .content(outputStream)
                                                .build()))
                                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                                .collectList())
                .switchIfEmpty(Mono.just(new ArrayList<>()))
                .flatMap(attList -> {
                    var mailFld = compilaMail(digitalCourtesyMailRequest);
                    if (!attList.isEmpty()) {
                        mailFld.setEmailAttachments(attList);
                    }
                    return sesService.send(mailFld).retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY);
                })
                .map(this::createGeneratedMessageDto)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, emailPresaInCaricoInfo.getRequestIdx(), EMAIL_SES_SEND_STEP, result));
    }

    private Mono<SendMessageResponse> notificationTrackerStep(GeneratedMessageDto generatedMessageDto, final EmailPresaInCaricoInfo emailPresaInCaricoInfo) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, NOTIFICATION_TRACKER_STEP_EMAIL, emailPresaInCaricoInfo);
        return sendNotificationOnStatusQueue(emailPresaInCaricoInfo,
                SENT.getStatusTransactionTableCompliant(),
                new DigitalProgressStatusDto().generatedMessage(generatedMessageDto))
                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, emailPresaInCaricoInfo.getRequestIdx(), NOTIFICATION_TRACKER_STEP_EMAIL, result));
    }

    private GeneratedMessageDto createGeneratedMessageDto(SendRawEmailResponse publishResponse) {
        return new GeneratedMessageDto().id(publishResponse.messageId()).system("toBeDefined");
    }

    @Override
    public Mono<DeleteMessageResponse> deleteMessageFromErrorQueue(Message message) {
        return sqsService.deleteMessageFromQueue(message, emailSqsQueueName.errorName());
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnStatusQueue(PresaInCaricoInfo presaInCaricoInfo, String status,
                                                                   DigitalProgressStatusDto digitalProgressStatusDto) {
        return sqsService.send(notificationTrackerSqsName.statoEmailName(),
                               createNotificationTrackerQueueDtoDigital(presaInCaricoInfo, status, digitalProgressStatusDto));
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnErrorQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(emailSqsQueueName.errorName(), presaInCaricoInfo);
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnDlqErrorQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(emailSqsQueueName.dlqErrorName(), presaInCaricoInfo);
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnBatchQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(emailSqsQueueName.batchName(), presaInCaricoInfo);
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnInteractiveQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(emailSqsQueueName.interactiveName(), presaInCaricoInfo);
    }

}
