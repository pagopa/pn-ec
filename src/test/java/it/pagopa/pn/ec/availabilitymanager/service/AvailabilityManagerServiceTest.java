package it.pagopa.pn.ec.availabilitymanager.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.availabilitymanager.model.dto.AvailabilityManagerDetailDto;
import it.pagopa.pn.ec.availabilitymanager.model.dto.AvailabilityManagerDto;
import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.pdfraster.service.DynamoPdfRasterService;
import it.pagopa.pn.ec.rest.v1.dto.AttachmentToConvertDto;
import it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest;
import it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequestAttachments;
import it.pagopa.pn.ec.rest.v1.dto.RequestConversionDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@CustomLog
class AvailabilityManagerServiceTest {

    @Value("${sqs.queue.availabilitymanager.name}")
    String availabilityManagerQueueName;

    @Autowired
    AvailabilityManagerService availabilityManagerService;
    @SpyBean
    SqsService sqsService;
    @Autowired
    CartaceoSqsQueueName cartaceoSqsQueueName;
    @SpyBean
    DynamoPdfRasterService dynamoPdfRasterService;
    @MockBean
    private Acknowledgment acknowledgment;

    private AvailabilityManagerDto createAvailabilityManagerDto(String newFileKey, String checksum) {
        AvailabilityManagerDto availabilityManagerDto = new AvailabilityManagerDto();
        AvailabilityManagerDetailDto detailDto = new AvailabilityManagerDetailDto();
        detailDto.setKey(newFileKey);
        detailDto.setChecksum(checksum);

        availabilityManagerDto.setDetail(detailDto);

        return availabilityManagerDto;
    }

    private RequestConversionDto createRequestConversionDto(List<AttachmentToConvertDto> list, String requestId) {
        RequestConversionDto requestConversionDto = new RequestConversionDto();
        requestConversionDto.setxPagopaExtchCxId("clientId");
        PaperEngageRequest paperEngageRequest = new PaperEngageRequest();
        list.forEach( attachment -> {
            PaperEngageRequestAttachments paperEngageRequestAttachments = new PaperEngageRequestAttachments().sha256("7645373453").uri("safestorage://"+attachment.getOriginalFileKey());
            paperEngageRequest.addAttachmentsItem(paperEngageRequestAttachments);

            requestConversionDto.addAttachmentsItem(attachment);
        });

        requestConversionDto.setRequestId(requestId);
        requestConversionDto.setOriginalRequest(paperEngageRequest);

        return requestConversionDto;
    }

    @Test
    void convertPDFNotAllConvertedOk() {

        // GIVEN: oggetti necessari al test (Dto)
        String originalFileKey1 = "originalFileKey1";
        String newFileKey1 = "newFileKey1";
        String sha256 = "7645373453";
        AvailabilityManagerDto dto = createAvailabilityManagerDto(newFileKey1, sha256);
        AttachmentToConvertDto attachmentToConvertDto1 = new AttachmentToConvertDto().originalFileKey(originalFileKey1).newFileKey(newFileKey1).sha256(sha256);
        AttachmentToConvertDto attachmentToConvertDto2 = new AttachmentToConvertDto().originalFileKey("originale").newFileKey("nuova").sha256(sha256);
        RequestConversionDto requestConversionDto= createRequestConversionDto(List.of(attachmentToConvertDto1, attachmentToConvertDto2), "requestId1");
        dynamoPdfRasterService.insertRequestConversion(requestConversionDto).block();

        // THEN: test del metodo, assertions e verify
        StepVerifier.create(availabilityManagerService.handleAvailabilityManager(dto, acknowledgment)).verifyComplete();
        verify(sqsService, never()).send(eq(cartaceoSqsQueueName.batchName()), any());
        verify(dynamoPdfRasterService , times(1)).updateRequestConversion(newFileKey1, true, sha256);
    }

    @Test
    void convertPDFAllConvertedOk() {

        // GIVEN: oggetti necessari al test (Dto)
        String originalFileKey2 = "originalFileKey2";
        String newFileKey2 = "newFileKey2";
        String sha256 = "76453734531";
        AvailabilityManagerDto dto = createAvailabilityManagerDto(newFileKey2, sha256);
        AttachmentToConvertDto attachmentToConvertDto1 = new AttachmentToConvertDto().originalFileKey(originalFileKey2).newFileKey(newFileKey2).sha256(sha256);
        RequestConversionDto requestConversionDto= createRequestConversionDto(List.of(attachmentToConvertDto1), "requestId2");
        dynamoPdfRasterService.insertRequestConversion(requestConversionDto).block();

        // THEN: test del metodo, assertions e verify
        StepVerifier.create(availabilityManagerService.handleAvailabilityManager(dto, acknowledgment)).verifyComplete();
        verify(sqsService, times(1)).send(eq(cartaceoSqsQueueName.batchName()), any());
        verify(dynamoPdfRasterService , times(1)).updateRequestConversion(newFileKey2, true, sha256);
    }

    @Test
    void convertPDFNotFoundKo() {

        // GIVEN: oggetti necessari al test (Dto)
        String newFileKey3 = "newFileKey3";
        String sha256 = "76453734532";
        AvailabilityManagerDto dto = createAvailabilityManagerDto(newFileKey3, sha256);

        // THEN: test del metodo, assertions e verify
        StepVerifier.create(availabilityManagerService.handleAvailabilityManager(dto, acknowledgment)).verifyError(RepositoryManagerException.PdfConversionNotFoundException.class);
        verify(sqsService, never()).send(eq(cartaceoSqsQueueName.batchName()), any());
        verify(dynamoPdfRasterService , times(1)).updateRequestConversion(newFileKey3, true, sha256);
    }

}
