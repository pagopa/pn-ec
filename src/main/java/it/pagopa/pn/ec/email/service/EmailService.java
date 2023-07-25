package it.pagopa.pn.ec.email.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.RetryAttemptsExceededExeption;
import it.pagopa.pn.ec.commons.exception.SemaphoreException;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital;
import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.NOTIFICATION_TRACKER_STEP;
import static it.pagopa.pn.ec.commons.service.SesService.DEFAULT_RETRY_STRATEGY;
import static it.pagopa.pn.ec.commons.utils.ReactorUtils.pullFromFluxUntilIsEmpty;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
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
            .doBeforeRetry(retrySignal -> log.debug("Retry number {}, caused by : {}", retrySignal.totalRetries(), retrySignal.failure().getMessage(), retrySignal.failure()));

    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {

        var emailPresaInCaricoInfo = (EmailPresaInCaricoInfo) presaInCaricoInfo;
        var requestIdx = emailPresaInCaricoInfo.getRequestIdx();
        var xPagopaExtchCxId = emailPresaInCaricoInfo.getXPagopaExtchCxId();
        var digitalNotificationRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
        var senderAddress= digitalNotificationRequest.getSenderDigitalAddress();
        if(Objects.isNull(senderAddress) || senderAddress.isEmpty()) {
            digitalNotificationRequest.setSenderDigitalAddress(emailDefault.defaultSenderAddress());
        }

        log.info("<-- START PRESA IN CARICO EMAIL --> Request ID: {}, Client ID: {}", requestIdx, xPagopaExtchCxId);

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
                                .then();
    }

    @SuppressWarnings("Duplicates")
    private Mono<RequestDto> insertRequestFromEmail(final DigitalCourtesyMailRequest digitalCourtesyMailRequest, String xPagopaExtchCxId) {
        log.info("<-- START INSERT REQUEST FROM EMAIL --> Request ID: {}, Client ID: {}",
                 digitalCourtesyMailRequest.getRequestId(),
                 xPagopaExtchCxId);

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
        }).flatMap(gestoreRepositoryCall::insertRichiesta).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY);
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

    private final Retry LAVORAZIONE_RICHIESTA_RETRY_STRATEGY = Retry.backoff(3, Duration.ofSeconds(2));

    Mono<SendMessageResponse> lavorazioneRichiesta(final EmailPresaInCaricoInfo emailPresaInCaricoInfo) {
        log.info("<-- START LAVORAZIONE RICHIESTA EMAIL --> Request ID : {}, Client ID : {}, QOS : {}",
                 emailPresaInCaricoInfo.getRequestIdx(),
                 emailPresaInCaricoInfo.getXPagopaExtchCxId(),
                 emailPresaInCaricoInfo.getDigitalCourtesyMailRequest().getQos());

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new SemaphoreException(e.getMessage());
        }

        var digitalCourtesyMailRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
        var requestId = emailPresaInCaricoInfo.getXPagopaExtchCxId();
        AtomicReference<GeneratedMessageDto> generatedMessageDto = new AtomicReference<>();

        // Try to send EMAIL
        return attachmentService.getAllegatiPresignedUrlOrMetadata(digitalCourtesyMailRequest.getAttachmentUrls(), requestId, false)
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

                                .flatMap(attList -> {
                                    var mailFld = compilaMail(digitalCourtesyMailRequest);
                                    mailFld.setEmailAttachments(attList);
                                    return sesService.send(mailFld);
                                })

                                // The EMAIL in sent, publish to Notification Tracker with next status -> SENT
                                .flatMap(publishResponse -> {
                                    generatedMessageDto.set(new GeneratedMessageDto().id(publishResponse.messageId())
                                                                                     .system("systemPlaceholder"));
                                    return sendNotificationOnStatusQueue(emailPresaInCaricoInfo,
                                                                         SENT.getStatusTransactionTableCompliant(),
                                                                         new DigitalProgressStatusDto().generatedMessage(generatedMessageDto.get()))
                                            // An error occurred during EMAIL send, start retries
                                            .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                                            // An error occurred during SQS publishing to the Notification Tracker -> Publish to
                                            // Errori EMAIL queue and
                                            // notify to retry update status only
                                            .onErrorResume(sqsPublishException -> {
                                                var stepError = new StepError();
                                                emailPresaInCaricoInfo.setStepError(stepError);
                                                emailPresaInCaricoInfo.getStepError()
                                                                      .setStep(NOTIFICATION_TRACKER_STEP);
                                                emailPresaInCaricoInfo.getStepError().setGeneratedMessageDto(generatedMessageDto.get());
                                                return sendNotificationOnErrorQueue(emailPresaInCaricoInfo);
                                            });
                                })
                                // The maximum number of retries has ended
                                .onErrorResume(throwable ->

                                                       sendNotificationOnStatusQueue(emailPresaInCaricoInfo,
                                                                                     RETRY.getStatusTransactionTableCompliant(),
                                                                                     new DigitalProgressStatusDto())

                                                               // Publish to ERRORI EMAIL queue
                                                               .then(sendNotificationOnErrorQueue(emailPresaInCaricoInfo)))
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


    public Mono<DeleteMessageResponse> gestioneRetryEmail(final EmailPresaInCaricoInfo emailPresaInCaricoInfo, Message message) {
        log.info("<-- START GESTIONE RETRY EMAIL --> Request ID : {}, Client ID : {}",
                 emailPresaInCaricoInfo.getRequestIdx(),
                 emailPresaInCaricoInfo.getXPagopaExtchCxId());
        logIncomingMessage(emailSqsQueueName.interactiveName(), emailPresaInCaricoInfo);
        var digitalCourtesyMailRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
        if (!digitalCourtesyMailRequest.getAttachmentUrls().isEmpty()) {
            return processWithAttachRetry(emailPresaInCaricoInfo, message);
        } else {
            return processOnlyBodyRetry(emailPresaInCaricoInfo, message);
        }
    }

    private Mono<RequestDto> filterRequestEmail(final EmailPresaInCaricoInfo emailPresaInCaricoInfo) {
        log.info("<-- START PROCESS WITH ATTACH RETRY--> Request ID : {}, Client ID : {}",
                 emailPresaInCaricoInfo.getRequestIdx(),
                 emailPresaInCaricoInfo.getXPagopaExtchCxId());
        var requestId = emailPresaInCaricoInfo.getRequestIdx();
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
                                    .flatMap(requestDto -> {
                                        if (requestDto.getRequestMetadata().getRetry() == null) {
                                            log.debug("Primo tentativo di Retry");
                                            RetryDto retryDto = new RetryDto();
                                            log.debug("policy" + retryPolicies.getPolicy().get("EMAIL"));
                                            return getMono(requestId, retryPolicies, requestDto, retryDto);

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

    private Mono<DeleteMessageResponse> checkTentativiEccessiviEmail(String requestId, RequestDto requestDto,
                                                                     final EmailPresaInCaricoInfo emailPresaInCaricoInfo, Message message) {
        if (idSaved == null) {
            idSaved = requestId;
        }
        if (requestDto.getRequestMetadata().getRetry().getRetryStep().compareTo(BigDecimal.valueOf(3)) > 0) {
            // operazioni per la rimozione del messaggio
            log.debug("Il messaggio è stato rimosso dalla coda d'errore" + " per eccessivi tentativi: {}", emailSqsQueueName.errorName());
            return sendNotificationOnStatusQueue(emailPresaInCaricoInfo,
                                                 ERROR.getStatusTransactionTableCompliant(),
                                                 new DigitalProgressStatusDto()).flatMap(sendMessageResponse -> deleteMessageFromErrorQueue(
                    message));

        }
        return Mono.empty();
    }

    private Mono<DeleteMessageResponse> processWithAttachRetry(final EmailPresaInCaricoInfo emailPresaInCaricoInfo, Message message) {

        var digitalCourtesyMailRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
        var requestId = emailPresaInCaricoInfo.getRequestIdx();
        AtomicReference<GeneratedMessageDto> generatedMessageDto = new AtomicReference<>();

        return filterRequestEmail(emailPresaInCaricoInfo).flatMap(requestDto -> {
                                                             // Try to send EMAIL
//                                        check step error per evitare null pointer
                                                             if (emailPresaInCaricoInfo.getStepError() == null) {
                                                                 var stepError = new StepError();
                                                                 emailPresaInCaricoInfo.setStepError(stepError);
                                                             }
//                                        check step error per evitare nuova chiamata verso ses
//              caso in cui è avvenuto un errore nella pubblicazione sul notification tracker,  The EMAIL in sent, publish to
//              Notification Tracker with next status -> SENT
                                                             if (Objects.equals(emailPresaInCaricoInfo.getStepError().getStep(), NOTIFICATION_TRACKER_STEP)) {
                                                                 return sendNotificationOnStatusQueue(emailPresaInCaricoInfo,
                                                                                                      SENT.getStatusTransactionTableCompliant(),
                                                                                                      new DigitalProgressStatusDto().generatedMessage(emailPresaInCaricoInfo.getStepError()
                                                                                                                                                                            .getGeneratedMessageDto())).flatMap(
                                                                                                                                                                                                               sendMessageResponse -> {
                                                                                                                                                                                                                   log.debug("Il messaggio è stato gestito correttamente e rimosso" + " dalla coda d'errore: {}",
                                                                                                                                                                                                                             emailSqsQueueName.errorName());
                                                                                                                                                                                                                   return deleteMessageFromErrorQueue(message);
                                                                                                                                                                                                               })
                                                                                                                                                                                                       .onErrorResume(
                                                                                                                                                                                                               sqsPublishException -> {
                                                                                                                                                                                                                   log.warn("Exception in processWithAttachRetry {}, {}", sqsPublishException, sqsPublishException.getMessage());
                                                                                                                                                                                                                   return checkTentativiEccessiviEmail(
                                                                                                                                                                                                                           requestId,
                                                                                                                                                                                                                           requestDto,
                                                                                                                                                                                                                           emailPresaInCaricoInfo,
                                                                                                                                                                                                                           message);
                                                                                                                                                                                                               });
                                                             } else {
                                                                 //                gestisco il caso retry a partire dalla gestione
                                                                 //                allegati e invio a ses
                                                                 return attachmentService.getAllegatiPresignedUrlOrMetadata(digitalCourtesyMailRequest.getAttachmentUrls(),
                                                                                                                            emailPresaInCaricoInfo.getXPagopaExtchCxId(),
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

                                                                                         .collectList()

                                                                                         .flatMap(attList -> {
                                                                                             EmailField mailFld =
                                                                                                     compilaMail(digitalCourtesyMailRequest);
                                                                                             mailFld.setEmailAttachments(attList);
                                                                                             return sesService.send(mailFld);
                                                                                         })

                                                                                         .retryWhen(DEFAULT_RETRY_STRATEGY)

                                                                                         .map(this::createGeneratedMessageDto)

                                                                                         // The EMAIL in sent, publish to Notification
                                                                                         // Tracker with next
                                                                                         // status -> SENT
                                                                                         .flatMap(publishResponse -> sendNotificationOnStatusQueue(emailPresaInCaricoInfo,
                                                                                                                                                   SENT.getStatusTransactionTableCompliant(),
                                                                                                                                                   new DigitalProgressStatusDto().generatedMessage(
                                                                                                                                                           generatedMessageDto.get())))
                                                                                         .flatMap(sendMessageResponse -> {
                                                                                             log.debug("Il messaggio è stato gestito " +
                                                                                                       "correttamente e rimosso" + " " +
                                                                                                       "dalla coda d'errore: {}",
                                                                                                       emailSqsQueueName.errorName());
                                                                                             return deleteMessageFromErrorQueue(message);
                                                                                         })
                                                                                         .onErrorResume(sqsPublishException -> {
                                                                                             log.warn("Exception in processWithAttachRetry {}, {}", sqsPublishException, sqsPublishException.getMessage());
                                                                                             return checkTentativiEccessiviEmail(requestId,
                                                                                                     requestDto,
                                                                                                     emailPresaInCaricoInfo,
                                                                                                     message);
                                                                                         });
                                                             }
                                                         })//              Catch errore tirato per lo stato toDelete
                                                         .onErrorResume(RetryAttemptsExceededExeption.class,
                                                                        retryAttemptsExceededExeption -> {
                                                                            log.debug(
                                                                                    "Il messaggio è stato rimosso dalla coda d'errore per" +
                                                                                    " status toDelete: {}", emailSqsQueueName.errorName());
                                                                            return sendNotificationOnStatusQueue(emailPresaInCaricoInfo,
                                                                                                                 DELETED.getStatusTransactionTableCompliant(),
                                                                                                                 new DigitalProgressStatusDto()).flatMap(
                                                                                    sendMessageResponse -> deleteMessageFromErrorQueue(
                                                                                            message));


                                                                        })
                                                         .onErrorResume(internalError -> sendNotificationOnStatusQueue(
                                                                 emailPresaInCaricoInfo,
                                                                 INTERNAL_ERROR.getStatusTransactionTableCompliant(),
                                                                 new DigitalProgressStatusDto()).then(deleteMessageFromErrorQueue(message)))
                                                         .doOnError(throwable->log.error("* FATAL * processWithAttachRetry {}, {}", throwable, throwable.getMessage()));
    }

    private Mono<? extends RequestDto> getMono(String requestId, Policy retryPolicies, RequestDto requestDto, RetryDto retryDto) {
        retryDto.setRetryPolicy(retryPolicies.getPolicy().get("EMAIL"));
        retryDto.setRetryStep(BigDecimal.ZERO);
        retryDto.setLastRetryTimestamp(OffsetDateTime.now());
        requestDto.getRequestMetadata().setRetry(retryDto);
        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());
        return gestoreRepositoryCall.patchRichiesta(requestDto.getxPagopaExtchCxId(), requestId, patchDto);
    }

    private Mono<DeleteMessageResponse> processOnlyBodyRetry(final EmailPresaInCaricoInfo emailPresaInCaricoInfo, Message message) {

        var digitalCourtesyMailRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
        var requestId = emailPresaInCaricoInfo.getRequestIdx();
        AtomicReference<GeneratedMessageDto> generatedMessageDto = new AtomicReference<>();
        // Try to send EMAIL
        EmailField mailFld = compilaMail(digitalCourtesyMailRequest);
        return filterRequestEmail(emailPresaInCaricoInfo).flatMap(requestDto -> {
                                                             //                                        check step error per evitare null
                                                             //                                        pointer
                                                             if (emailPresaInCaricoInfo.getStepError() == null) {
                                                                 var stepError = new StepError();
                                                                 emailPresaInCaricoInfo.setStepError(stepError);
                                                             }
                                                             //                                        check step error per evitare nuova
                                                             //                                        chiamata verso ses
//              caso in cui è avvenuto un errore nella pubblicazione sul notification tracker,  The EMAIL in sent, publish to
//              Notification Tracker with next status -> SENT
                                                             if (Objects.equals(emailPresaInCaricoInfo.getStepError().getStep(), NOTIFICATION_TRACKER_STEP)) {
                                                                 log.debug("requestDto Value: {}",
                                                                           requestDto.getRequestMetadata().getRetry());
                                                                 return sqsService.send(notificationTrackerSqsName.statoEmailName(),
                                                                                        createNotificationTrackerQueueDtoDigital(emailPresaInCaricoInfo,
                                                                                                                                 SENT.getStatusTransactionTableCompliant(),
                                                                                                                                 new DigitalProgressStatusDto().generatedMessage(
                                                                                                                                         emailPresaInCaricoInfo.getStepError()
                                                                                                                                                               .getGeneratedMessageDto())))

                                                                                  .flatMap(sendMessageResponse -> {
                                                                                      log.debug("Il messaggio è stato gestito " +
                                                                                                "correttamente e rimosso dalla coda " +
                                                                                                "d'errore {}",
                                                                                                emailSqsQueueName.errorName());
                                                                                      return sqsService.deleteMessageFromQueue(message,
                                                                                                                               emailSqsQueueName.errorName());
                                                                                  })
                                                                                  .onErrorResume(sqsPublishException -> {
                                                                                      log.warn("Exception in processOnlyBodyRetry {}, {}", sqsPublishException, sqsPublishException.getMessage());
                                                                                      return checkTentativiEccessiviEmail(requestId,
                                                                                              requestDto,
                                                                                              emailPresaInCaricoInfo,
                                                                                              message);
                                                                                  });
                                                             } else {
                                                                 log.debug("requestDto Value: {}",
                                                                           requestDto.getRequestMetadata().getRetry());
                                                                 return sesService.send(mailFld)
                                                                                  .retryWhen(DEFAULT_RETRY_STRATEGY)

                                                                                  .map(this::createGeneratedMessageDto)
                                                                                  // The EMAIL in sent, publish to Notification Tracker
                                                                                  // with next status -> SENT
                                                                                  .flatMap(publishResponse -> sendNotificationOnStatusQueue(emailPresaInCaricoInfo,
                                                                                                                                            SENT.getStatusTransactionTableCompliant(),
                                                                                                                                            new DigitalProgressStatusDto().generatedMessage(
                                                                                                                                                    generatedMessageDto.get())))

                                                                                  .flatMap(sendMessageResponse -> {
                                                                                      log.debug("Il messaggio è stato gestito " +
                                                                                                "correttamente e rimosso dalla coda " +
                                                                                                "d'errore {}",
                                                                                                emailSqsQueueName.errorName());
                                                                                      return deleteMessageFromErrorQueue(message);
                                                                                  })
                                                                                  .onErrorResume(sqsPublishException -> {
                                                                                      log.warn("Exception in processOnlyBodyRetry {}, {}", sqsPublishException, sqsPublishException.getMessage());
                                                                                      return checkTentativiEccessiviEmail(requestId,
                                                                                              requestDto,
                                                                                              emailPresaInCaricoInfo,
                                                                                              message);
                                                                                  });
                                                             }

                                                         })
                                                         .onErrorResume(it.pagopa.pn.ec.commons.exception.StatusToDeleteException.class,
                                                                        statusToDeleteException -> {
                                                                            log.debug("Il messaggio è stato rimosso dalla coda d'errore " + "per status toDelete: {}",
                                                                                      emailSqsQueueName.errorName());
                                                                            return sendNotificationOnStatusQueue(emailPresaInCaricoInfo,
                                                                                                                 DELETED.getStatusTransactionTableCompliant(),
                                                                                                                 new DigitalProgressStatusDto().generatedMessage(
                                                                                                                         new GeneratedMessageDto())).flatMap(
                                                                                    sendMessageResponse -> deleteMessageFromErrorQueue(
                                                                                            message));

                                                                        })
                                                         .onErrorResume(internalError -> sendNotificationOnStatusQueue(
                                                                 emailPresaInCaricoInfo,
                                                                 INTERNAL_ERROR.getStatusTransactionTableCompliant(),
                                                                 new DigitalProgressStatusDto()).then(deleteMessageFromErrorQueue(message)))
                                                         .doOnError(throwable->log.error("* FATAL * processOnlyBodyRetry {}, {}", throwable, throwable.getMessage()));
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
    public Mono<SendMessageResponse> sendNotificationOnBatchQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(emailSqsQueueName.batchName(), presaInCaricoInfo);
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnInteractiveQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(emailSqsQueueName.interactiveName(), presaInCaricoInfo);
    }

}
