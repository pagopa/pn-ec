package it.pagopa.pn.ec.richiestemetadati;

import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.model.entity.PaperRequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.rest.v1.dto.RequestMetadataPatchRequest;
import it.pagopa.pn.ec.richiestemetadati.service.impl.PaperRequestMetadataPatchServiceImpl;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.OffsetDateTime;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTestWebEnv
@CustomLog
@DirtiesContext
class PaperRequestMetadataPatchServiceTest {

    @Autowired
    PaperRequestMetadataPatchServiceImpl paperRequestMetadataPatchService;
    @Autowired
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    DynamoDbAsyncTableDecorator<RequestMetadata> dynamoDbAsyncTableDecorator;
    TableSchema<RequestMetadata> requestMetadataTableSchema;


    private static final String X_PAGOPA_CX_ID = "CLIENT_001";
    private static final String REQUEST_ID = "REQ_12345";
    private static final String INVALID_REQUEST_ID = "INVALID_REQUEST_ID";
    private static final String COMPOSITE_KEY = "CLIENT_001~REQ_12345";
    private static final OffsetDateTime timestamp = OffsetDateTime.now();

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        RequestMetadata testRequest = createTestRequestMetadata(COMPOSITE_KEY, false, timestamp);
        requestMetadataTableSchema = TableSchema.fromBean(RequestMetadata.class);
        dynamoDbAsyncTableDecorator = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedAsyncClient.table("pn-EcRichiesteMetadati", requestMetadataTableSchema));
        try {
            dynamoDbAsyncTableDecorator.deleteItem(
                    Key.builder().partitionValue(COMPOSITE_KEY).build()
            ).get();
        } catch (Exception e) {
            log.info("item does not exist: {}", e.getMessage());
        }
        dynamoDbAsyncTableDecorator.putItem(testRequest).get();
    }


    @Test
    void testPatchIsOpenReworkRequest_Success_CloseRework() throws ExecutionException, InterruptedException {
        // Given
        RequestMetadataPatchRequest openRequest = new RequestMetadataPatchRequest();
        openRequest.setIsOpenReworkRequest(true);

        paperRequestMetadataPatchService
                .patchIsOpenReworkRequest(X_PAGOPA_CX_ID, REQUEST_ID, openRequest)
                .block();

        // When
        RequestMetadataPatchRequest closeRequest = new RequestMetadataPatchRequest();
        closeRequest.setIsOpenReworkRequest(false);

        Mono<Void> result = paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                X_PAGOPA_CX_ID,
                REQUEST_ID,
                closeRequest
        );

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        RequestMetadata updatedRequestMetadata = dynamoDbAsyncTableDecorator
                .getItem(Key.builder().partitionValue(COMPOSITE_KEY).build())
                .get();

        assertThat(updatedRequestMetadata).isNotNull();
        assertThat(updatedRequestMetadata.getPaperRequestMetadata().getIsOpenReworkRequest())
                .isFalse();
    }

    @Test
    void testPatchIsOpenReworkRequest_Success_VerifyCompositeKey() throws ExecutionException, InterruptedException {
        // Given
        RequestMetadataPatchRequest patchRequest = new RequestMetadataPatchRequest();
        patchRequest.setIsOpenReworkRequest(true);

        // When
        Mono<Void> result = paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                X_PAGOPA_CX_ID,
                REQUEST_ID,
                patchRequest
        );

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        RequestMetadata updatedRequestMetadata = dynamoDbAsyncTableDecorator
                .getItem(Key.builder().partitionValue(COMPOSITE_KEY).build())
                .get();

        assertThat(updatedRequestMetadata.getRequestId()).isEqualTo(COMPOSITE_KEY);
        assertThat(updatedRequestMetadata.getRequestId()).contains("~");
        assertThat(updatedRequestMetadata.getRequestId()).startsWith(X_PAGOPA_CX_ID);
        assertThat(updatedRequestMetadata.getRequestId()).endsWith(REQUEST_ID);
    }


    @Test
    void testPatchIsOpenReworkRequest_RequestNotFound() {
        // Given
        String nonExistentRequestId = "NON_EXISTENT_999";
        RequestMetadataPatchRequest patchRequest = new RequestMetadataPatchRequest();
        patchRequest.setIsOpenReworkRequest(true);

        // When
        Mono<Void> result = paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                X_PAGOPA_CX_ID,
                nonExistentRequestId,
                patchRequest
        );

        // Then
        StepVerifier.create(result)
                .expectError(RepositoryManagerException.RequestNotFoundException.class)
                .verify();
    }

    @Test
    void testPatchIsOpenReworkRequest_NullPatchRequest() {
        // Given
        RequestMetadataPatchRequest patchRequest = null;

        // When
        Mono<Void> result = paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                X_PAGOPA_CX_ID,
                REQUEST_ID,
                patchRequest
        );

        // Then
        StepVerifier.create(result)
                .expectError(RepositoryManagerException.RequestMalformedException.class)
                .verify();
    }

    @Test
    void testPatchIsOpenReworkRequest_BlankRequestId() {
        // Given
        RequestMetadataPatchRequest patchRequest = new RequestMetadataPatchRequest();
        patchRequest.setIsOpenReworkRequest(true);

        // When
        Mono<Void> result = paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                X_PAGOPA_CX_ID,
                "",
                patchRequest
        );

        // Then
        StepVerifier.create(result)
                .expectError(RepositoryManagerException.RequestMalformedException.class)
                .verify();
    }

    @Test
    void testPatchIsOpenReworkRequest_NullRequestId() {
        // Given
        RequestMetadataPatchRequest patchRequest = new RequestMetadataPatchRequest();
        patchRequest.setIsOpenReworkRequest(true);

        // When
        Mono<Void> result = paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                X_PAGOPA_CX_ID,
                null,
                patchRequest
        );

        // Then
        StepVerifier.create(result)
                .expectError(RepositoryManagerException.RequestMalformedException.class)
                .verify();
    }

    @Test
    void testPatchIsOpenReworkRequest_WhitespaceRequestId() {
        // Given
        RequestMetadataPatchRequest patchRequest = new RequestMetadataPatchRequest();
        patchRequest.setIsOpenReworkRequest(true);

        // When
        Mono<Void> result = paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                X_PAGOPA_CX_ID,
                "   ",
                patchRequest
        );

        // Then
        StepVerifier.create(result)
                .expectError(RepositoryManagerException.RequestMalformedException.class)
                .verify();
    }


    @Test
    void testPatchIsOpenReworkRequest_ToggleRework_MultipleChanges() throws ExecutionException, InterruptedException {
        RequestMetadataPatchRequest openRequest = new RequestMetadataPatchRequest();
        openRequest.setIsOpenReworkRequest(true);

        RequestMetadataPatchRequest closeRequest = new RequestMetadataPatchRequest();
        closeRequest.setIsOpenReworkRequest(false);

        StepVerifier.create(paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                        X_PAGOPA_CX_ID, REQUEST_ID, openRequest))
                .verifyComplete();

        RequestMetadata afterOpen = dynamoDbAsyncTableDecorator
                .getItem(Key.builder().partitionValue(COMPOSITE_KEY).build())
                .get();
        assertThat(afterOpen.getPaperRequestMetadata().getIsOpenReworkRequest()).isTrue();

        StepVerifier.create(paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                        X_PAGOPA_CX_ID, REQUEST_ID, closeRequest))
                .verifyComplete();

        RequestMetadata afterClose = dynamoDbAsyncTableDecorator
                .getItem(Key.builder().partitionValue(COMPOSITE_KEY).build())
                .get();
        assertThat(afterClose.getPaperRequestMetadata().getIsOpenReworkRequest()).isFalse();

        StepVerifier.create(paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                        X_PAGOPA_CX_ID, REQUEST_ID, openRequest))
                .verifyComplete();

        RequestMetadata afterReopen = dynamoDbAsyncTableDecorator
                .getItem(Key.builder().partitionValue(COMPOSITE_KEY).build())
                .get();
        assertThat(afterReopen.getPaperRequestMetadata().getIsOpenReworkRequest()).isTrue();
    }

    @Test
    void testPatchIsOpenReworkRequest_IdempotentUpdate_SameValueTwice() throws ExecutionException, InterruptedException {
        RequestMetadataPatchRequest patchRequest = new RequestMetadataPatchRequest();
        patchRequest.setIsOpenReworkRequest(true);

        StepVerifier.create(paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                        X_PAGOPA_CX_ID, REQUEST_ID, patchRequest))
                .verifyComplete();

        RequestMetadata afterFirstUpdate = dynamoDbAsyncTableDecorator
                .getItem(Key.builder().partitionValue(COMPOSITE_KEY).build())
                .get();

        StepVerifier.create(paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                        X_PAGOPA_CX_ID, REQUEST_ID, patchRequest))
                .verifyComplete();

        RequestMetadata afterSecondUpdate = dynamoDbAsyncTableDecorator
                .getItem(Key.builder().partitionValue(COMPOSITE_KEY).build())
                .get();

        assertThat(afterFirstUpdate.getPaperRequestMetadata().getIsOpenReworkRequest())
                .isEqualTo(afterSecondUpdate.getPaperRequestMetadata().getIsOpenReworkRequest());
        assertThat(afterSecondUpdate.getPaperRequestMetadata().getIsOpenReworkRequest())
                .isTrue();
    }

    @Test
    void testPatchIsOpenReworkRequest_MultipleClientIds_SameRequestId() throws ExecutionException, InterruptedException {
        String clientId2 = "CLIENT_002";
        String compositeKey2 = clientId2 + "~" + REQUEST_ID;

        RequestMetadata testRequest2 = createTestRequestMetadata(
                compositeKey2,
                false,
                OffsetDateTime.now()
        );
        dynamoDbAsyncTableDecorator.putItem(testRequest2).get();

        RequestMetadataPatchRequest patchRequest = new RequestMetadataPatchRequest();
        patchRequest.setIsOpenReworkRequest(true);

        StepVerifier.create(paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                        X_PAGOPA_CX_ID, REQUEST_ID, patchRequest))
                .verifyComplete();

        StepVerifier.create(paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                        clientId2, REQUEST_ID, patchRequest))
                .verifyComplete();

        RequestMetadata updated1 = dynamoDbAsyncTableDecorator
                .getItem(Key.builder().partitionValue(COMPOSITE_KEY).build())
                .get();
        RequestMetadata updated2 = dynamoDbAsyncTableDecorator
                .getItem(Key.builder().partitionValue(compositeKey2).build())
                .get();

        assertThat(updated1.getPaperRequestMetadata().getIsOpenReworkRequest()).isTrue();
        assertThat(updated2.getPaperRequestMetadata().getIsOpenReworkRequest()).isTrue();
        assertThat(updated1.getRequestId()).isNotEqualTo(updated2.getRequestId());
    }

    @Test
    void testPatchIsOpenReworkRequest_DifferentRequestIds_SameClient() throws ExecutionException, InterruptedException {
        String requestId2 = "REQ_67890";
        String compositeKey2 = X_PAGOPA_CX_ID + "~" + requestId2;

        RequestMetadata testRequest2 = createTestRequestMetadata(
                compositeKey2,
                false,
                OffsetDateTime.now()
        );
        dynamoDbAsyncTableDecorator.putItem(testRequest2).get();

        RequestMetadataPatchRequest openRequest = new RequestMetadataPatchRequest();
        openRequest.setIsOpenReworkRequest(true);

        RequestMetadataPatchRequest closeRequest = new RequestMetadataPatchRequest();
        closeRequest.setIsOpenReworkRequest(false);

        StepVerifier.create(paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                        X_PAGOPA_CX_ID, REQUEST_ID, openRequest))
                .verifyComplete();

        StepVerifier.create(paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                        X_PAGOPA_CX_ID, requestId2, closeRequest))
                .verifyComplete();

        RequestMetadata updated1 = dynamoDbAsyncTableDecorator
                .getItem(Key.builder().partitionValue(COMPOSITE_KEY).build())
                .get();
        RequestMetadata updated2 = dynamoDbAsyncTableDecorator
                .getItem(Key.builder().partitionValue(compositeKey2).build())
                .get();

        assertThat(updated1.getPaperRequestMetadata().getIsOpenReworkRequest()).isTrue();
        assertThat(updated2.getPaperRequestMetadata().getIsOpenReworkRequest()).isFalse();
    }

    @Test
    void testPatchIsOpenReworkRequest_paperEngageRequestNull_should_return_error() throws ExecutionException, InterruptedException {
        RequestMetadata localTestRequest = createTestRequestMetadata(COMPOSITE_KEY, false, timestamp);
        localTestRequest.setPaperRequestMetadata(null);
        try {
            dynamoDbAsyncTableDecorator.deleteItem(
                    Key.builder().partitionValue(COMPOSITE_KEY).build()
            ).get();
        } catch (Exception e) {
            log.info("item does not exist: {}", e.getMessage());
        }
        dynamoDbAsyncTableDecorator.putItem(localTestRequest).get();
        RequestMetadataPatchRequest patchRequest = new RequestMetadataPatchRequest();
        patchRequest.setIsOpenReworkRequest(true);

        Mono<Void> result = paperRequestMetadataPatchService.patchIsOpenReworkRequest(X_PAGOPA_CX_ID, REQUEST_ID, patchRequest);
        StepVerifier.create(result)
                .expectError(RepositoryManagerException.RequestMalformedException.class).verify();

    }

    @Test
    void testPatchIsOpenReworkRequest_GetRequestMetadata_isNotFound_should_return_error()  {
        RequestMetadataPatchRequest patchRequest = new RequestMetadataPatchRequest();
        patchRequest.setIsOpenReworkRequest(true);

        Mono<Void> result = paperRequestMetadataPatchService.patchIsOpenReworkRequest(X_PAGOPA_CX_ID, INVALID_REQUEST_ID, patchRequest);
        StepVerifier.create(result)
                .expectError(RepositoryManagerException.RequestNotFoundException.class).verify();

    }

    @Test
    void testPatchIsOpenReworkRequest_PreservesOtherFields() throws ExecutionException, InterruptedException {
        // Given
        RequestMetadata originalMetadata = dynamoDbAsyncTableDecorator
                .getItem(Key.builder().partitionValue(COMPOSITE_KEY).build())
                .get();

        String originalIun = originalMetadata.getPaperRequestMetadata().getIun();
        String originalRequestPaId = originalMetadata.getPaperRequestMetadata().getRequestPaId();
        OffsetDateTime originalTimestamp = originalMetadata.getClientRequestTimeStamp();

        RequestMetadataPatchRequest patchRequest = new RequestMetadataPatchRequest();
        patchRequest.setIsOpenReworkRequest(true);

        // When
        StepVerifier.create(paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                        X_PAGOPA_CX_ID, REQUEST_ID, patchRequest))
                .verifyComplete();

        RequestMetadata updatedMetadata = dynamoDbAsyncTableDecorator
                .getItem(Key.builder().partitionValue(COMPOSITE_KEY).build())
                .get();

        assertThat(updatedMetadata.getPaperRequestMetadata().getIun())
                .isEqualTo(originalIun);
        assertThat(updatedMetadata.getPaperRequestMetadata().getRequestPaId())
                .isEqualTo(originalRequestPaId);
        assertThat(updatedMetadata.getClientRequestTimeStamp())
                .isEqualTo(originalTimestamp);
        assertThat(updatedMetadata.getXPagopaExtchCxId())
                .isEqualTo(X_PAGOPA_CX_ID);
    }

    @Test
    void testPatchIsOpenReworkRequest_InitiallyNull_SetToTrue() throws ExecutionException, InterruptedException {
        String specialKey = X_PAGOPA_CX_ID + "~" + "REQ_NULL_TEST";
        RequestMetadata nullMetadata = createTestRequestMetadata(specialKey, false, OffsetDateTime.now());
        nullMetadata.getPaperRequestMetadata().setIsOpenReworkRequest(null);
        dynamoDbAsyncTableDecorator.putItem(nullMetadata).get();

        RequestMetadataPatchRequest patchRequest = new RequestMetadataPatchRequest();
        patchRequest.setIsOpenReworkRequest(true);

        StepVerifier.create(paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                        X_PAGOPA_CX_ID, "REQ_NULL_TEST", patchRequest))
                .verifyComplete();

        RequestMetadata updatedMetadata = dynamoDbAsyncTableDecorator
                .getItem(Key.builder().partitionValue(specialKey).build())
                .get();

        assertThat(updatedMetadata.getPaperRequestMetadata().getIsOpenReworkRequest())
                .isNotNull()
                .isTrue();
    }


    @Test
    void testPatchIsOpenReworkRequest_SequentialUpdates_NoConflict() throws ExecutionException, InterruptedException {
        RequestMetadataPatchRequest patchRequest1 = new RequestMetadataPatchRequest();
        patchRequest1.setIsOpenReworkRequest(true);

        RequestMetadataPatchRequest patchRequest2 = new RequestMetadataPatchRequest();
        patchRequest2.setIsOpenReworkRequest(false);

        paperRequestMetadataPatchService
                .patchIsOpenReworkRequest(X_PAGOPA_CX_ID, REQUEST_ID, patchRequest1)
                .block();

        paperRequestMetadataPatchService
                .patchIsOpenReworkRequest(X_PAGOPA_CX_ID, REQUEST_ID, patchRequest2)
                .block();

        RequestMetadata finalMetadata = dynamoDbAsyncTableDecorator
                .getItem(Key.builder().partitionValue(COMPOSITE_KEY).build())
                .get();

        assertThat(finalMetadata.getPaperRequestMetadata().getIsOpenReworkRequest())
                .isFalse();
    }

    @Test
    void testPatchIsOpenReworkRequest_Success_OpenRework() throws ExecutionException, InterruptedException {
        RequestMetadataPatchRequest patchRequest = new RequestMetadataPatchRequest();
        patchRequest.setIsOpenReworkRequest(true);

        Mono<Void> result = paperRequestMetadataPatchService.patchIsOpenReworkRequest(X_PAGOPA_CX_ID, REQUEST_ID, patchRequest);
        StepVerifier.create(result)
                .verifyComplete();
        RequestMetadata updatedRequestMetadata = dynamoDbAsyncTableDecorator.getItem(Key.builder().partitionValue(COMPOSITE_KEY).build()).get();
        Assertions.assertNotNull(updatedRequestMetadata);
        Assertions.assertEquals(patchRequest.getIsOpenReworkRequest(), updatedRequestMetadata.getPaperRequestMetadata().getIsOpenReworkRequest());
    }

    public static  RequestMetadata createTestRequestMetadata(String compositeKey, boolean openRework, OffsetDateTime timestamp) {
        PaperRequestMetadata paperRequestMetadata = PaperRequestMetadata.builder()
                .requestPaId(compositeKey)
                .isOpenReworkRequest(openRework)
                .iun(REQUEST_ID)
                .duplicateCheckPassthrough(false)
                .printType("test").build();
        return RequestMetadata.builder()
                .requestId(compositeKey)
                .paperRequestMetadata(paperRequestMetadata)
                .xPagopaExtchCxId(X_PAGOPA_CX_ID)
                .clientRequestTimeStamp(timestamp)
                .build();
    }


}