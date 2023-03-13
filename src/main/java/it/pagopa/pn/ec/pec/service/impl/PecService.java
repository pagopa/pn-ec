package it.pagopa.pn.ec.pec.service.impl;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.aruba.ArubaSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import static it.pagopa.pn.ec.commons.configuration.retry.RetryStrategy.DEFAULT_BACKOFF_RETRY_STRATEGY;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.encodeMessageId;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.PEC;
import static java.time.OffsetDateTime.now;

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
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;

    protected PecService(AuthService authService, ArubaCall arubaCall, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService
            , AttachmentServiceImpl attachmentService, DownloadCall downloadCall, ArubaSecretValue arubaSecretValue,
                         NotificationTrackerSqsName notificationTrackerSqsName, PecSqsQueueName pecSqsQueueName,
                         TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
        super(authService, gestoreRepositoryCall);
        this.arubaCall = arubaCall;
        this.sqsService = sqsService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.attachmentService = attachmentService;
        this.downloadCall = downloadCall;
        this.arubaSecretValue = arubaSecretValue;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.pecSqsQueueName = pecSqsQueueName;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {
//      Cast PresaInCaricoInfo to specific PecPresaInCaricoInfo
        var pecPresaInCaricoInfo = (PecPresaInCaricoInfo) presaInCaricoInfo;
        pecPresaInCaricoInfo.setStatusAfterStart("booked");
        var digitalNotificationRequest = pecPresaInCaricoInfo.getDigitalNotificationRequest();
        digitalNotificationRequest.setRequestId(presaInCaricoInfo.getRequestIdx());
        var xPagopaExtchCxId = presaInCaricoInfo.getXPagopaExtchCxId();

        return attachmentService.getAllegatiPresignedUrlOrMetadata(pecPresaInCaricoInfo.getDigitalNotificationRequest()
                                                                                       .getAttachmentsUrls(), xPagopaExtchCxId, true)

                                .then(insertRequestFromPec(digitalNotificationRequest,
                                                           xPagopaExtchCxId).onErrorResume(throwable -> Mono.error(new EcInternalEndpointHttpException())))

                                .flatMap(requestDto -> sqsService.send(notificationTrackerSqsName.statoPecName(),
                                                                       new NotificationTrackerQueueDto(presaInCaricoInfo.getRequestIdx(),
                                                                                                       presaInCaricoInfo.getXPagopaExtchCxId(),
                                                                                                       now(),
                                                                                                       transactionProcessConfigurationProperties.pec(),
                                                                                                       transactionProcessConfigurationProperties.pecStartStatus(),
                                                                                                       pecPresaInCaricoInfo.getStatusAfterStart(),
                                                                                                       // TODO: SET eventDetails
                                                                                                       "",
                                                                                                       null)))
//                              Publish to PEC INTERACTIVE or PEC BATCH
                                .flatMap(sendMessageResponse -> {
                                    DigitalNotificationRequest.QosEnum qos = pecPresaInCaricoInfo.getDigitalNotificationRequest().getQos();
                                    if (qos == INTERACTIVE) {
                                        return sqsService.send(pecSqsQueueName.interactiveName(), pecPresaInCaricoInfo);
                                    } else if (qos == BATCH) {
                                        return sqsService.send(pecSqsQueueName.batchName(), pecPresaInCaricoInfo);
                                    } else {
                                        return Mono.empty();
                                    }
                                }).then();
    }

    @SuppressWarnings("Duplicates")
    private Mono<RequestDto> insertRequestFromPec(final DigitalNotificationRequest digitalNotificationRequest, String xPagopaExtchCxId) {
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
    public void lavorazioneRichiestaListener(final PecPresaInCaricoInfo pecPresaInCaricoInfo, final Acknowledgment acknowledgment) {

        log.info("<-- START LAVORAZIONE RICHIESTA PEC -->");
        logIncomingMessage(pecSqsQueueName.interactiveName(), pecPresaInCaricoInfo);

        lavorazioneRichiesta(pecPresaInCaricoInfo).thenReturn(acknowledgment.acknowledge()).subscribe();
    }

    Mono<SendMessageResponse> lavorazioneRichiesta(final PecPresaInCaricoInfo pecPresaInCaricoInfo) {

        var requestIdx = pecPresaInCaricoInfo.getRequestIdx();
        var xPagopaExtchCxId = pecPresaInCaricoInfo.getXPagopaExtchCxId();
        var digitalNotificationRequest = pecPresaInCaricoInfo.getDigitalNotificationRequest();

//      Get attachment presigned url Flux
        return attachmentService.getAllegatiPresignedUrlOrMetadata(digitalNotificationRequest.getAttachmentsUrls(), xPagopaExtchCxId, false)
                                .retryWhen(DEFAULT_BACKOFF_RETRY_STRATEGY.onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                    throw new RuntimeException("Retry exhausted for retrieving the presigned url");
                                }))

                                .filter(fileDownloadResponse -> fileDownloadResponse.getDownload() != null)

//                              Download the attachment and create the EmailAttachment object
                                .flatMap(fileDownloadResponse -> {
                                    var url = fileDownloadResponse.getDownload().getUrl();
                                    return downloadCall.downloadFile(url)
                                                       .retryWhen(DEFAULT_BACKOFF_RETRY_STRATEGY.onRetryExhaustedThrow((retryBackoffSpec,
                                                                                                                        retrySignal) -> {
                                                           throw new RuntimeException(String.format(
                                                                   "Exhausted retries for attachment download at %s",
                                                                   url));
                                                       }))
                                                       .map(outputStream -> EmailAttachment.builder()
                                                                                           .nameWithExtension(fileDownloadResponse.getKey())
                                                                                           .content(outputStream)
                                                                                           .build());
                                })

//                              Convert to Mono<List>
                                .collectList()

//                              Create EmailField object with request info and attachments. If there are no attachments fileDownloadResponses is empty
                                .map(fileDownloadResponses -> EmailField.builder()
                                                                        .msgId(encodeMessageId(requestIdx, xPagopaExtchCxId))
                                                                        .from(arubaSecretValue.getPecUsername())
                                                                        .to(digitalNotificationRequest.getReceiverDigitalAddress())
                                                                        .subject(digitalNotificationRequest.getSubjectText())
                                                                        .text(digitalNotificationRequest.getMessageText())
                                                                        .contentType(digitalNotificationRequest.getMessageContentType()
                                                                                                               .getValue())
                                                                        .emailAttachments(fileDownloadResponses)
                                                                        .build())

//                              Convert EmailField to mime message wrapped in <![CDATA[...]]>
                                .map(EmailUtils::getMimeMessageInCDATATag)

//                              Send PEC. Aruba call retries are handled in their implementations
                                .flatMap(mimeMessageInCdata -> {
                                    var sendMail = new SendMail();
                                    sendMail.setData(mimeMessageInCdata);
                                    return arubaCall.sendMail(sendMail);
                                })

//                              If the errcode is different from 0 it means that the sending was not successful, throw ArubaSendException
                                .handle((sendMailResponse, sink) -> {
                                    if (sendMailResponse.getErrcode() != 0) {
                                        sink.error(new ArubaSendException());
                                    } else {
                                        sink.next(sendMailResponse);
                                    }
                                })

//                              Cast the emitted value from the handle operator
                                .cast(SendMailResponse.class)

//                              Create the GeneratedMessageDto object and set the id as the returned messageId from the SendMailResponse
                                .map(this::createGeneratedMessageDto)

                                .zipWith(gestoreRepositoryCall.setMessageIdInRequestMetadata(requestIdx))

                                .flatMap(objects -> sqsService.send(notificationTrackerSqsName.statoPecName(),
                                                                    new NotificationTrackerQueueDto(requestIdx,
                                                                                                    xPagopaExtchCxId,
                                                                                                    now(),
                                                                                                    transactionProcessConfigurationProperties.pec(),
                                                                                                    pecPresaInCaricoInfo.getStatusAfterStart(),
                                                                                                    "sent",
                                                                                                    // TODO: SET eventDetails
                                                                                                    "",
                                                                                                    objects.getT1()))
                                                              .retryWhen(DEFAULT_BACKOFF_RETRY_STRATEGY)

//                                                            An error occurred during SQS publishing to the Notification Tracker ->
//                                                            Publish to Errori PEC queue and notify to retry update status only
//                                                            TODO: CHANGE THE PAYLOAD
                                                              .onErrorResume(SqsPublishException.class,
                                                                             sqsPublishException -> sqsService.send(pecSqsQueueName.errorName(),
                                                                                                                    pecPresaInCaricoInfo)))
                                .doOnError(throwable -> {
                                    log.info("An error occurred during lavorazione PEC");
                                    log.error(throwable.getMessage());
                                })

                                .onErrorResume(throwable -> sqsService.send(notificationTrackerSqsName.statoPecName(),
                                                                            new NotificationTrackerQueueDto(requestIdx,
                                                                                                            xPagopaExtchCxId,
                                                                                                            now(),
                                                                                                            transactionProcessConfigurationProperties.pec(),
                                                                                                            pecPresaInCaricoInfo.getStatusAfterStart(),
                                                                                                            "retry",
                                                                                                            // TODO: SET
                                                                                                            //  eventDetails
                                                                                                            "",
                                                                                                            null))

//                                                                    Publish to ERRORI PEC queue
                                                                      .then(sqsService.send(pecSqsQueueName.errorName(),
                                                                                            pecPresaInCaricoInfo)));
    }

    private GeneratedMessageDto createGeneratedMessageDto(SendMailResponse sendMailResponse) {
        var errstr = sendMailResponse.getErrstr();
//      Remove the last 2 char '\r\n'
        return new GeneratedMessageDto().id(errstr.substring(0, errstr.length() - 2)).system("toBeDefined");
    }
}
