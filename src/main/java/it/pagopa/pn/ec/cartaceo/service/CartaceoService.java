package it.pagopa.pn.ec.cartaceo.service;

import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoPaper;

@Service
@Slf4j
public class CartaceoService extends PresaInCaricoService {


    private final SqsService sqsService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final AttachmentServiceImpl attachmentService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final CartaceoSqsQueueName cartaceoSqsQueueName;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;


    protected CartaceoService(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService,
                              GestoreRepositoryCall gestoreRepositoryCall1, AttachmentServiceImpl attachmentService,
                              NotificationTrackerSqsName notificationTrackerSqsName, CartaceoSqsQueueName cartaceoSqsQueueName,
                              TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
        this.gestoreRepositoryCall = gestoreRepositoryCall1;
        this.attachmentService = attachmentService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.cartaceoSqsQueueName = cartaceoSqsQueueName;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(PresaInCaricoInfo presaInCaricoInfo) {
        var cartaceoPresaInCaricoInfo = (CartaceoPresaInCaricoInfo) presaInCaricoInfo;

        var paperEngageRequestAttachments = cartaceoPresaInCaricoInfo.getPaperEngageRequest().getAttachments();

        return attachmentService.getAllegatiPresignedUrlOrMetadata(paperEngageRequestAttachments.stream()
                                                                                                .map(PaperEngageRequestAttachments::getUri)
                                                                                                .toList(),
                                                                   presaInCaricoInfo.getXPagopaExtchCxId(),
                                                                   true)
                                .flatMap(fileDownloadResponse -> {
                                    log.info("fileDownloadResponse" + fileDownloadResponse);
                                    var paperEngageRequest = cartaceoPresaInCaricoInfo.getPaperEngageRequest();
                                    paperEngageRequest.setRequestId(presaInCaricoInfo.getRequestIdx());
                                    return insertRequestFromCartaceo(paperEngageRequest).onErrorResume(throwable -> Mono.error(new EcInternalEndpointHttpException()));
                                })
                                .flatMap(requestDto -> sqsService.send(notificationTrackerSqsName.statoCartaceoName(),
                                                                       createNotificationTrackerQueueDtoPaper(cartaceoPresaInCaricoInfo,
                                                                                                              transactionProcessConfigurationProperties.paperStarterStatus(),
                                                                                                              "booked",
                                                                                                              // TODO: SET MISSING
                                                                                                              //  PROPERTIES
                                                                                                              new PaperProgressStatusDto())))
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

    private Mono<RequestDto> insertRequestFromCartaceo(PaperEngageRequest paperEngageRequest) {

        return Mono.fromCallable(() -> {
            var requestDto = new RequestDto();
            requestDto.setRequestIdx(paperEngageRequest.getRequestId());
            requestDto.setClientRequestTimeStamp(paperEngageRequest.getClientRequestTimeStamp());
            var requestPersonalDto = new RequestPersonalDto();
            var digitalRequestPersonalDto = new PaperRequestPersonalDto();

            List<AttachmentsEngageRequestDto> engageRequestDto = new ArrayList<>();
            if (!paperEngageRequest.getAttachments().isEmpty()) {
                for (PaperEngageRequestAttachments attachment : paperEngageRequest.getAttachments()) {
                    AttachmentsEngageRequestDto attachments = new AttachmentsEngageRequestDto();
                    attachments.setUri(attachment.getUri());
                    attachments.setOrder(attachment.getOrder());
                    attachments.setDocumentType(attachment.getDocumentType());
                    attachments.setSha256(attachment.getSha256());
                    engageRequestDto.add(attachments);

                }
            }

            digitalRequestPersonalDto.setAttachments(engageRequestDto);

            digitalRequestPersonalDto.setReceiverName(paperEngageRequest.getReceiverName());
            digitalRequestPersonalDto.setReceiverNameRow2(paperEngageRequest.getReceiverNameRow2());
            digitalRequestPersonalDto.setReceiverAddress(paperEngageRequest.getReceiverAddress());
            digitalRequestPersonalDto.setReceiverAddressRow2(paperEngageRequest.getReceiverAddressRow2());
            digitalRequestPersonalDto.setReceiverCap(paperEngageRequest.getReceiverCap());
            digitalRequestPersonalDto.setReceiverCity(paperEngageRequest.getReceiverCity());
            digitalRequestPersonalDto.setReceiverCity2(paperEngageRequest.getReceiverCity2());
            digitalRequestPersonalDto.setReceiverPr(paperEngageRequest.getReceiverPr());
            digitalRequestPersonalDto.setReceiverCountry(paperEngageRequest.getReceiverCountry());
            digitalRequestPersonalDto.setReceiverFiscalCode(paperEngageRequest.getReceiverFiscalCode());
            digitalRequestPersonalDto.setSenderName(paperEngageRequest.getSenderName());
            digitalRequestPersonalDto.setSenderAddress(paperEngageRequest.getSenderAddress());
            digitalRequestPersonalDto.setSenderCity(paperEngageRequest.getSenderCity());
            digitalRequestPersonalDto.setSenderPr(paperEngageRequest.getSenderPr());
            digitalRequestPersonalDto.setSenderDigitalAddress(paperEngageRequest.getSenderDigitalAddress());
            digitalRequestPersonalDto.setArName(paperEngageRequest.getArName());
            digitalRequestPersonalDto.setArAddress(paperEngageRequest.getArAddress());
            digitalRequestPersonalDto.setArCap(paperEngageRequest.getArCap());
            digitalRequestPersonalDto.setArCity(paperEngageRequest.getArCity());
            requestPersonalDto.setPaperRequestPersonal(digitalRequestPersonalDto);

            var requestMetadataDto = new RequestMetadataDto();
            var digitalRequestMetadataDto = new PaperRequestMetadataDto();
            digitalRequestMetadataDto.setRequestPaId(paperEngageRequest.getRequestPaId());
            digitalRequestMetadataDto.setIun(paperEngageRequest.getIun());
            digitalRequestMetadataDto.setVas(paperEngageRequest.getVas());
            digitalRequestMetadataDto.setPrintType(paperEngageRequest.getPrintType());
            digitalRequestMetadataDto.setProductType(paperEngageRequest.getProductType());
            requestMetadataDto.setPaperRequestMetadata(digitalRequestMetadataDto);

            requestDto.setRequestPersonal(requestPersonalDto);
            requestDto.setRequestMetadata(requestMetadataDto);
            return requestDto;
        }).flatMap(gestoreRepositoryCall::insertRichiesta);
    }

}
