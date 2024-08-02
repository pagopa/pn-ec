package it.pagopa.pn.ec.pdfraster.service;

import com.github.dockerjava.api.exception.ConflictException;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic500ErrorException;
import it.pagopa.pn.ec.pdfraster.model.entity.*;

import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.rest.v1.dto.AttachmentToConvertDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestConversionDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Collections;
import java.util.Objects;

import static org.mockito.Mockito.when;


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
        return new RequestConversionDto().requestId("123456789")
                .attachments(Collections.singletonList(
                        new AttachmentToConvertDto().newFileKey("12345")
                ));
    }



    @Test
    void insertRequestConversionOk() {

        RequestConversionDto requestConversionDto = MockRequestConversionDto();

        Mono<RequestConversionDto> response = dynamoPdfRasterService.insertRequestConversion(requestConversionDto);

        StepVerifier.create(response)
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (AttachmentToConvertDto keyValue : requestConversionDto.getAttachments()) {
            PdfConversionEntity result = pdfConversionEntityDynamoDbTable.getItem(builder -> builder.key(Key.builder().partitionValue(keyValue.getNewFileKey()).build()));
            Assertions.assertNotNull(result);
            Assertions.assertEquals(requestConversionDto.getRequestId(), result.getRequestId());
        }
    }

    @Test
    void updateRequestConversionOk() {

        RequestConversionDto requestConversionDto = MockRequestConversionDto();

        Mono<RequestConversionDto> response = dynamoPdfRasterService.insertRequestConversion(requestConversionDto);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        String requestId = requestConversionDto.getRequestId();

        for (AttachmentToConvertDto attachment : requestConversionDto.getAttachments()) {
                attachment.setConverted(true);
        }

        Mono<RequestConversionDto> responseUpdate = dynamoPdfRasterService.updateRequestConversion("12345", true);

        StepVerifier.create(responseUpdate)
                .expectNextMatches(updatedDto -> updatedDto.getRequestId().equals(requestId))
                .verifyComplete();

        RequestConversionEntity updatedEcRequestConversionEntity = requestConversionEntityDynamoDbTable.getItem(
                builder -> builder.key(Key.builder().partitionValue(requestId).build())
        );
        Assertions.assertNotNull(updatedEcRequestConversionEntity);

        boolean isConverted = updatedEcRequestConversionEntity.getAttachments().stream()
                .allMatch(AttachmentToConvert::getConverted);
        Assertions.assertTrue(isConverted);
    }


    @Test
    void insertRequestConversionKoInternalServerError() {
        RequestConversionDto requestConversionDto = MockRequestConversionDto();


        when(dynamoPdfRasterService.insertRequestConversion(requestConversionDto))
                .thenReturn(Mono.error(new Generic500ErrorException("Internal Server Error", "")));

        Mono<RequestConversionDto> response = dynamoPdfRasterService.insertRequestConversion(requestConversionDto);
        StepVerifier.create(response)
                .expectErrorMatches(error -> error instanceof Generic500ErrorException && error.getMessage().startsWith("Internal Server Error"))
                .verify();
    }

    @Test
    void insertRequestConversionKoConflictError() {

        RequestConversionDto requestConversionDto = MockRequestConversionDto();

        when(dynamoPdfRasterService.insertRequestConversion(requestConversionDto))
                .thenReturn(Mono.error(new ConflictException("Element already exists")));

        Mono<RequestConversionDto> responseConflict = dynamoPdfRasterService.insertRequestConversion(requestConversionDto);

        StepVerifier.create(responseConflict)
                .expectError(ConflictException.class)
                .verify();
    }



    @Test
    void updateRequestConversionInvalidConvertedValue() {

        when(dynamoPdfRasterService.updateRequestConversion("FileKey", false))
                .thenReturn(Mono.error(new IllegalArgumentException()));

        Mono<RequestConversionDto> responseUpdate = dynamoPdfRasterService.updateRequestConversion("StringFileKey", false);

        StepVerifier.create(responseUpdate)
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                        && throwable.getMessage().equals("Invalid value for 'converted': must be true."))
                .verify();

    }



}





