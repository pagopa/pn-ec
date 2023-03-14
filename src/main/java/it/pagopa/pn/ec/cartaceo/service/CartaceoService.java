package it.pagopa.pn.ec.cartaceo.service;

import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.mapper.CartaceoMapper;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.cartaceo.CartaceoSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall.DEFAULT_RETRY_STRATEGY;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static java.time.OffsetDateTime.now;

@Service
@Slf4j
public class CartaceoService  extends PresaInCaricoService {

    private final SqsService sqsService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final AttachmentServiceImpl attachmentService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final CartaceoSqsQueueName cartaceoSqsQueueName;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;
    private final PaperMessageCall paperMessageCall;
    private final CartaceoMapper cartaceoMapper;

    protected CartaceoService(AuthService authService//
            , GestoreRepositoryCall gestoreRepositoryCall//
            , SqsService sqsService//
            , GestoreRepositoryCall gestoreRepositoryCall1//
            , AttachmentServiceImpl attachmentService//
            , NotificationTrackerSqsName notificationTrackerSqsName//
            , CartaceoSqsQueueName cartaceoSqsQueueName//
            , TransactionProcessConfigurationProperties transactionProcessConfigurationProperties//
            , PaperMessageCall paperMessageCall//
            , CartaceoMapper cartaceoMapper//
            ) {
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
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = (CartaceoPresaInCaricoInfo)presaInCaricoInfo;

        List<String> attachmentsUri = getPaperUri(cartaceoPresaInCaricoInfo.getPaperEngageRequest().getAttachments());
        var peperNotificationRequest = cartaceoPresaInCaricoInfo.getPaperEngageRequest();
        var xPagopaExtchCxId = presaInCaricoInfo.getXPagopaExtchCxId();
        return attachmentService.getAllegatiPresignedUrlOrMetadata(attachmentsUri,
                                                                   presaInCaricoInfo.getXPagopaExtchCxId(),
                                                                   true)
                .then(insertRequestFromCartaceo(peperNotificationRequest,
                xPagopaExtchCxId).onErrorResume(throwable -> Mono.error(new EcInternalEndpointHttpException())))

                .flatMap(requestDto -> sqsService.send(notificationTrackerSqsName.statoCartaceoName(),
                        new NotificationTrackerQueueDto(presaInCaricoInfo.getRequestIdx(),
                                presaInCaricoInfo.getXPagopaExtchCxId(),
                                now(),
                                transactionProcessConfigurationProperties.paper(),
                                transactionProcessConfigurationProperties.paperStarterStatus(),
                                "booked",
                                null)))
                .flatMap(sendMessageResponse -> {
                    PaperEngageRequest req = cartaceoPresaInCaricoInfo.getPaperEngageRequest();
                    if (req !=null) {
                        return sqsService.send(cartaceoSqsQueueName.batchName(),
                                cartaceoPresaInCaricoInfo.getPaperEngageRequest());
                    } else {
                        return Mono.empty();
                    }
                        }
                )
                .then();
    }

    private ArrayList<String> getPaperUri(List<PaperEngageRequestAttachments> paperEngageRequestAttachments) {
        ArrayList<String> list = new ArrayList<>();
        if(!paperEngageRequestAttachments.isEmpty() ){
            for ( PaperEngageRequestAttachments attachment : paperEngageRequestAttachments ){
                list.add(attachment.getUri());
            }
        }
        return list;
    }

