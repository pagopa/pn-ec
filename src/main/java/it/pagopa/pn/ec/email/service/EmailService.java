package it.pagopa.pn.ec.email.service;

import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.email.configurationproperties.EmailSqsQueueName;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.EMAIL;
import static java.time.OffsetDateTime.now;

@Service
@Slf4j
public class EmailService extends PresaInCaricoService {

    private final SqsService sqsService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final AttachmentServiceImpl attachmentService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final EmailSqsQueueName emailSqsQueueName;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;

    protected EmailService(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService,
                           GestoreRepositoryCall gestoreRepositoryCall1, AttachmentServiceImpl attachmentService,
                           NotificationTrackerSqsName notificationTrackerSqsName, EmailSqsQueueName emailSqsQueueName,
                           TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
        this.gestoreRepositoryCall = gestoreRepositoryCall1;
        this.attachmentService = attachmentService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.emailSqsQueueName = emailSqsQueueName;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {
        EmailPresaInCaricoInfo emailPresaInCaricoInfo = (EmailPresaInCaricoInfo) presaInCaricoInfo;

        return attachmentService.checkAllegatiPresence(emailPresaInCaricoInfo.getDigitalCourtesyMailRequest().getAttachmentsUrls(),
                                                       presaInCaricoInfo.getXPagopaExtchCxId(),
                                                       true)
                                .flatMap(fileDownloadResponse -> {
                                    var digitalNotificationRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
                                    digitalNotificationRequest.setRequestId(presaInCaricoInfo.getRequestIdx());
                                    return insertRequestFromEmail(emailPresaInCaricoInfo.getXPagopaExtchCxId(),
                                                                  digitalNotificationRequest).onErrorResume(throwable -> Mono.error(new EcInternalEndpointHttpException()));
                                })
                                .flatMap(requestDto -> sqsService.send(notificationTrackerSqsName.statoEmailName(),
                                                                       new NotificationTrackerQueueDto(presaInCaricoInfo.getRequestIdx(),
                                                                                                       presaInCaricoInfo.getXPagopaExtchCxId(),
                                                                                                       now(),
                                                                                                       transactionProcessConfigurationProperties.email(),
                                                                                                       transactionProcessConfigurationProperties.emailStartStatus(),
                                                                                                       "booked",
                                                                                                       null)))
                                .flatMap(sendMessageResponse -> {
                                    DigitalCourtesyMailRequest.QosEnum qos =
                                            emailPresaInCaricoInfo.getDigitalCourtesyMailRequest().getQos();
                                    if (qos == INTERACTIVE) {
                                        return sqsService.send(emailSqsQueueName.interactiveName(),
                                                               emailPresaInCaricoInfo.getDigitalCourtesyMailRequest());
                                    } else if (qos == BATCH) {
                                        return sqsService.send(emailSqsQueueName.errorName(),
                                                               emailPresaInCaricoInfo.getDigitalCourtesyMailRequest());
                                    } else {
                                        return Mono.empty();
                                    }
                                })
                                .then();
    }

    @SuppressWarnings("Duplicates")
    private Mono<RequestDto> insertRequestFromEmail(String xPagopaExtchCxId, final DigitalCourtesyMailRequest digitalCourtesyMailRequest) {
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
}
