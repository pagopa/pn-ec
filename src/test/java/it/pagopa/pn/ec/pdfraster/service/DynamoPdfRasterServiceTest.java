package it.pagopa.pn.ec.pdfraster.service;

import com.github.dockerjava.api.exception.ConflictException;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic500ErrorException;
import it.pagopa.pn.ec.pdfraster.model.dto.PdfConversionDto;
import it.pagopa.pn.ec.pdfraster.model.dto.RequestConversionDto;
import it.pagopa.pn.ec.pdfraster.model.entity.PdfConversionEntity;
import it.pagopa.pn.ec.pdfraster.model.entity.RequestConversionEntity;
import it.pagopa.pn.ec.pdfraster.model.entity.AttachmentToConvert;

import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Collections;


@SpringBootTestWebEnv
@CustomLog
class DynamoPdfRasterServiceTest {
    @Autowired
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Autowired
    private RepositoryManagerDynamoTableName repositoryManagerDynamoTableName;

    private static DynamoDbTable<PdfConversionEntity> pdfConversionEntityDynamoDbTable;
    private static DynamoDbTable<RequestConversionEntity> requestConversionEntityDynamoDbTable;

    @SpyBean
    DynamoPdfRasterService dynamoPdfRasterService;

    @BeforeEach
    public void initializeTables() {
        pdfConversionEntityDynamoDbTable = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.richiesteConversionePdfName(),
                TableSchema.fromBean(PdfConversionEntity.class));
        requestConversionEntityDynamoDbTable = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.richiesteConversioneRequestName(),
                TableSchema.fromBean(RequestConversionEntity.class));
    }


    private static RequestConversionDto MockRequestConversionDto() {
        return RequestConversionDto.builder().requestId("123456789")
                .attachments(Collections.singletonList(
                        AttachmentToConvert.builder().newFileKey("12345").build()
                )).build();
    }

    private static RequestConversionDto MockRequestConversionDtoUpdate() {
        return RequestConversionDto.builder().requestId("987654333")
                .attachments(Collections.singletonList(
                        AttachmentToConvert.builder().newFileKey("33333").build()
                )).build();
    }


    private static PdfConversionDto MockPdfConversionDto() {
        return PdfConversionDto.builder().requestId("123456789")
                .fileKey("123456789").build();
    }

    @Test
    void insertRequestConversionOk() {

        RequestConversionDto requestConversionDto = MockRequestConversionDto();

        Mono<RequestConversionDto> response = dynamoPdfRasterService.insertRequestConversion(requestConversionDto);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();
        //LOOKUP
        RequestConversionEntity responseTable = requestConversionEntityDynamoDbTable.getItem(builder -> builder.key(Key.builder().partitionValue(requestConversionDto.getRequestId()).build()));
        Assertions.assertNotNull(responseTable);

        for (AttachmentToConvert keyValue : requestConversionDto.getAttachments()) {
            PdfConversionEntity result = pdfConversionEntityDynamoDbTable.getItem(builder -> builder.key(Key.builder().partitionValue(keyValue.getNewFileKey()).build()));
            Assertions.assertNotNull(result);
            Assertions.assertEquals(requestConversionDto.getRequestId(), result.getRequestId());
        }
    }

    @Test
    void updateRequestConvertionOk() {

        RequestConversionDto requestConversionDto = MockRequestConversionDto();
        RequestConversionDto requestConversionDtoUpdate = MockRequestConversionDtoUpdate();

        Mono<RequestConversionDto> response = dynamoPdfRasterService.insertRequestConversion(requestConversionDto);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        requestConversionDtoUpdate.setRequestId("test");
        Mono<RequestConversionDto> responseUpdate = dynamoPdfRasterService.updateRequestConversion(requestConversionDtoUpdate);

        StepVerifier.create(responseUpdate)
                .expectNextMatches(updatedDto -> updatedDto.getRequestId().equals("test"))
                .verifyComplete();

        PdfConversionEntity updatedResponseTable = pdfConversionEntityDynamoDbTable.getItem(
                builder -> builder.key(Key.builder().partitionValue("test").build())
        );
        Assertions.assertNotNull(updatedResponseTable);
        Assertions.assertEquals("test", updatedResponseTable.getRequestId());
    }


    @Test
    void insertPdfConversionOk() {

        PdfConversionDto pdfConversionDto = MockPdfConversionDto();

        Mono<PdfConversionDto> response = dynamoPdfRasterService.insertPdfConversion(pdfConversionDto);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        RequestConversionEntity responseTable = requestConversionEntityDynamoDbTable.getItem(builder -> builder.key(Key.builder().partitionValue(pdfConversionDto.getRequestId()).build()));
        Assertions.assertNotNull(responseTable);

    }


    @Test
    void insertRequestConversionKo() {
        RequestConversionDto requestConversionDto = MockRequestConversionDto();

        Mono<RequestConversionDto> firstResponse = dynamoPdfRasterService.insertRequestConversion(requestConversionDto);
        StepVerifier.create(firstResponse).expectNextCount(1).verifyComplete();

        // Errore 500
        Mockito.when(dynamoPdfRasterService.insertRequestConversion(requestConversionDto))
                .thenReturn(Mono.error(new Generic500ErrorException("Internal Server Error", "")));

        Mono<RequestConversionDto> response = dynamoPdfRasterService.insertRequestConversion(requestConversionDto);
        StepVerifier.create(response)
                .expectErrorMatches(error -> error instanceof Generic500ErrorException && "Internal Server Error".equals(error.getMessage()))
                .verify();

        // Errore di conflitto
        Mono<RequestConversionDto> responseConflict = dynamoPdfRasterService.insertRequestConversion(requestConversionDto);
        StepVerifier.create(responseConflict).expectError(ConflictException.class).verify();

        Mockito.when(dynamoPdfRasterService.insertRequestConversion(requestConversionDto))
                .thenReturn(Mono.error(new ConflictException("Elemento già presente")));

        responseConflict = dynamoPdfRasterService.insertRequestConversion(requestConversionDto);
        StepVerifier.create(responseConflict)
                .expectErrorMatches(error -> error instanceof ConflictException && "Elemento già presente".equals(error.getMessage()))
                .verify();
        Assertions.fail();
    }

    @Test
    void insertPdfConversionKo() {
        PdfConversionDto pdfConversionDto = MockPdfConversionDto();

        Mono<PdfConversionDto> firstResponse = dynamoPdfRasterService.insertPdfConversion(pdfConversionDto);
        StepVerifier.create(firstResponse).expectNextCount(1).verifyComplete();

        // Errore 500
        Mockito.when(dynamoPdfRasterService.insertPdfConversion(pdfConversionDto))
                .thenReturn(Mono.error(new Generic500ErrorException("", "")));

        Mono<PdfConversionDto> response = dynamoPdfRasterService.insertPdfConversion(pdfConversionDto);
        StepVerifier.create(response)
                .expectErrorMatches(error -> error instanceof Generic500ErrorException)
                .verify();

        // Errore di conflitto
        Mono<PdfConversionDto> responseConflict = dynamoPdfRasterService.insertPdfConversion(pdfConversionDto);
        StepVerifier.create(responseConflict).expectError(ConflictException.class).verify();

        response = dynamoPdfRasterService.insertPdfConversion(pdfConversionDto);
        StepVerifier.create(response)
                .expectErrorMatches(error -> error instanceof ConflictException && "Elemento già presente".equals(error.getMessage()))
                .verify();
    }


    @Test
    void updateRequestConversionKo() {

        RequestConversionDto requestConversionDto = MockRequestConversionDto();

        // Errore 500
        Mockito.when(dynamoPdfRasterService.updateRequestConversion(requestConversionDto))
                .thenReturn(Mono.error(new Generic500ErrorException("", "")));

        Mono<RequestConversionDto> response = dynamoPdfRasterService.updateRequestConversion(requestConversionDto);
        StepVerifier.create(response)
                .expectErrorMatches(error -> error instanceof Generic500ErrorException)
                .verify();

        Assertions.fail();

    }


}





