package it.pagopa.pn.ec.cartaceo.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.mapper.CartaceoMapper;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.cartaceo.CartaceoSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoPaper;
import static it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall.DEFAULT_RETRY_STRATEGY;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;

@Service
@Slf4j
public class CartaceoService extends PresaInCaricoService {


    private final SqsService sqsService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final AttachmentServiceImpl attachmentService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final CartaceoSqsQueueName cartaceoSqsQueueName;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;
    private final PaperMessageCall paperMessageCall;
    private final CartaceoMapper cartaceoMapper;

    protected CartaceoService(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService,
                              GestoreRepositoryCall gestoreRepositoryCall1, AttachmentServiceImpl attachmentService,
                              NotificationTrackerSqsName notificationTrackerSqsName, CartaceoSqsQueueName cartaceoSqsQueueName,
                              TransactionProcessConfigurationProperties transactionProcessConfigurationProperties, PaperMessageCall paperMessageCall,
                              CartaceoMapper cartaceoMapper) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
        this.gestoreRepositoryCall = gestoreRepositoryCall1;
        this.attachmentService = attachmentService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.cartaceoSqsQueueName = cartaceoSqsQueueName;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
        this.paperMessageCall = paperMessageCall;
        this.cartaceoMapper = cartaceoMapper;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(PresaInCaricoInfo presaInCaricoInfo) {
//        Cast PresaInCaricoInfo to specific CartaceoPresaInCaricoInfo
        var cartaceoPresaInCaricoInfo = (CartaceoPresaInCaricoInfo) presaInCaricoInfo;
        var xPagopaExtchCxId = presaInCaricoInfo.getXPagopaExtchCxId();
        List<String> attachmentsUri = getPaperUri(cartaceoPresaInCaricoInfo.getPaperEngageRequest().getAttachments());
        var paperNotificationRequest = cartaceoPresaInCaricoInfo.getPaperEngageRequest();

        return attachmentService.getAllegatiPresignedUrlOrMetadata(attachmentsUri, presaInCaricoInfo.getXPagopaExtchCxId(), true)
                                .then(insertRequestFromCartaceo(paperNotificationRequest,
                                                                xPagopaExtchCxId).onErrorResume(throwable -> Mono.error(new EcInternalEndpointHttpException())))

                                .flatMap(requestDto -> sqsService.send(notificationTrackerSqsName.statoCartaceoName(),
                                                                       createNotificationTrackerQueueDtoPaper(cartaceoPresaInCaricoInfo,
                                                                                                              transactionProcessConfigurationProperties.paperStarterStatus(),
                                                                                                              "booked",
                                                                                                              // TODO: SET MISSING
                                                                                                              //  PROPERTIES
                                                                                                              new PaperProgressStatusDto())))
//                              Publish to CARTACEO BATCH
                                .flatMap(sendMessageResponse -> {
                                    PaperEngageRequest req = cartaceoPresaInCaricoInfo.getPaperEngageRequest();
                                    if (req != null) {
                                        return sqsService.send(cartaceoSqsQueueName.batchName(),
                                                               cartaceoPresaInCaricoInfo.getPaperEngageRequest());
                                    } else {
                                        return Mono.empty();
                                    }
                                })
                                .then();
    }

    private ArrayList<String> getPaperUri(List<PaperEngageRequestAttachments> paperEngageRequestAttachments) {
        ArrayList<String> list = new ArrayList<>();
        if (!paperEngageRequestAttachments.isEmpty()) {
            for (PaperEngageRequestAttachments attachment : paperEngageRequestAttachments) {
                list.add(attachment.getUri());
            }
        }
        return list;
    }

