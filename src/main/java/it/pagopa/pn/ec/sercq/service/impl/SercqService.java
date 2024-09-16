package it.pagopa.pn.ec.sercq.service.impl;

import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.*;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sercq.model.pojo.SercqPresaInCaricoInfo;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import java.time.Duration;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoSercq;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.insertRequestFromDigitalNotificationRequest;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.SERCQ;


@Service
@CustomLog
public class SercqService extends PresaInCaricoService implements QueueOperationsService {
    private final AttachmentService attachmentService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final SqsService sqsService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;

    @Value("${sercq.receiver-digital-address}")
    private String receiverDigitalAddress;


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

        var pecPresaInCaricoInfo = (SercqPresaInCaricoInfo) presaInCaricoInfo;
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
                        controlloReciverDigitalAddress(pecPresaInCaricoInfo.getDigitalNotificationRequest().getReceiverDigitalAddress()).getStatusTransactionTableCompliant(),
                        new DigitalProgressStatusDto()).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY))
                .onErrorResume(SqsClientException.class,
                        sqsClientException -> sendNotificationOnStatusQueue(pecPresaInCaricoInfo,
                                INTERNAL_ERROR.getStatusTransactionTableCompliant(),
                                new DigitalProgressStatusDto()).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY).then(Mono.error(
                                sqsClientException)))
                .then()
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PRESA_IN_CARICO_SERCQ, result));
    }

    public Mono<RequestDto> insertRequestFromSercq(final DigitalNotificationRequest digitalNotificationRequest, String xPagopaExtchCxId) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, INSERT_REQUEST_FROM_SERCQ, digitalNotificationRequest);


        return Mono.fromCallable(() ->
                    insertRequestFromDigitalNotificationRequest(digitalNotificationRequest, xPagopaExtchCxId, SERCQ)
                ).flatMap(gestoreRepositoryCall::insertRichiesta).retryWhen(PRESA_IN_CARICO_RETRY_STRATEGY)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, INSERT_REQUEST_FROM_SERCQ, result));
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnStatusQueue(PresaInCaricoInfo presaInCaricoInfo, String status,
                                                                   DigitalProgressStatusDto digitalProgressStatusDto) {
        return sqsService.send(notificationTrackerSqsName.statoSercqName(),
                createNotificationTrackerQueueDtoSercq(presaInCaricoInfo, status, digitalProgressStatusDto));

    }


    private Status controlloReciverDigitalAddress (String digitalAddress){
        String substringDigitalAddress = digitalAddress.split("\\?")[0];
                if(receiverDigitalAddress.equals(substringDigitalAddress)){
            return SENT;
        }
        return ADDRESS_ERROR;
    }
}
