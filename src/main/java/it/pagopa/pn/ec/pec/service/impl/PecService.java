package it.pagopa.pn.ec.pec.service.impl;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.RetryAttemptsExceededExeption;
import it.pagopa.pn.ec.commons.exception.aruba.ArubaSendException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.StatusToDeleteException;
import it.pagopa.pn.ec.commons.model.pojo.MonoResultWrapper;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.policy.Policy;
import it.pagopa.pn.ec.commons.rest.call.aruba.ArubaCall;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
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

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital;
import static it.pagopa.pn.ec.commons.utils.EmailUtils.getDomainFromAddress;
import static it.pagopa.pn.ec.commons.utils.ReactorUtils.pullFromMonoUntilIsEmpty;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.encodeMessageId;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.PEC;

@Service
@Slf4j
public class PecService extends PresaInCaricoService {

    private final SqsService sqsService;
    private final ArubaCall arubaCall;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final AttachmentServiceImpl attachmentService;
    private final DownloadCall downloadCall;
    private final ArubaSecretValue arubaSecretValue;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final PecSqsQueueName pecSqsQueueName;

    private String idSaved;

    protected PecService(AuthService authService, ArubaCall arubaCall, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService
            , AttachmentServiceImpl attachmentService, DownloadCall downloadCall, ArubaSecretValue arubaSecretValue,
                         NotificationTrackerSqsName notificationTrackerSqsName, PecSqsQueueName pecSqsQueueName) {
        super(authService, gestoreRepositoryCall);
        this.arubaCall = arubaCall;
        this.sqsService = sqsService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.attachmentService = attachmentService;
        this.downloadCall = downloadCall;
        this.arubaSecretValue = arubaSecretValue;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.pecSqsQueueName = pecSqsQueueName;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {
        log.info("<-- START specificPresaInCarico --> richiesta: {}", presaInCaricoInfo.getRequestIdx());
//      Cast PresaInCaricoInfo to specific PecPresaInCaricoInfo
        var pecPresaInCaricoInfo = (PecPresaInCaricoInfo) presaInCaricoInfo;
        var xPagopaExtchCxId = pecPresaInCaricoInfo.getXPagopaExtchCxId();

        var digitalNotificationRequest = pecPresaInCaricoInfo.getDigitalNotificationRequest();
        digitalNotificationRequest.setRequestId(pecPresaInCaricoInfo.getRequestIdx());

        return attachmentService.getAllegatiPresignedUrlOrMetadata(pecPresaInCaricoInfo.getDigitalNotificationRequest()
                                                                                       .getAttachmentsUrls(), xPagopaExtchCxId, true)

                                .then(insertRequestFromPec(digitalNotificationRequest,
                                                           xPagopaExtchCxId).onErrorResume(throwable -> Mono.error(new EcInternalEndpointHttpException())))

                                .flatMap(requestDto -> sqsService.send(notificationTrackerSqsName.statoPecName(),
                                                                       createNotificationTrackerQueueDtoDigital(pecPresaInCaricoInfo,
                                                                                                                BOOKED.getStatusTransactionTableCompliant(),
                                                                                                                new DigitalProgressStatusDto()))

//                                                               Publish to PEC INTERACTIVE or PEC BATCH
                                                                 .flatMap(sendMessageResponse -> {
                                                                     DigitalNotificationRequest.QosEnum qos =
                                                                             pecPresaInCaricoInfo.getDigitalNotificationRequest().getQos();
                                                                     if (qos == INTERACTIVE) {
                                                                         return sqsService.send(pecSqsQueueName.interactiveName(),
                                                                                                pecPresaInCaricoInfo);
                                                                     } else if (qos == BATCH) {
                                                                         return sqsService.send(pecSqsQueueName.batchName(),
                                                                                                pecPresaInCaricoInfo);
                                                                     } else {
                                                                         return Mono.empty();
                                                                     }
                                                                 })).then();
    }

    @SuppressWarnings("Duplicates")
    private Mono<RequestDto> insertRequestFromPec(final DigitalNotificationRequest digitalNotificationRequest, String xPagopaExtchCxId) {
        log.info("<-- START insertRequestFromPec --> richiesta: {}", digitalNotificationRequest.getRequestId());
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
            digitalRequestPersonalDto.setAttachmentsUrls(digitalNotificationRequest.getAttachmentsUrls());
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
        }).flatMap(gestoreRepositoryCall::insertRichiesta);
    }

    @SqsListener(value = "${sqs.queue.pec.interactive-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void lavorazioneRichiestaInteractive(final PecPresaInCaricoInfo pecPresaInCaricoInfo, final Acknowledgment acknowledgment) {
        logIncomingMessage(pecSqsQueueName.interactiveName(), pecPresaInCaricoInfo);
        lavorazioneRichiesta(pecPresaInCaricoInfo).doOnNext(result -> acknowledgment.acknowledge()).subscribe();
    }

    @Scheduled(cron = "${cron.value.lavorazione-batch-pec}")
    public void lavorazioneRichiestaBatch() {

        sqsService.getOneMessage(pecSqsQueueName.batchName(), PecPresaInCaricoInfo.class)
                  .doOnNext(pecPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(pecSqsQueueName.batchName(),
                                                                                        pecPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                  .flatMap(pecPresaInCaricoInfoSqsMessageWrapper -> Mono.zip(Mono.just(pecPresaInCaricoInfoSqsMessageWrapper.getMessage()),
                                                                             lavorazioneRichiesta(pecPresaInCaricoInfoSqsMessageWrapper.getMessageContent())))
                  .flatMap(pecPresaInCaricoInfoSqsMessageWrapper -> sqsService.deleteMessageFromQueue(pecPresaInCaricoInfoSqsMessageWrapper.getT1(),
                                                                                                      pecSqsQueueName.batchName()))
                  .transform(pullFromMonoUntilIsEmpty())
                  .subscribe();
    }

    private static final Retry LAVORAZIONE_RICHIESTA_RETRY_STRATEGY = Retry.backoff(3, Duration.ofSeconds(2));

    public Mono<SendMessageResponse> lavorazioneRichiesta(final PecPresaInCaricoInfo pecPresaInCaricoInfo) {
        log.info("<-- START LAVORAZIONE RICHIESTA PEC --> richiesta: {}", pecPresaInCaricoInfo.getRequestIdx());

        var requestIdx = pecPresaInCaricoInfo.getRequestIdx();
        var xPagopaExtchCxId = pecPresaInCaricoInfo.getXPagopaExtchCxId();
        var digitalNotificationRequest = pecPresaInCaricoInfo.getDigitalNotificationRequest();

//      Get attachment presigned url Flux
        return attachmentService.getAllegatiPresignedUrlOrMetadata(digitalNotificationRequest.getAttachmentsUrls(), xPagopaExtchCxId, false)
                                .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)

                                .filter(fileDownloadResponse -> fileDownloadResponse.getDownload() != null)

                                .flatMap(fileDownloadResponse -> downloadCall.downloadFile(fileDownloadResponse.getDownload().getUrl())
                                                                             .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)
                                                                             .map(outputStream -> EmailAttachment.builder()
                                                                                                                 .nameWithExtension(
                                                                                                                         fileDownloadResponse.getKey())
                                                                                                                 .content(outputStream)
                                                                                                                 .build()))

//                              Convert to Mono<List>
                                .collectList()

//                              Create EmailField object with request info and attachments
                                .map(fileDownloadResponses -> EmailField.builder()
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
                                        sink.error(new ArubaSendException());
                                    } else {
                                        sink.next(sendMailResponse);
                                    }
                                })

                                .cast(SendMailResponse.class)

                                .map(this::createGeneratedMessageDto)

                                .zipWhen(generatedMessageDto -> gestoreRepositoryCall.setMessageIdInRequestMetadata(xPagopaExtchCxId,
                                                                                                                    requestIdx))

                                .flatMap(objects -> sqsService.send(notificationTrackerSqsName.statoPecName(),
                                                                    createNotificationTrackerQueueDtoDigital(pecPresaInCaricoInfo,
                                                                                                             SENT.getStatusTransactionTableCompliant(),
                                                                                                             new DigitalProgressStatusDto().generatedMessage(
                                                                                                                     objects.getT1())))

//                                                            An error occurred during PEC send, start retries
                                                              .retryWhen(LAVORAZIONE_RICHIESTA_RETRY_STRATEGY)

//                                                            An error occurred during SQS publishing to the Notification Tracker ->
//                                                            Publish to Errori PEC queue and notify to retry update status only
//                                                            TODO: CHANGE THE PAYLOAD
                                                              .onErrorResume(throwable -> sqsService.send(pecSqsQueueName.errorName(),
                                                                                                          pecPresaInCaricoInfo)))

                                .doOnError(throwable -> log.error("An error occurred during lavorazione PEC {}", throwable.getMessage()))

                                .onErrorResume(throwable -> sqsService.send(notificationTrackerSqsName.statoPecName(),
                                                                            createNotificationTrackerQueueDtoDigital(pecPresaInCaricoInfo,
                                                                                                                     RETRY.getStatusTransactionTableCompliant(),
                                                                                                                     new DigitalProgressStatusDto()))

//                                                                    Publish to ERRORI PEC queue
                                                                      .then(sqsService.send(pecSqsQueueName.errorName(),
                                                                                            pecPresaInCaricoInfo))
                                                .onErrorResume(internalError -> {
                                                    log.error("Internal Error ---> {}", internalError.getMessage());
                                                    return sqsService.send(notificationTrackerSqsName.statoPecName(),
                                                            createNotificationTrackerQueueDtoDigital(pecPresaInCaricoInfo,
                                                                    RETRY.getStatusTransactionTableCompliant(),
                                                                    new DigitalProgressStatusDto()));

                                                })
                                );
    }

    private GeneratedMessageDto createGeneratedMessageDto(SendMailResponse sendMailResponse) {
        var errstr = sendMailResponse.getErrstr();
//      Remove the last 2 char '\r\n'
        return new GeneratedMessageDto().id(errstr.substring(0, errstr.length() - 2))
                                        .system(getDomainFromAddress(arubaSecretValue.getPecUsername()));
    }

    @Scheduled(cron = "${cron.value.gestione-retry-pec}")
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

    public Mono<DeleteMessageResponse> gestioneRetryPec(final PecPresaInCaricoInfo pecPresaInCaricoInfo, Message message) {

        log.info("<-- START GESTIONE RETRY PEC--> richiesta: {}", pecPresaInCaricoInfo.getRequestIdx());
        logIncomingMessage(pecSqsQueueName.errorName(), pecPresaInCaricoInfo);
        Policy retryPolicies = new Policy();

        var requestIdx = pecPresaInCaricoInfo.getRequestIdx();
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
                                            log.debug("Primo tentativo di Retry");
                                            RetryDto retryDto = new RetryDto();
                                            log.debug("policy" + retryPolicies.getPolicy().get("PEC"));
                                            retryDto.setRetryPolicy(retryPolicies.getPolicy().get("PEC"));
                                            retryDto.setRetryStep(BigDecimal.ZERO);
                                            retryDto.setLastRetryTimestamp(OffsetDateTime.now());
                                            requestDto.getRequestMetadata().setRetry(retryDto);
                                            PatchDto patchDto = new PatchDto();
                                            patchDto.setRetry(requestDto.getRequestMetadata().getRetry());
                                            return gestoreRepositoryCall.patchRichiesta(xPagopaExtchCxId, requestIdx, patchDto);

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
                                        return gestoreRepositoryCall.patchRichiesta(xPagopaExtchCxId, requestIdx, patchDto);
                                    }).flatMap(requestDto -> {
                    log.debug("requestDto Value: {}", requestDto.getRequestMetadata().getRetry());

//      Get attachment presigned url Flux
                    return attachmentService.getAllegatiPresignedUrlOrMetadata(pecPresaInCaricoInfo.getDigitalNotificationRequest()
                                                                                                   .getAttachmentsUrls(),
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
                                            .map(fileDownloadResponses -> EmailField.builder()
                                                                                    .msgId(encodeMessageId(xPagopaExtchCxId, requestIdx))
                                                                                    .from(arubaSecretValue.getPecUsername())
                                                                                    .to(pecPresaInCaricoInfo.getDigitalNotificationRequest()
                                                                                                            .getReceiverDigitalAddress())
                                                                                    .subject(pecPresaInCaricoInfo.getDigitalNotificationRequest()
                                                                                                                 .getSubjectText())
                                                                                    .text(pecPresaInCaricoInfo.getDigitalNotificationRequest()
                                                                                                              .getMessageText())
                                                                                    .contentType(pecPresaInCaricoInfo.getDigitalNotificationRequest()
                                                                                                                     .getMessageContentType()
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
                                                    sink.error(new ArubaSendException());
                                                } else {
                                                    sink.next(sendMailResponse);
                                                }
                                            })

                                            .cast(SendMailResponse.class)

                                            .map(this::createGeneratedMessageDto)

                                            .zipWhen(generatedMessageDto -> gestoreRepositoryCall.setMessageIdInRequestMetadata(
                                                    xPagopaExtchCxId,
                                                    requestIdx))

                                            .flatMap(objects -> sqsService.send(notificationTrackerSqsName.statoPecName(),
                                                                                createNotificationTrackerQueueDtoDigital(
                                                                                        pecPresaInCaricoInfo,
                                                                                        SENT.getStatusTransactionTableCompliant(),
                                                                                        new DigitalProgressStatusDto().generatedMessage(
                                                                                                objects.getT1()))))
                                            .flatMap(sendMessageResponse -> {
                                                log.debug("Il messaggio è stato gestito correttamente e rimosso dalla coda d'errore '{}'",
                                                          pecSqsQueueName.errorName());
                                                return sqsService.deleteMessageFromQueue(message, pecSqsQueueName.errorName());
                                            })
                                            .onErrorResume(sqsPublishException -> {
                                                if (idSaved == null) {
                                                    idSaved = requestIdx;
                                                }
                                                if (requestDto.getRequestMetadata()
                                                              .getRetry()
                                                              .getRetryStep()
                                                              .compareTo(BigDecimal.valueOf(3)) > 0) {
                                                    // operazioni per la rimozione del messaggio
                                                    log.debug("Il messaggio è stato rimosso dalla coda d'errore per eccessivi tentativi: " +
                                                              "{}", pecSqsQueueName.errorName());
                                                    return sqsService.send(notificationTrackerSqsName.statoEmailName(),
                                                                           createNotificationTrackerQueueDtoDigital(pecPresaInCaricoInfo,
                                                                                                                    ERROR.getStatusTransactionTableCompliant(),
                                                                                                                    new DigitalProgressStatusDto().generatedMessage(
                                                                                                                            new GeneratedMessageDto())))
                                                                     .flatMap(sendMessageResponse -> sqsService.deleteMessageFromQueue(
                                                                             message,
                                                                             pecSqsQueueName.errorName()));

                                                }
                                                return Mono.empty();
                                            });

                })//              Catch errore tirato per lo stato toDelete
                                    .onErrorResume(RetryAttemptsExceededExeption.class, retryAttemptsExceededExeption -> {
                                        log.debug("Il messaggio è stato rimosso dalla coda d'errore per status toDelete: {}",
                                                  pecSqsQueueName.errorName());
                                        return sqsService.send(notificationTrackerSqsName.statoEmailName(),
                                                               createNotificationTrackerQueueDtoDigital(pecPresaInCaricoInfo,
                                                                                                        DELETED.getStatusTransactionTableCompliant(),
                                                                                                        new DigitalProgressStatusDto().generatedMessage(
                                                                                                                new GeneratedMessageDto())))
                                                         .flatMap(sendMessageResponse -> sqsService.deleteMessageFromQueue(message,
                                                                                                                           pecSqsQueueName.errorName()));


                                    }).onErrorResume(throwable -> {
                    log.error(throwable.getMessage());
                    return Mono.empty();
                });
    }

}
