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
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Duration;
import java.util.Collections;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
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



    private RequestConversionDto createMockRequestConversionDto() {
        return new RequestConversionDto()
                .requestId("123456789")
                .attachments(Collections.singletonList(
                        new AttachmentToConvertDto().newFileKey("12345")
                ));
    }

    @Test
    void insertRequestConversionOk() {
        RequestConversionDto requestConversionDto = createMockRequestConversionDto();

        Mono<RequestConversionDto> response = dynamoPdfRasterService.insertRequestConversion(requestConversionDto);

        StepVerifier.create(response)
                .expectNextMatches(this::validateNonNullResponse)
                .verifyComplete();

        awaitPropagation();

        verifyPdfConversionEntities(requestConversionDto);
    }


    private boolean validateNonNullResponse(RequestConversionDto responseDto) {
        return responseDto != null;
    }

    private void awaitPropagation() {
        await().atMost(Duration.ofSeconds(2))
                .until(this::verifyPdfConversionEntitiesExist);
    }

    private boolean verifyPdfConversionEntitiesExist() {
        for (AttachmentToConvertDto attachmentDto : createMockRequestConversionDto().getAttachments()) {
            PdfConversionEntity result = pdfConversionEntityDynamoDbTable.getItem(builder ->
                    builder.key(Key.builder().partitionValue(attachmentDto.getNewFileKey()).build()));
            if (result == null) {
                return false;
            }
        }
        return true;
    }

    private void verifyPdfConversionEntities(RequestConversionDto requestConversionDto) {
        for (AttachmentToConvertDto attachmentDto : requestConversionDto.getAttachments()) {
            PdfConversionEntity result = pdfConversionEntityDynamoDbTable.getItem(builder ->
                    builder.key(Key.builder().partitionValue(attachmentDto.getNewFileKey()).build()));
            Assertions.assertNotNull(result);
            Assertions.assertEquals(requestConversionDto.getRequestId(), result.getRequestId());
        }
    }


    @Test
    void updateRequestConversionOk() {
        RequestConversionDto requestConversionDto = createMockRequestConversionDto();

        Mono<RequestConversionDto> insertResponse = dynamoPdfRasterService.insertRequestConversion(requestConversionDto);
        StepVerifier.create(insertResponse)
                .expectNextCount(1)
                .verifyComplete();

        markAttachmentsAsConverted(requestConversionDto);

        Mono<RequestConversionDto> updateResponse = dynamoPdfRasterService.updateRequestConversion("12345", true, RandomStringUtils.randomAlphanumeric(10));


        StepVerifier.create(updateResponse)
                .expectNextMatches(updatedDto -> updatedDto.getRequestId().equals(requestConversionDto.getRequestId()))
                .verifyComplete();

        verifyUpdatedRequestConversionEntity(requestConversionDto.getRequestId());
    }


    private void markAttachmentsAsConverted(RequestConversionDto dto) {
        dto.getAttachments().forEach(attachment -> attachment.setConverted(true));
    }

    private void verifyUpdatedRequestConversionEntity(String requestId) {
        RequestConversionEntity updatedEntity = requestConversionEntityDynamoDbTable.getItem(
                builder -> builder.key(Key.builder().partitionValue(requestId).build())
        );
        Assertions.assertNotNull(updatedEntity);

        boolean isConverted = updatedEntity.getAttachments().stream()
                .allMatch(AttachmentToConvert::getConverted);
        Assertions.assertTrue(isConverted);
    }


    @Test
    void insertRequestConversionKoInternalServerError() {
        RequestConversionDto requestConversionDto = createMockRequestConversionDto();

        simulateInternalServerError();

        StepVerifier.create(dynamoPdfRasterService.insertRequestConversion(requestConversionDto))
                .expectErrorMatches(error ->
                        error instanceof Generic500ErrorException && error.getMessage().startsWith("Internal Server Error"))
                .verify();
    }

    private void simulateInternalServerError() {
        when(dynamoPdfRasterService.insertRequestConversion(any(RequestConversionDto.class)))
                .thenReturn(Mono.error(new Generic500ErrorException("Internal Server Error", "")));
    }



    @Test
    void insertRequestConversionKoConflictError() {

        RequestConversionDto requestConversionDto = createMockRequestConversionDto();

        simulateConflictError();

        StepVerifier.create(dynamoPdfRasterService.insertRequestConversion(requestConversionDto))
                .expectError(ConflictException.class)
                .verify();
    }

    private void simulateConflictError() {
        when(dynamoPdfRasterService.insertRequestConversion(any(RequestConversionDto.class)))
                .thenReturn(Mono.error(new ConflictException("Element already exists")));
    }



    @Test
    void updateRequestConversionInvalidConvertedValue() {
        String fileKey = "FileKey";
        boolean convertedValue = false;

        simulateInvalidConvertedValueError(fileKey, convertedValue);


        StepVerifier.create(dynamoPdfRasterService.updateRequestConversion(fileKey, convertedValue, RandomStringUtils.randomAlphanumeric(10)))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                        && throwable.getMessage().equals("Invalid value for 'converted': must be true."))
                .verify();
    }

    private void simulateInvalidConvertedValueError(String fileKey, boolean converted) {
        when(dynamoPdfRasterService.updateRequestConversion(fileKey, converted, RandomStringUtils.randomAlphanumeric(10)))
                .thenReturn(Mono.error(new IllegalArgumentException("Invalid value for 'converted': must be true.")));
    }



}





