package it.pagopa.pn.ec.pec.service.impl;

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
import it.pagopa.pn.ec.pec.configurationproperties.PecSqsQueueName;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.PEC;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.MessageContentTypeEnum.PLAIN;
import static java.time.OffsetDateTime.now;

@Service
@Slf4j
public class PecService extends PresaInCaricoService {

    private final SqsService sqsService;
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
//      Cast PresaInCaricoInfo to specific SmsPresaInCaricoInfo
        PecPresaInCaricoInfo pecPresaInCaricoInfo = (PecPresaInCaricoInfo) presaInCaricoInfo;
        return attachmentService.checkAllegatiPresence(pecPresaInCaricoInfo.getDigitalNotificationRequest().getAttachmentsUrls(),
                                                       presaInCaricoInfo.getXPagopaExtchCxId(),
                                                       true)
                                .flatMap(fileDownloadResponse -> {
                                    var digitalNotificationRequest = pecPresaInCaricoInfo.getDigitalNotificationRequest();
                                    digitalNotificationRequest.setRequestId(presaInCaricoInfo.getRequestIdx());
                                    return insertRequestFromPec(pecPresaInCaricoInfo.getXPagopaExtchCxId(),
                                                                digitalNotificationRequest).onErrorResume(throwable -> Mono.error(new EcInternalEndpointHttpException()));
                                })
                                .flatMap(requestDto -> sqsService.send(notificationTrackerSqsName.statoPecName(),
                                                                       new NotificationTrackerQueueDto(presaInCaricoInfo.getRequestIdx(),
                                                                                                       presaInCaricoInfo.getXPagopaExtchCxId(),
                                                                                                       now(),
                                                                                                       transactionProcessConfigurationProperties.pec(),
                                                                                                       transactionProcessConfigurationProperties.pecStartStatus(),
                                                                                                       "booked",
                                                                                                       null)))
                                .flatMap(sendMessageResponse -> {
                                    DigitalNotificationRequest.QosEnum qos = pecPresaInCaricoInfo.getDigitalNotificationRequest().getQos();
                                    if (qos == INTERACTIVE) {
                                        return sqsService.send(pecSqsQueueName.interactiveName(),
                                                               pecPresaInCaricoInfo.getDigitalNotificationRequest());
                                    } else if (qos == BATCH) {
                                        return sqsService.send(pecSqsQueueName.batchName(),
                                                               pecPresaInCaricoInfo.getDigitalNotificationRequest());
                                    } else {
                                        return Mono.empty();
                                    }
                                })
                                .then();
    }

    @SuppressWarnings("Duplicates")
    private Mono<RequestDto> insertRequestFromPec(String xPagopaExtchCxId, final DigitalNotificationRequest digitalNotificationRequest) {
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
}
