package it.pagopa.pn.ec.email.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.ses.SesSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital;
import static it.pagopa.pn.ec.commons.service.SesService.DEFAULT_RETRY_STRATEGY;
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
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;

    protected EmailService(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService,
                           SesService sesService, GestoreRepositoryCall gestoreRepositoryCall1, AttachmentServiceImpl attachmentService,
                           NotificationTrackerSqsName notificationTrackerSqsName, EmailSqsQueueName emailSqsQueueName,
                           TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
        this.sesService = sesService;
        this.gestoreRepositoryCall = gestoreRepositoryCall1;
        this.attachmentService = attachmentService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.emailSqsQueueName = emailSqsQueueName;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {

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
                                                                                                                transactionProcessConfigurationProperties.emailStartStatus(),
                                                                                                                "booked",
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
                                })

                                .then();
    }

    @SuppressWarnings("Duplicates")
    private Mono<RequestDto> insertRequestFromEmail(final DigitalCourtesyMailRequest digitalCourtesyMailRequest, String xPagopaExtchCxId) {
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
    public void lavorazioneRichiesta(final EmailPresaInCaricoInfo emailPresaInCaricoInfo, final Acknowledgment acknowledgment) {
        log.info("<-- START LAVORAZIONE RICHIESTA EMAIL -->");
        logIncomingMessage(emailSqsQueueName.interactiveName(), emailPresaInCaricoInfo);
        var digitalCourtesyMailRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
        if (!digitalCourtesyMailRequest.getAttachmentsUrls().isEmpty()) {
            processWithAttach(emailPresaInCaricoInfo, acknowledgment);
        } else {
            processOnlyBody(emailPresaInCaricoInfo, acknowledgment);
        }
    }

    private void processWithAttach(final EmailPresaInCaricoInfo emailPresaInCaricoInfo, final Acknowledgment acknowledgment) {

        var digitalCourtesyMailRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();

        AtomicReference<GeneratedMessageDto> generatedMessageDto = new AtomicReference<>();

        // Try to send EMAIL
        attachmentService.getAllegatiPresignedUrlOrMetadata(digitalCourtesyMailRequest.getAttachmentsUrls(),
                                                            emailPresaInCaricoInfo.getXPagopaExtchCxId(),
                                                            false).map(this::convertiUrl).collectList().flatMap(attList -> {
                             EmailField mailFld = compilaMail(digitalCourtesyMailRequest);
                             mailFld.setEmailAttachments(attList);
                             return sesService.send(mailFld);
                         })

                         // The EMAIL in sent, publish to Notification Tracker with next status -> SENT
                         .flatMap(publishResponse -> {
                             generatedMessageDto.set(new GeneratedMessageDto().id(publishResponse.messageId()).system("systemPlaceholder"));
                             return sqsService.send(notificationTrackerSqsName.statoEmailName(),
                                                    createNotificationTrackerQueueDtoDigital(emailPresaInCaricoInfo,
                                                                                             "booked",
                                                                                             "sent",
                                                                                             new DigitalProgressStatusDto().generatedMessage(
                                                                                                     generatedMessageDto.get())));
                         })

                         // Delete from queue
                         .doOnSuccess(result -> acknowledgment.acknowledge())

                         // An error occurred during EMAIL send, start retries
                         .retryWhen(DEFAULT_RETRY_STRATEGY)

                         // The maximum number of retries has ended
                         .onErrorResume(SesSendException.SesMaxRetriesExceededException.class,
                                        sesMaxRetriesExceeded -> emailRetriesExceeded(acknowledgment, emailPresaInCaricoInfo))

                         // An error occurred during SQS publishing to the Notification Tracker -> Publish to Errori EMAIL queue and
                         // notify to retry update status only
                         // TODO: CHANGE THE PAYLOAD
                         .onErrorResume(SqsPublishException.class,
                                        sqsPublishException -> sqsService.send(emailSqsQueueName.errorName(), emailPresaInCaricoInfo))

                         .subscribe();
    }

    private void processOnlyBody(final EmailPresaInCaricoInfo emailPresaInCaricoInfo, final Acknowledgment acknowledgment) {

        var digitalCourtesyMailRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();

        AtomicReference<GeneratedMessageDto> generatedMessageDto = new AtomicReference<>();

        // Try to send EMAIL
        EmailField mailFld = compilaMail(digitalCourtesyMailRequest);

        sesService.send(mailFld)

                  // The EMAIL in sent, publish to Notification Tracker with next status -> SENT
                  .flatMap(publishResponse -> {
                      generatedMessageDto.set(new GeneratedMessageDto().id(publishResponse.messageId()).system("systemPlaceholder"));
                      return sqsService.send(notificationTrackerSqsName.statoEmailName(),
                                             createNotificationTrackerQueueDtoDigital(emailPresaInCaricoInfo,
                                                                                      "booked",
                                                                                      "sent",
                                                                                      new DigitalProgressStatusDto().generatedMessage(
                                                                                              generatedMessageDto.get())));
                  })

                  // Delete from queue
                  .doOnSuccess(result -> acknowledgment.acknowledge())

                  // An error occurred during EMAIL send, start retries
                  .retryWhen(DEFAULT_RETRY_STRATEGY)

                  // The maximum number of retries has ended
                  .onErrorResume(SesSendException.SesMaxRetriesExceededException.class,
                                 sesMaxRetriesExceeded -> emailRetriesExceeded(acknowledgment, emailPresaInCaricoInfo))

                  // An error occurred during SQS publishing to the Notification Tracker -> Publish to Errori EMAIL queue and
                  // notify to retry update status only
                  // TODO: CHANGE THE PAYLOAD
                  .onErrorResume(SqsPublishException.class,
                                 sqsPublishException -> sqsService.send(emailSqsQueueName.errorName(), emailPresaInCaricoInfo))

                  .subscribe();
    }

    private EmailAttachment convertiUrl(FileDownloadResponse resp) {
        return EmailAttachment.builder().nameWithExtension(resp.getKey()).url(resp.getDownload().getUrl()).build();
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

    private Mono<SendMessageResponse> emailRetriesExceeded(final Acknowledgment acknowledgment,
                                                           final EmailPresaInCaricoInfo emailPresaInCaricoInfo) {

        // Publish to Notification Tracker with next status -> RETRY
        return sqsService.send(notificationTrackerSqsName.statoEmailName(),
                               createNotificationTrackerQueueDtoDigital(emailPresaInCaricoInfo,
                                                                        "booked",
                                                                        "retry",
                                                                        new DigitalProgressStatusDto()))

                         // Publish to ERRORI EMAIL queue
                         .then(sqsService.send(emailSqsQueueName.errorName(), emailPresaInCaricoInfo))

                         // Delete from queue
                         .doOnSuccess(result -> acknowledgment.acknowledge());
    }
}
