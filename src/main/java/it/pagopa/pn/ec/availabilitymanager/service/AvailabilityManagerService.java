package it.pagopa.pn.ec.availabilitymanager.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.availabilitymanager.model.dto.AvailabilityManagerDetailDto;
import it.pagopa.pn.ec.availabilitymanager.model.dto.AvailabilityManagerDto;
import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.pdfraster.service.DynamoPdfRasterService;
import it.pagopa.pn.ec.rest.v1.dto.AttachmentToConvertDto;
import it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest;
import it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequestAttachments;
import it.pagopa.pn.ec.rest.v1.dto.RequestConversionDto;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.ec.commons.utils.LogUtils.HANDLE_AVAILABILITY_MANAGER;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;

@Service
@CustomLog
public class AvailabilityManagerService {

    private final DynamoPdfRasterService dynamoPdfRasterService;

    private final CartaceoSqsQueueName cartaceoSqsQueueName;

    private final SqsService sqsService;

    public AvailabilityManagerService (DynamoPdfRasterService dynamoPdfRasterService, CartaceoSqsQueueName cartaceoSqsQueueName, SqsService sqsService) {
        this.dynamoPdfRasterService = dynamoPdfRasterService;
        this.cartaceoSqsQueueName = cartaceoSqsQueueName;
        this.sqsService = sqsService;
    }

    @Value("${sqs.queue.availabilitymanager.name}")
    String availabilityManagerQueueName;

    @SqsListener(value = "${sqs.queue.availabilitymanager.name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void lavorazioneEsitiPecInteractive(final AvailabilityManagerDto availabilityManagerDto, Acknowledgment acknowledgment) {
        logIncomingMessage(availabilityManagerQueueName, availabilityManagerDto);
        handleAvailabilityManager(availabilityManagerDto, acknowledgment).subscribe();
    }

    Mono<Void> handleAvailabilityManager(AvailabilityManagerDto availabilityManagerDto, Acknowledgment acknowledgment) {
        log.logStartingProcess(HANDLE_AVAILABILITY_MANAGER);
        return Mono.justOrEmpty(availabilityManagerDto).flatMap(dto -> {
                    AvailabilityManagerDetailDto detailDto = dto.getDetail();
                    String newFilekey = detailDto.getKey();
                    String sha256 = detailDto.getChecksum();

                    return dynamoPdfRasterService.updateRequestConversion(newFilekey, true, sha256);
                }).filter(this::allAttachmentsConverted)
                .map(requestConversionDto -> {
                    List<AttachmentToConvertDto> attachments = requestConversionDto.getAttachments();
                    PaperEngageRequest originalRequest = requestConversionDto.getOriginalRequest();
                    List<PaperEngageRequestAttachments> originalRequestAttachments = originalRequest.getAttachments();

                    for (PaperEngageRequestAttachments paperAttachment : originalRequestAttachments) {
                        String oldFileKey = paperAttachment.getUri().replace("safestorage://", "");
                        String newFileKey = "";
                        String checkSum = "";
                        for (AttachmentToConvertDto toConvert : attachments) {
                            if (oldFileKey.equals(toConvert.getOriginalFileKey())) {
                                newFileKey = toConvert.getNewFileKey();
                                checkSum = toConvert.getSha256();
                            }
                        }
                        paperAttachment.setUri("safestorage://" + newFileKey);
                        paperAttachment.setSha256(checkSum);
                    }

                    originalRequest.setAttachments(originalRequestAttachments);

                    CartaceoPresaInCaricoInfo info = new CartaceoPresaInCaricoInfo();
                    info.setPaperEngageRequest(originalRequest);
                    info.setRequestIdx(requestConversionDto.getRequestId());
                    info.setXPagopaExtchCxId(requestConversionDto.getxPagopaExtchCxId());

                    return info;
                }).flatMap(cartaceoPresaInCaricoInfo -> sqsService.send(cartaceoSqsQueueName.batchName(), cartaceoPresaInCaricoInfo))
                .doOnSuccess(throwable -> log.logEndingProcess(HANDLE_AVAILABILITY_MANAGER))
                .doOnError(throwable -> log.logEndingProcess(HANDLE_AVAILABILITY_MANAGER, false, throwable.getMessage()))
                .then();

    }


    private boolean allAttachmentsConverted(RequestConversionDto dto) {
        return dto.getAttachments().stream().allMatch(AttachmentToConvertDto::getConverted);
    }

}
