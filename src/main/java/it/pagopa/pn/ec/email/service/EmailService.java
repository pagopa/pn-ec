package it.pagopa.pn.ec.email.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.RetryAttemptsExceededExeption;
import it.pagopa.pn.ec.commons.exception.ss.attachment.StatusToDeleteException;
import it.pagopa.pn.ec.commons.model.pojo.MonoResultWrapper;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.policy.Policy;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.SesService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.email.configurationproperties.EmailSqsQueueName;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.atomic.AtomicReference;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital;
import static it.pagopa.pn.ec.commons.service.SesService.DEFAULT_RETRY_STRATEGY;
import static it.pagopa.pn.ec.commons.utils.ReactorUtils.pullFromMonoUntilIsEmpty;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.MessageContentTypeEnum.HTML;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.EMAIL;

@Service
@Slf4j
public class EmailService extends PresaInCaricoService {

    private final SqsService sqsService;
    private final SesService sesService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final AttachmentServiceImpl attachmentService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final EmailSqsQueueName emailSqsQueueName;
    private final DownloadCall downloadCall;

    private String idSaved;

    private static final String GENERIC_ERROR = "Errore generico";

    protected EmailService(AuthService authService//
            , GestoreRepositoryCall gestoreRepositoryCall//
            , SqsService sqsService//
            , SesService sesService//
            , GestoreRepositoryCall gestoreRepositoryCall1//
            , AttachmentServiceImpl attachmentService//
            , NotificationTrackerSqsName notificationTrackerSqsName//
            , EmailSqsQueueName emailSqsQueueName//
            , DownloadCall downloadCall//
                          ) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
        this.sesService = sesService;
        this.gestoreRepositoryCall = gestoreRepositoryCall1;
        this.attachmentService = attachmentService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.emailSqsQueueName = emailSqsQueueName;
        this.downloadCall = downloadCall;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {
        log.info("<-- START PRESA IN CARICO EMAIL --> Request ID: {}, Client ID: {}",
                 presaInCaricoInfo.getRequestIdx(),
                 presaInCaricoInfo.getXPagopaExtchCxId());

        var emailPresaInCaricoInfo = (EmailPresaInCaricoInfo) presaInCaricoInfo;

        var digitalNotificationRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
        digitalNotificationRequest.setRequestId(presaInCaricoInfo.getRequestIdx());

        return attachmentService.getAllegatiPresignedUrlOrMetadata(emailPresaInCaricoInfo.getDigitalCourtesyMailRequest()
                                                                                         .getAttachmentsUrls(),
                                                                   presaInCaricoInfo.getXPagopaExtchCxId(),
                                                                   true)

                                .then(insertRequestFromEmail(digitalNotificationRequest,
                                                             emailPresaInCaricoInfo.getXPagopaExtchCxId()).onErrorResume(throwable -> Mono.error(
                                        new EcInternalEndpointHttpException())))

                                .flatMap(requestDto -> sqsService.send(notificationTrackerSqsName.statoEmailName(),
                                                                       createNotificationTrackerQueueDtoDigital(emailPresaInCaricoInfo,
                                                                                                                BOOKED.getStatusTransactionTableCompliant(),
                                                                                                                new DigitalProgressStatusDto())))

                                .flatMap(sendMessageResponse -> {
                                    DigitalCourtesyMailRequest.QosEnum qos =
                                            emailPresaInCaricoInfo.getDigitalCourtesyMailRequest().getQos();
                                    if (qos == INTERACTIVE) {
                                        return sqsService.send(emailSqsQueueName.interactiveName(), emailPresaInCaricoInfo);
                                    } else if (qos == BATCH) {
                                        return sqsService.send(emailSqsQueueName.batchName(), emailPresaInCaricoInfo);
                                    } else {
                                        return Mono.empty();
                                    }
                                }).then();
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
            digitalRequestPersonalDto.setAttachmentsUrls(digitalCourtesyMailRequest.getAttachmentsUrls());
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
        }).flatMap(gestoreRepositoryCall::insertRichiesta);
    }

    @SqsListener(value = "${sqs.queue.email.interactive-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void lavorazioneRichiestaInteractive(final EmailPresaInCaricoInfo emailPresaInCaricoInfo, final Acknowledgment acknowledgment) {
        logIncomingMessage(emailSqsQueueName.interactiveName(), emailPresaInCaricoInfo);
        lavorazioneRichiesta(emailPresaInCaricoInfo).doOnSuccess(result -> acknowledgment.acknowledge()).subscribe();
    }

    @Scheduled(cron = "${cron.value.lavorazione-batch-email}")
    public void lavorazioneRichiestaBatch() {
        sqsService.getOneMessage(emailSqsQueueName.batchName(), EmailPresaInCaricoInfo.class)
                  .doOnNext(emailPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(emailSqsQueueName.batchName(),
                                                                                          emailPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                  .flatMap(emailPresaInCaricoInfoSqsMessageWrapper -> Mono.zip(Mono.just(emailPresaInCaricoInfoSqsMessageWrapper.getMessage()),
                                                                               lavorazioneRichiesta(emailPresaInCaricoInfoSqsMessageWrapper.getMessageContent())))
                  .flatMap(emailPresaInCaricoInfoSqsMessageWrapper -> sqsService.deleteMessageFromQueue(
                          emailPresaInCaricoInfoSqsMessageWrapper.getT1(),
                          emailSqsQueueName.batchName()))
                  .transform(pullFromMonoUntilIsEmpty())
                  .subscribe();
    }

    private static final Retry LAVORAZIONE_RICHIESTA_RETRY_STRATEGY = Retry.backoff(3, Duration.ofSeconds(2));

    Mono<SendMessageResponse> lavorazioneRichiesta(final EmailPresaInCaricoInfo emailPresaInCaricoInfo) {
        log.info("<-- START LAVORAZIONE RICHIESTA EMAIL --> Request ID : {}, Client ID : {}, QOS : {}",
                 emailPresaInCaricoInfo.getRequestIdx(),
                 emailPresaInCaricoInfo.getXPagopaExtchCxId(),
                 emailPresaInCaricoInfo.getDigitalCourtesyMailRequest().getQos());

        var digitalCourtesyMailRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
        var requestId = emailPresaInCaricoInfo.getXPagopaExtchCxId();
        AtomicReference<GeneratedMessageDto> generatedMessageDto = new AtomicReference<>();

        // Try to send EMAIL
        return attachmentService.getAllegatiPresignedUrlOrMetadata(digitalCourtesyMailRequest.getAttachmentsUrls(), requestId, false)
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
                                    return sendNotificationOnStatusQueue(emailPresaInCaricoInfo, SENT, generatedMessageDto.get())

                                                     // An error occurred during EMAIL send, start retries
                                                     .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)

                                                     // An error occurred during SQS publishing to the Notification Tracker -> Publish to
                                                     // Errori EMAIL queue and
                                                     // notify to retry update status only
                                                     // TODO: CHANGE THE PAYLOAD
                                                     .onErrorResume(throwable -> sendNotificationOnErrorQueue(emailPresaInCaricoInfo));
                                })
                                // The maximum number of retries has ended
                                .onErrorResume(throwable ->
                                        sendNotificationOnStatusQueue(emailPresaInCaricoInfo, RETRY)
                                                               // Publish to ERRORI EMAIL queue
                                                               .then(sendNotificationOnErrorQueue(emailPresaInCaricoInfo)))
                                                               .onErrorResume(internalError -> sendNotificationOnStatusQueue(emailPresaInCaricoInfo, INTERNAL_ERROR));
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

    @Scheduled(cron = "${cron.value.gestione-retry-email}")
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
        if (!digitalCourtesyMailRequest.getAttachmentsUrls().isEmpty()) {
            return processWithAttachRetry(emailPresaInCaricoInfo, message);
        } else {
            return processOnlyBodyRerty(emailPresaInCaricoInfo, message);
        }
    }


    private Mono<DeleteMessageResponse> processWithAttachRetry(final EmailPresaInCaricoInfo emailPresaInCaricoInfo, Message message) {
        log.info("<-- START PROCESS WITH ATTACH RETRY--> Request ID : {}, Client ID : {}",
                 emailPresaInCaricoInfo.getRequestIdx(),
                 emailPresaInCaricoInfo.getXPagopaExtchCxId());
        var digitalCourtesyMailRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
        var requestId = emailPresaInCaricoInfo.getRequestIdx();
        var clientId = emailPresaInCaricoInfo.getXPagopaExtchCxId();
        Policy retryPolicies = new Policy();
        String toDelete = "toDelete";
        AtomicReference<GeneratedMessageDto> generatedMessageDto = new AtomicReference<>();

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
                                    }).flatMap(requestDto ->
                                                       // Try to send EMAIL
                                                       attachmentService.getAllegatiPresignedUrlOrMetadata(digitalCourtesyMailRequest.getAttachmentsUrls(),
                                                                                                           emailPresaInCaricoInfo.getXPagopaExtchCxId(),
                                                                                                           false)

                                                                        .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)

                                                                        .filter(fileDownloadResponse ->
                                                                                        fileDownloadResponse.getDownload() != null)

                                                                        .flatMap(fileDownloadResponse -> downloadCall.downloadFile(
                                                                                                                             fileDownloadResponse.getDownload().getUrl())
                                                                                                                     .retryWhen(
                                                                                                                             LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                                                                                                                     .map(outputStream -> EmailAttachment.builder()
                                                                                                                                                         .nameWithExtension(
                                                                                                                                                                 fileDownloadResponse.getKey())
                                                                                                                                                         .content(
                                                                                                                                                                 outputStream)
                                                                                                                                                         .build()))

                                                                        .collectList()

                                                                        .flatMap(attList -> {
                                                                            EmailField mailFld = compilaMail(digitalCourtesyMailRequest);
                                                                            mailFld.setEmailAttachments(attList);
                                                                            return sesService.send(mailFld);
                                                                        })

                                                                        .retryWhen(DEFAULT_RETRY_STRATEGY)

                                                                        .map(this::createGeneratedMessageDto)

                                                                        // The EMAIL in sent, publish to Notification Tracker with next
                                                                        // status -> SENT
                                                                        .flatMap(publishResponse -> sendNotificationOnStatusQueue(emailPresaInCaricoInfo, SENT, generatedMessageDto.get()))
                                                                        .flatMap(sendMessageResponse -> {
                                                                            log.debug(
                                                                                    "Il messaggio è stato gestito correttamente e rimosso" +
                                                                                    " dalla coda d'errore", emailSqsQueueName.errorName());
                                                                            return deleteFromErrorQueue(message);
                                                                        })
                                                                        .onErrorResume(sqsPublishException -> {
                                                                            if (idSaved == null) {
                                                                                idSaved = requestId;
                                                                            }
                                                                            if (requestDto.getRequestMetadata()
                                                                                          .getRetry()
                                                                                          .getRetryStep()
                                                                                          .compareTo(BigDecimal.valueOf(3)) > 0) {
                                                                                // operazioni per la rimozione del messaggio
                                                                                log.debug(
                                                                                        "Il messaggio è stato rimosso dalla coda d'errore" +
                                                                                        " per eccessivi tentativi: {}",
                                                                                        emailSqsQueueName.errorName());
                                                                                return sendNotificationOnStatusQueue(emailPresaInCaricoInfo, ERROR)
                                                                                                 .flatMap(sendMessageResponse -> deleteFromErrorQueue(message));
                                                                            }
                                                                            return Mono.empty();
                                                                        })

                                              )//              Catch errore tirato per lo stato toDelete
                                    .onErrorResume(RetryAttemptsExceededExeption.class, retryAttemptsExceededExeption -> {
                                        log.debug("Il messaggio è stato rimosso dalla coda d'errore per status toDelete: {}",
                                                  emailSqsQueueName.errorName());
                                        return sendNotificationOnStatusQueue(emailPresaInCaricoInfo, DELETED)
                                                         .flatMap(sendMessageResponse -> deleteFromErrorQueue(message));


                                    })
                                      .onErrorResume(internalError -> sendNotificationOnStatusQueue(emailPresaInCaricoInfo, INTERNAL_ERROR)
                                      .then(deleteFromErrorQueue(message)));
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

    private Mono<DeleteMessageResponse> processOnlyBodyRerty(final EmailPresaInCaricoInfo emailPresaInCaricoInfo, Message message) {

        var digitalCourtesyMailRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
        var requestId = emailPresaInCaricoInfo.getRequestIdx();
        var clientId = emailPresaInCaricoInfo.getXPagopaExtchCxId();
        Policy retryPolicies = new Policy();
        AtomicReference<GeneratedMessageDto> generatedMessageDto = new AtomicReference<>();
        String toDelete = "toDelete";
        // Try to send EMAIL
        EmailField mailFld = compilaMail(digitalCourtesyMailRequest);



        return gestoreRepositoryCall.getRichiesta(clientId, requestId)

//              check status toDelete
                                    .filter(requestDto -> !Objects.equals(requestDto.getStatusRequest(), toDelete))
//              se status toDelete throw Error
                                    .switchIfEmpty(Mono.error(new RetryAttemptsExceededExeption(
                                            "La lunghezza del valore non è maggiore di 5")))
//              check Id per evitare loop
                                    .filter(requestDto -> !Objects.equals(requestDto.getRequestIdx(), idSaved))
//              se il primo step, inizializza l'attributo retry
                                    .flatMap(requestDto -> {
                                        if (requestDto.getRequestMetadata().getRetry() == null) {
                                            log.debug("Primo tentativo di Retry");
                                            RetryDto retryDto = new RetryDto();
                                            return getMono(requestId, retryPolicies, requestDto, retryDto);

                                        } else {
                                            var retryNumber = requestDto.getRequestMetadata().getRetry().getRetryStep();
                                            log.debug(retryNumber + " tentativo di Retry");
                                            return Mono.just(requestDto);
                                        }
                                    }).filter(requestDto -> {

                    var dateTime1 = requestDto.getRequestMetadata().getRetry().getLastRetryTimestamp();
                    var dateTime2 = OffsetDateTime.now();
                    Duration duration = Duration.between(dateTime1, dateTime2);
                    int step = requestDto.getRequestMetadata().getRetry().getRetryStep().intValueExact();
                    long minutes = duration.toMinutes();
                    long minutesToCheck = requestDto.getRequestMetadata().getRetry().getRetryPolicy().get(step).longValue();
                    return minutes >= minutesToCheck;
                }).flatMap(requestDto -> {
                    requestDto.getRequestMetadata().getRetry().setLastRetryTimestamp(OffsetDateTime.now());
                    requestDto.getRequestMetadata()
                              .getRetry()
                              .setRetryStep(requestDto.getRequestMetadata().getRetry().getRetryStep().add(BigDecimal.ONE));
                    PatchDto patchDto = new PatchDto();
                    patchDto.setRetry(requestDto.getRequestMetadata().getRetry());
                    return gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto);
                })
                .flatMap(requestDto -> {
                    log.debug("requestDto Value:", requestDto.getRequestMetadata().getRetry());
                    return sesService.send(mailFld).retryWhen(DEFAULT_RETRY_STRATEGY)

                                     .map(this::createGeneratedMessageDto)
                                     // The EMAIL in sent, publish to Notification Tracker with next status -> SENT
                                     .flatMap(publishResponse -> sendNotificationOnStatusQueue(emailPresaInCaricoInfo, ERROR, generatedMessageDto.get()))

                                     .flatMap(sendMessageResponse -> {
                                         log.debug("Il messaggio è stato gestito correttamente e rimosso dalla coda d'errore",
                                                   emailSqsQueueName.errorName());
                                         return deleteFromErrorQueue(message);
                                     }).onErrorResume(sqsPublishException -> {
                                if (idSaved == null) {
                                    idSaved = requestId;
                                }
                                if (requestDto.getRequestMetadata().getRetry().getRetryStep().compareTo(BigDecimal.valueOf(3)) > 0) {
                                    // operazioni per la rimozione del messaggio
                                    log.debug("Il messaggio è stato rimosso dalla coda d'errore per eccessivi tentativi: {}",
                                            emailSqsQueueName.errorName());
                                    return sendNotificationOnStatusQueue(emailPresaInCaricoInfo, ERROR)
                                            .flatMap(sendMessageResponse -> deleteFromErrorQueue(message));
                                }
                                return Mono.empty();
                            });

                }).onErrorResume(RetryAttemptsExceededExeption.class, retryAttemptsExceededExeption -> {
                    log.debug("Il messaggio è stato rimosso dalla coda d'errore per status toDelete: {}", emailSqsQueueName.errorName());
                    return sendNotificationOnStatusQueue(emailPresaInCaricoInfo, DELETED).flatMap(sendMessageResponse -> deleteFromErrorQueue(message));
                }).onErrorResume(internalError -> sendNotificationOnStatusQueue(emailPresaInCaricoInfo, INTERNAL_ERROR)
                        .then(deleteFromErrorQueue(message)));
    }


    private GeneratedMessageDto createGeneratedMessageDto(SendRawEmailResponse publishResponse) {
        return new GeneratedMessageDto().id(publishResponse.messageId()).system("toBeDefined");
    }
    @Override
    protected Mono<DeleteMessageResponse> deleteFromErrorQueue(Message message) {
        return sqsService.deleteMessageFromQueue(message, emailSqsQueueName.errorName());
    }
    @Override
    protected Mono<SendMessageResponse> sendNotificationOnStatusQueue(PresaInCaricoInfo presaInCaricoInfo, Status status)
    {
        return sqsService.send(notificationTrackerSqsName.statoEmailName(),
                        createNotificationTrackerQueueDtoDigital(presaInCaricoInfo,
                                status.getStatusTransactionTableCompliant(),
                                new DigitalProgressStatusDto().generatedMessage(new GeneratedMessageDto())));
    }

    @Override
    protected Mono<SendMessageResponse> sendNotificationOnErrorQueue(PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(emailSqsQueueName.errorName(), presaInCaricoInfo);
    }

    private Mono<SendMessageResponse> sendNotificationOnStatusQueue(PresaInCaricoInfo presaInCaricoInfo, Status status, GeneratedMessageDto generatedMessageDto)
    {
        return sqsService.send(notificationTrackerSqsName.statoEmailName(),
                createNotificationTrackerQueueDtoDigital(presaInCaricoInfo,
                        status.getStatusTransactionTableCompliant(),
                        new DigitalProgressStatusDto().generatedMessage(generatedMessageDto)));
    }

}