    private Mono<RequestDto> insertRequestFromCartaceo(PaperEngageRequest peperNotificationRequest, String xPagopaExtchCxId) {

        return Mono.fromCallable(() ->{
            var requestDto = new RequestDto();
            requestDto.setRequestIdx(peperNotificationRequest.getRequestId());
            requestDto.setClientRequestTimeStamp(peperNotificationRequest.getClientRequestTimeStamp());
            requestDto.setxPagopaExtchCxId(xPagopaExtchCxId);
            var requestPersonalDto = new RequestPersonalDto();
            var digitalRequestPersonalDto = new PaperRequestPersonalDto();

            List<AttachmentsEngageRequestDto> attachmentsEngageRequestDtos = new ArrayList<>();
            if(!peperNotificationRequest.getAttachments().isEmpty() ){
                for ( PaperEngageRequestAttachments attachment : peperNotificationRequest.getAttachments() ){
                    AttachmentsEngageRequestDto attachments =  new AttachmentsEngageRequestDto();
                    attachments.setUri(attachment.getUri());
                    attachments.setOrder(attachment.getOrder());
                    attachments.setDocumentType(attachment.getDocumentType());
                    attachments.setSha256(attachment.getSha256());
                    attachmentsEngageRequestDtos.add(attachments);
                }
            }

            digitalRequestPersonalDto.setAttachments(attachmentsEngageRequestDtos);

            digitalRequestPersonalDto.setReceiverName(peperNotificationRequest.getReceiverName());
            digitalRequestPersonalDto.setReceiverNameRow2(peperNotificationRequest.getReceiverNameRow2());
            digitalRequestPersonalDto.setReceiverAddress(peperNotificationRequest.getReceiverAddress());
            digitalRequestPersonalDto.setReceiverAddressRow2(peperNotificationRequest.getReceiverAddressRow2());
            digitalRequestPersonalDto.setReceiverCap(peperNotificationRequest.getReceiverCap());
            digitalRequestPersonalDto.setReceiverCity(peperNotificationRequest.getReceiverCity());
            digitalRequestPersonalDto.setReceiverCity2(peperNotificationRequest.getReceiverCity2());
            digitalRequestPersonalDto.setReceiverPr(peperNotificationRequest.getReceiverPr());
            digitalRequestPersonalDto.setReceiverCountry(peperNotificationRequest.getReceiverCountry());
            digitalRequestPersonalDto.setReceiverFiscalCode(peperNotificationRequest.getReceiverFiscalCode());
            digitalRequestPersonalDto.setSenderName(peperNotificationRequest.getSenderName());
            digitalRequestPersonalDto.setSenderAddress(peperNotificationRequest.getSenderAddress());
            digitalRequestPersonalDto.setSenderCity(peperNotificationRequest.getSenderCity());
            digitalRequestPersonalDto.setSenderPr(peperNotificationRequest.getSenderPr());
            digitalRequestPersonalDto.setSenderDigitalAddress(peperNotificationRequest.getSenderDigitalAddress());
            digitalRequestPersonalDto.setArName(peperNotificationRequest.getArName());
            digitalRequestPersonalDto.setArAddress(peperNotificationRequest.getArAddress());
            digitalRequestPersonalDto.setArCap(peperNotificationRequest.getArCap());
            digitalRequestPersonalDto.setArCity(peperNotificationRequest.getArCity());
            requestPersonalDto.setPaperRequestPersonal(digitalRequestPersonalDto);

            var requestMetadataDto = new RequestMetadataDto();
            var digitalRequestMetadataDto = new PaperRequestMetadataDto();
            digitalRequestMetadataDto.setRequestPaId(peperNotificationRequest.getRequestPaId());
            digitalRequestMetadataDto.setIun(peperNotificationRequest.getIun());
            digitalRequestMetadataDto.setVas(peperNotificationRequest.getVas());
            digitalRequestMetadataDto.setPrintType(peperNotificationRequest.getPrintType());
            digitalRequestMetadataDto.setProductType(peperNotificationRequest.getProductType());
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
        var requestId = cartaceoPresaInCaricoInfo.getRequestIdx();
        var clientId = cartaceoPresaInCaricoInfo.getXPagopaExtchCxId();
        var paperEngageRequestSrc = cartaceoPresaInCaricoInfo.getPaperEngageRequest();
        var paperEngageRequestDst = cartaceoMapper.convert(paperEngageRequestSrc);

        AtomicReference<GeneratedMessageDto> generatedMessageDto = new AtomicReference<>();

        // Try to send PAPER
        paperMessageCall.putRequest(paperEngageRequestDst)

                // The PAPER in sent, publish to Notification Tracker with next status -> SENT
                .flatMap(operationResultCodeResponse -> {
                    generatedMessageDto.set(new GeneratedMessageDto().id(operationResultCodeResponse.getResultCode()).system("systemPlaceholder"));
                    return sqsService.send(notificationTrackerSqsName.statoCartaceoName()//
                            , new NotificationTrackerQueueDto(requestId//
                                    , clientId//
                                    , now()//
                                    , transactionProcessConfigurationProperties.paper()//
                                    , cartaceoPresaInCaricoInfo.getStatusAfterStart()//
                                    , "sent"//
                    // TODO: SET eventDetails
                                    , ""//
                                    , generatedMessageDto.get()//
                    ));
                })

                // Delete from queue
                .doOnSuccess(result -> acknowledgment.acknowledge())

                // An error occurred during PAPER send, start retries
                .retryWhen(DEFAULT_RETRY_STRATEGY)

                // The maximum number of retries has ended
                .onErrorResume(CartaceoSendException.CartaceoMaxRetriesExceededException.class//
                        , cartaceoMaxRetriesExceeded -> cartaceoRetriesExceeded(acknowledgment//
                                , cartaceoPresaInCaricoInfo//
                                , cartaceoPresaInCaricoInfo.getStatusAfterStart()//
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

    private Mono<SendMessageResponse> cartaceoRetriesExceeded(final Acknowledgment acknowledgment//
            , final CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo//
            , String currentStatus//
    ) {

        var requestId = cartaceoPresaInCaricoInfo.getRequestIdx();
        var clientId = cartaceoPresaInCaricoInfo.getXPagopaExtchCxId();

        // Publish to Notification Tracker with next status -> RETRY
        return sqsService.send(notificationTrackerSqsName.statoCartaceoName()//
                , new NotificationTrackerQueueDto(requestId//
                        , clientId//
                        , now()//
                        , transactionProcessConfigurationProperties.paper()//
                        , currentStatus//
                        , "retry"//
                        // TODO: SET eventDetails
                        , ""//
                        , null//
                ))

                // Publish to ERRORI PAPER queue
                .then(sqsService.send(cartaceoSqsQueueName.errorName(), cartaceoPresaInCaricoInfo))

                // Delete from queue
                .doOnSuccess(result -> acknowledgment.acknowledge());
    }
    
}
