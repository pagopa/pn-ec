package it.pagopa.pn.ec.pec.service.impl;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.pec.configurationproperties.PecSqsQueueName;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.PEC;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.MessageContentTypeEnum.PLAIN;
import static java.time.OffsetDateTime.now;

@Service
@Slf4j
public class PecService extends PresaInCaricoService {

    private final SqsService sqsService;
    //ARUBA?
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final AttachmentServiceImpl attachmentService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final PecSqsQueueName pecSqsQueueName;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;


    protected PecService(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService,
                         AttachmentServiceImpl attachmentService, NotificationTrackerSqsName notificationTrackerSqsName,
                         PecSqsQueueName pecSqsQueueName,
                         TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.attachmentService = attachmentService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.pecSqsQueueName = pecSqsQueueName;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {
//      Cast PresaInCaricoInfo to specific PecPresaInCaricoInfo
        var pecPresaInCaricoInfo = (PecPresaInCaricoInfo) presaInCaricoInfo;
        var xPagopaExtchCxId = presaInCaricoInfo.getXPagopaExtchCxId();
        return attachmentService.checkAllegatiPresence(pecPresaInCaricoInfo.getDigitalNotificationRequest().getAttachmentsUrls(),
                                                       xPagopaExtchCxId,
                                                       true)
                                .flatMap(fileDownloadResponse -> {
                                   var digitalNotificationRequest = pecPresaInCaricoInfo.getDigitalNotificationRequest();
                                   digitalNotificationRequest.setRequestId(presaInCaricoInfo.getRequestIdx());
//                                 Insert request from PEC request and publish to Notification Tracker with next status -> BOOKED
                                   return insertRequestFromPec(digitalNotificationRequest, xPagopaExtchCxId)
                                           .onErrorResume(throwable -> Mono.error(new EcInternalEndpointHttpException()));
                               })
                                .flatMap(requestDto -> sqsService.send(notificationTrackerSqsName.statoPecName(),
                                                                      new NotificationTrackerQueueDto(presaInCaricoInfo.getRequestIdx(),
                                                                                                      presaInCaricoInfo.getXPagopaExtchCxId(),
                                                                                                      now(),
                                                                                                      transactionProcessConfigurationProperties.pec(),
                                                                                                      transactionProcessConfigurationProperties.pecStartStatus(),
                                                                                                      "booked",
                                                                                                      // TODO: SET eventDetails
                                                                                                      "",
                                                                                                      null)))
//                              Publish to PEC INTERACTIVE or PEC BATCH
                                .flatMap(sendMessageResponse -> {
                                   DigitalNotificationRequest.QosEnum qos = pecPresaInCaricoInfo.getDigitalNotificationRequest().getQos();
                                   if (qos == INTERACTIVE) {
                                       return sqsService.send(pecSqsQueueName.interactiveName(),
                                                              pecPresaInCaricoInfo);
                                   } else if (qos == BATCH) {
                                       return sqsService.send(pecSqsQueueName.batchName(),
                                                              pecPresaInCaricoInfo);
                                   } else {
                                       return Mono.empty();
                                   }
                               })
                                .then();
    }

    @SuppressWarnings("Duplicates")
    private Mono<RequestDto> insertRequestFromPec(final DigitalNotificationRequest digitalNotificationRequest,
                                                  String xPagopaExtchCxId) {
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
            digitalRequestMetadataDto.setMessageContentType(PLAIN);
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

//        lavorazioneRichiesta(pecPresaInCaricoInfo).doOnNext(result -> acknowledgment.acknowledge()).subscribe();
    }

//    public Mono<SendMessageResponse> lavorazioneRichiesta(final PecPresaInCaricoInfo pecPresaInCaricoInfo) {
////      Try to send PEC - Aruba?
//        return snsService.send(pecPresaInCaricoInfo.getDigitalNotificationRequest().getReceiverDigitalAddress(),
//                        pecPresaInCaricoInfo.getDigitalNotificationRequest().getSubjectText(),
//                        pecPresaInCaricoInfo.getDigitalNotificationRequest().getMessageText(),
//                        pecPresaInCaricoInfo.getDigitalNotificationRequest().getMessageContentType(),
//                        pecPresaInCaricoInfo.getDigitalNotificationRequest().getAttachmentsUrls())
//
////                       Retry to send PEC
//                .retryWhen(DEFAULT_RETRY_STRATEGY)
//
////                        Set message id after send
//                .map(this::createGeneratedMessageDto)
//
////                       The PEC in sent, publish to Notification Tracker with next status -> SENT
//                .flatMap(generatedMessageDto -> sqsService.send(notificationTrackerSqsName.statoSmsName(),
//                                new NotificationTrackerQueueDto(pecPresaInCaricoInfo.getRequestIdx(),
//                                        pecPresaInCaricoInfo.getXPagopaExtchCxId(),
//                                        now(),
//                                        transactionProcessConfigurationProperties.pec(),
//                                        pecPresaInCaricoInfo.getStatusAfterStart(),
//                                        "sent",
//                                        // TODO: SET eventDetails
//                                        "",
//                                        generatedMessageDto))
//
////                                                                An error occurred during SQS publishing to the Notification Tracker ->
////                                                                Publish to Errori PEC queue and notify to retry update status only
////                                                                TODO: CHANGE THE PAYLOAD
//                        .onErrorResume(SqsPublishException.class,
//                                sqsPublishException -> sqsService.send(pecSqsQueueName.errorName(),
//                                        pecPresaInCaricoInfo)))
//
////                       The maximum number of retries has ended
//                .onErrorResume(SnsSendException.SnsMaxRetriesExceededException.class,
//                        snsMaxRetriesExceeded -> sqsService.send(notificationTrackerSqsName.statoSmsName(),
//                                        new NotificationTrackerQueueDto(pecPresaInCaricoInfo.getRequestIdx(),
//                                                pecPresaInCaricoInfo.getXPagopaExtchCxId(),
//                                                now(),
//                                                transactionProcessConfigurationProperties.pec(),
//                                                pecPresaInCaricoInfo.getStatusAfterStart(),
//                                                "retry",
//                                                // TODO: SET eventDetails
//                                                "",
//                                                null))
//
////                                                                         Publish to ERRORI PEC queue
//                                .then(sqsService.send(pecSqsQueueName.errorName(),
//                                        pecPresaInCaricoInfo)));
//    }

    private GeneratedMessageDto createGeneratedMessageDto(PublishResponse publishResponse) {
        return new GeneratedMessageDto().id(publishResponse.messageId()).system("toBeDefined");
//        return null;
    }

}
