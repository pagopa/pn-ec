package it.pagopa.pn.ec.availabilitymanager.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.availabilitymanager.model.dto.AvailabilityManagerDetailDto;
import it.pagopa.pn.ec.availabilitymanager.model.dto.AvailabilityManagerDto;
import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.service.CartaceoService;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.pdfraster.configuration.PdfRasterProperties;
import it.pagopa.pn.ec.pdfraster.service.RequestConversionService;
import it.pagopa.pn.ec.rest.v1.dto.AttachmentToConvertDto;
import it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest;
import it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequestAttachments;
import it.pagopa.pn.ec.rest.v1.dto.RequestConversionDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@CustomLog
class AvailabilityManagerServiceTest {

    @Value("${sqs.queue.availabilitymanager.name}")
    String availabilityManagerQueueName;

    @MockBean
    AvailabilityManagerService availabilityManagerService;
    @SpyBean
    SqsService sqsService;
    @Autowired
    CartaceoSqsQueueName cartaceoSqsQueueName;
    @SpyBean
    RequestConversionService requestConversionService;
    @MockBean
    private Acknowledgment acknowledgment;
    @SpyBean
    private CallMacchinaStati callMachinaStati;
    @SpyBean
    private CartaceoService cartaceoService;
    @MockBean
    private TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;
    @MockBean
    private PdfRasterProperties pdfRasterProperties;
    
    
    

    private AvailabilityManagerDto createAvailabilityManagerDto(String newFileKey, String checksum) {
        AvailabilityManagerDto availabilityManagerDto = new AvailabilityManagerDto();
        AvailabilityManagerDetailDto detailDto = new AvailabilityManagerDetailDto();
        detailDto.setKey(newFileKey);
        detailDto.setChecksum(checksum);


        availabilityManagerDto.setDetail(detailDto);

        return availabilityManagerDto;
    }

    private AvailabilityManagerDto createAvailabilityManagerDtoError(String newFileKey, String checksum) {
        AvailabilityManagerDto availabilityManagerDto = new AvailabilityManagerDto();
        AvailabilityManagerDetailDto detailDto = new AvailabilityManagerDetailDto();
        detailDto.setKey(newFileKey);
        detailDto.setChecksum(checksum);
        detailDto.setDocumentStatus("ERROR");
        availabilityManagerDto.setSource("GESTORE DISPONIBILITA");
        availabilityManagerDto.setDetailType("SafeStorageTransformEvent");
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

    @BeforeEach
    void setUp() {
        availabilityManagerService =
                Mockito.spy(new AvailabilityManagerService(requestConversionService,cartaceoSqsQueueName, sqsService,callMachinaStati, cartaceoService, transactionProcessConfigurationProperties /*, ... */));
    }


    @Test
    void convertPDFNotAllConvertedOk() {
        Mockito.when(pdfRasterProperties.pdfConversionExpirationOffsetInDays()).thenReturn(1);

        // GIVEN: oggetti necessari al test (Dto)
        String originalFileKey1 = "originalFileKey1";
        String newFileKey1 = "newFileKey1";
        String sha256 = "7645373453";
        AvailabilityManagerDto dto = createAvailabilityManagerDto(newFileKey1, sha256);
        AttachmentToConvertDto attachmentToConvertDto1 = new AttachmentToConvertDto().originalFileKey(originalFileKey1).newFileKey(newFileKey1).sha256(sha256);
        AttachmentToConvertDto attachmentToConvertDto2 = new AttachmentToConvertDto().originalFileKey("originale").newFileKey("nuova").sha256(sha256);
        RequestConversionDto requestConversionDto= createRequestConversionDto(List.of(attachmentToConvertDto1, attachmentToConvertDto2), "requestId1");
        requestConversionService.insertRequestConversion(requestConversionDto).block();

        // THEN: test del metodo, assertions e verify
        StepVerifier.create(availabilityManagerService.handleAvailabilityManager(dto, acknowledgment)).verifyComplete();
        verify(sqsService, never()).send(eq(cartaceoSqsQueueName.batchName()), any());
        verify(requestConversionService, times(1)).updateRequestConversion(newFileKey1, true, sha256,false);
    }

    @Test
    void convertPDFAllConvertedOk() {
        Mockito.when(pdfRasterProperties.pdfConversionExpirationOffsetInDays()).thenReturn(1);

        // GIVEN: oggetti necessari al test (Dto)
        String originalFileKey2 = "originalFileKey2";
        String newFileKey2 = "newFileKey2";
        String sha256 = "76453734531";
        AvailabilityManagerDto dto = createAvailabilityManagerDto(newFileKey2, sha256);
        AttachmentToConvertDto attachmentToConvertDto1 = new AttachmentToConvertDto().originalFileKey(originalFileKey2).newFileKey(newFileKey2).sha256(sha256);
        RequestConversionDto requestConversionDto= createRequestConversionDto(List.of(attachmentToConvertDto1), "requestId2");
        requestConversionService.insertRequestConversion(requestConversionDto).block();

        // THEN: test del metodo, assertions e verify
        StepVerifier.create(availabilityManagerService.handleAvailabilityManager(dto, acknowledgment)).verifyComplete();
        verify(sqsService, times(1)).send(eq(cartaceoSqsQueueName.batchName()), any());
        verify(requestConversionService, times(1)).updateRequestConversion(newFileKey2, true, sha256,false);
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
        verify(requestConversionService, times(1)).updateRequestConversion(newFileKey3, true, sha256,false);
    }

    @Test
    void convertSafeStorageError() {
        // GIVEN
        String newFileKey1 = "newFileKey1";
        String sha256 = "7645373453";
        AvailabilityManagerDto dto = createAvailabilityManagerDtoError(newFileKey1, sha256);

        // "isSafeStorageError == true" -> evento indisponibilità
        when(availabilityManagerService.isSafeStorageError(dto)).thenReturn(true);

        RequestConversionDto reqConvDto = new RequestConversionDto();
        Map.Entry<RequestConversionDto, Boolean> entry = new AbstractMap.SimpleEntry<>(reqConvDto, true);
        when(requestConversionService.updateRequestConversion(dto.getDetail().getKey(), true, dto.getDetail().getChecksum(),true))
                .thenReturn(Mono.just(entry));

        doReturn(Mono.empty()).when(availabilityManagerService)
                .handleTransformationError(eq(reqConvDto), eq(dto.getDetail()), eq(acknowledgment));

        StepVerifier.create(availabilityManagerService.handleAvailabilityManager(dto, acknowledgment))
                .verifyComplete();

        verify(requestConversionService).updateRequestConversion(dto.getDetail().getKey(), true, dto.getDetail().getChecksum(),true);
        verify(availabilityManagerService).handleTransformationError(eq(reqConvDto), eq(dto.getDetail()), eq(acknowledgment));
        verifyNoInteractions(sqsService); // nessun invio SQS in questo ramo, si invia al NT
    }

}
