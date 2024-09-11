package it.pagopa.pn.ec.sercq.service.impl;

import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.*;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import java.time.Duration;
import static it.pagopa.pn.ec.commons.constant.Status.BOOKED;
import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.PEC;


@Service
@CustomLog
public class SercqService extends PresaInCaricoService implements QueueOperationsService {
    private final AttachmentService attachmentService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final SqsService sqsService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;


    private final Retry PRESA_IN_CARICO_RETRY_STRATEGY = Retry.backoff(3, Duration.ofMillis(500))
            .doBeforeRetry(retrySignal -> log.debug("Retry number {}, caused by : {}", retrySignal.totalRetries(), retrySignal.failure().getMessage(), retrySignal.failure()));

    public SercqService(AuthService authService, AttachmentService attachmentService, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService, NotificationTrackerSqsName notificationTrackerSqsName) {
        super(authService);
        this.attachmentService = attachmentService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.sqsService = sqsService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
    }


    @Override
    public Mono<Void> specificPresaInCarico(PresaInCaricoInfo presaInCaricoInfo) {

        var pecPresaInCaricoInfo = (PecPresaInCaricoInfo) presaInCaricoInfo;
        var requestIdx = pecPresaInCaricoInfo.getRequestIdx();
        var xPagopaExtchCxId = pecPresaInCaricoInfo.getXPagopaExtchCxId();
        var digitalNotificationRequest = pecPresaInCaricoInfo.getDigitalNotificationRequest();

        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PRESA_IN_CARICO_SERCQ, presaInCaricoInfo);

        digitalNotificationRequest.setRequestId(requestIdx);

        return attachmentService.getAllegatiPresignedUrlOrMetadata(pecPresaInCaricoInfo.getDigitalNotificationRequest()
                        .getAttachmentUrls(), xPagopaExtchCxId, true)
                .retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY)
                .then(insertRequestFromSercq(digitalNotificationRequest, xPagopaExtchCxId))
                .flatMap(requestDto -> sendNotificationOnStatusQueue(pecPresaInCaricoInfo,
                        BOOKED.getStatusTransactionTableCompliant(),
                        new DigitalProgressStatusDto()).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY))
                .then()
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PRESA_IN_CARICO_SERCQ, result));
    }

    public Mono<RequestDto> insertRequestFromSercq(final DigitalNotificationRequest digitalNotificationRequest, String xPagopaExtchCxId) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, INSERT_REQUEST_FROM_SERCQ, digitalNotificationRequest);
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
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, INSERT_REQUEST_FROM_SERCQ, result));
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnStatusQueue(PresaInCaricoInfo presaInCaricoInfo, String status,
                                                                   DigitalProgressStatusDto digitalProgressStatusDto) {
        return sqsService.send(notificationTrackerSqsName.statoSercqName(),
                createNotificationTrackerQueueDtoDigital(presaInCaricoInfo, status, digitalProgressStatusDto));    }
}