    private Mono<RequestDto> insertRequestFromCartaceo(PaperEngageRequest paperNotificationRequest, String xPagopaExtchCxId) {

        return Mono.fromCallable(() -> {
            var requestDto = new RequestDto();
            requestDto.setRequestIdx(paperNotificationRequest.getRequestId());
            requestDto.setClientRequestTimeStamp(paperNotificationRequest.getClientRequestTimeStamp());
            requestDto.setxPagopaExtchCxId(xPagopaExtchCxId);
            var requestPersonalDto = new RequestPersonalDto();
            var digitalRequestPersonalDto = new PaperRequestPersonalDto();

            List<AttachmentsEngageRequestDto> attachmentsEngageRequestDto = new ArrayList<>();
            if (!paperNotificationRequest.getAttachments().isEmpty()) {
                for (PaperEngageRequestAttachments attachment : paperNotificationRequest.getAttachments()) {
                    AttachmentsEngageRequestDto attachments = new AttachmentsEngageRequestDto();
                    attachments.setUri(attachment.getUri());
                    attachments.setOrder(attachment.getOrder());
                    attachments.setDocumentType(attachment.getDocumentType());
                    attachments.setSha256(attachment.getSha256());
                    attachmentsEngageRequestDto.add(attachments);

                }
            }

            digitalRequestPersonalDto.setAttachments(attachmentsEngageRequestDto);

            digitalRequestPersonalDto.setReceiverName(paperNotificationRequest.getReceiverName());
            digitalRequestPersonalDto.setReceiverNameRow2(paperNotificationRequest.getReceiverNameRow2());
            digitalRequestPersonalDto.setReceiverAddress(paperNotificationRequest.getReceiverAddress());
            digitalRequestPersonalDto.setReceiverAddressRow2(paperNotificationRequest.getReceiverAddressRow2());
            digitalRequestPersonalDto.setReceiverCap(paperNotificationRequest.getReceiverCap());
            digitalRequestPersonalDto.setReceiverCity(paperNotificationRequest.getReceiverCity());
            digitalRequestPersonalDto.setReceiverCity2(paperNotificationRequest.getReceiverCity2());
            digitalRequestPersonalDto.setReceiverPr(paperNotificationRequest.getReceiverPr());
            digitalRequestPersonalDto.setReceiverCountry(paperNotificationRequest.getReceiverCountry());
            digitalRequestPersonalDto.setReceiverFiscalCode(paperNotificationRequest.getReceiverFiscalCode());
            digitalRequestPersonalDto.setSenderName(paperNotificationRequest.getSenderName());
            digitalRequestPersonalDto.setSenderAddress(paperNotificationRequest.getSenderAddress());
            digitalRequestPersonalDto.setSenderCity(paperNotificationRequest.getSenderCity());
            digitalRequestPersonalDto.setSenderPr(paperNotificationRequest.getSenderPr());
            digitalRequestPersonalDto.setSenderDigitalAddress(paperNotificationRequest.getSenderDigitalAddress());
            digitalRequestPersonalDto.setArName(paperNotificationRequest.getArName());
            digitalRequestPersonalDto.setArAddress(paperNotificationRequest.getArAddress());
            digitalRequestPersonalDto.setArCap(paperNotificationRequest.getArCap());
            digitalRequestPersonalDto.setArCity(paperNotificationRequest.getArCity());
            requestPersonalDto.setPaperRequestPersonal(digitalRequestPersonalDto);

            var requestMetadataDto = new RequestMetadataDto();
            var digitalRequestMetadataDto = new PaperRequestMetadataDto();
            digitalRequestMetadataDto.setRequestPaId(paperNotificationRequest.getRequestPaId());
            digitalRequestMetadataDto.setIun(paperNotificationRequest.getIun());
            digitalRequestMetadataDto.setVas(paperNotificationRequest.getVas());
            digitalRequestMetadataDto.setPrintType(paperNotificationRequest.getPrintType());
            digitalRequestMetadataDto.setProductType(paperNotificationRequest.getProductType());
            requestMetadataDto.setPaperRequestMetadata(digitalRequestMetadataDto);

            requestDto.setRequestPersonal(requestPersonalDto);
            requestDto.setRequestMetadata(requestMetadataDto);
            return requestDto;
        }).flatMap(gestoreRepositoryCall::insertRichiesta);
    }

    @SqsListener(value = "${sqs.queue.cartaceo.batch-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void lavorazioneRichiesta(final CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo//
            , final Acknowledgment acknowledgment//
    ) {
        log.info("<-- START LAVORAZIONE RICHIESTA CARTACEO -->");
        logIncomingMessage(cartaceoSqsQueueName.batchName(), cartaceoPresaInCaricoInfo);
        var paperEngageRequestSrc = cartaceoPresaInCaricoInfo.getPaperEngageRequest();
        var paperEngageRequestDst = cartaceoMapper.convert(paperEngageRequestSrc);

//        AtomicReference<GeneratedMessageDto> generatedMessageDto = new AtomicReference<>();

        // Try to send PAPER
        paperMessageCall.putRequest(paperEngageRequestDst)

                // The PAPER in sent, publish to Notification Tracker with next status -> SENT
                .flatMap(operationResultCodeResponse -> {
                    return sqsService.send(notificationTrackerSqsName.statoCartaceoName()
                            , createNotificationTrackerQueueDtoPaper(cartaceoPresaInCaricoInfo,
                                    "booked",
                                    "sent",
                                    //TODO object paper
                                    new PaperProgressStatusDto()));
                })

                // Delete from queue
                .doOnSuccess(result -> acknowledgment.acknowledge())

                // An error occurred during PAPER send, start retries
                .retryWhen(DEFAULT_RETRY_STRATEGY)

                // The maximum number of retries has ended
                .onErrorResume(CartaceoSendException.CartaceoMaxRetriesExceededException.class//
                        , cartaceoMaxRetriesExceeded -> cartaceoRetriesExceeded(acknowledgment//
                                , cartaceoPresaInCaricoInfo
                        ))

                // An error occurred during SQS publishing to the Notification Tracker -> Publish to ERRORI PAPER queue and
                // notify to retry update status only
                // TODO: CHANGE THE PAYLOAD
                .onErrorResume(SqsPublishException.class//
                        , sqsPublishException -> sqsService.send(cartaceoSqsQueueName.errorName()//
                                , cartaceoPresaInCaricoInfo//
                        ))//

                .subscribe();
    }

    private Mono<SendMessageResponse> cartaceoRetriesExceeded(final Acknowledgment acknowledgment,
                                                              final CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo) {

        // Publish to Notification Tracker with next status -> RETRY
        return sqsService.send(notificationTrackerSqsName.statoCartaceoName(),
                                createNotificationTrackerQueueDtoPaper(cartaceoPresaInCaricoInfo,
                                        "booked",
                                        "retry",
                                        new PaperProgressStatusDto()))

                // Publish to ERRORI PAPER queue
                .then(sqsService.send(cartaceoSqsQueueName.errorName(), cartaceoPresaInCaricoInfo))

                // Delete from queue
                .doOnSuccess(result -> acknowledgment.acknowledge());
    }

}
