package it.pagopa.pn.ec.cartaceo.service;

import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.cartaceo.testutils.PaperEngageRequestFactory;
import it.pagopa.pn.ec.commons.exception.ss.attachment.AttachmentNotAvailableException;
import it.pagopa.pn.ec.commons.policy.Policy;
import it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.rest.call.upload.UploadCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.pdfraster.model.entity.PdfConversionEntity;
import it.pagopa.pn.ec.pdfraster.model.entity.RequestConversionEntity;
import it.pagopa.pn.ec.pdfraster.service.DynamoPdfRasterService;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.rest.v1.dto.*;

import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SqsResponse;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.consolidatore.utils.ContentTypes.APPLICATION_PDF;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.OK_CODE;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CartaceoRetryTest {

    @SpyBean
    SqsService sqsService;

    @Autowired
    private CartaceoSqsQueueName cartaceoSqsQueueName;

    @Autowired
    DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Autowired
    RepositoryManagerDynamoTableName repositoryManagerDynamoTableName;

    @SpyBean
    CartaceoService cartaceoService;

    @MockBean
    GestoreRepositoryCall gestoreRepositoryCall;

    @MockBean
    PaperMessageCall paperMessageCall;

    @SpyBean
    private DynamoPdfRasterService dynamoPdfRasterService;

    @MockBean
    private FileCall fileCall;

    @MockBean
    private DownloadCall downloadCall;

    @MockBean
    private UploadCall uploadCall;

    Message message = Message.builder().build();

    private static final String REQUEST_ID = "idTest";

    private static final String CLIENT_ID = "DEFAULT_ID_CLIENT_HEADER_VALUE";

    private static final String DOWNLOAD_URL = "http://downloadUrl";

    private static final String UPLOAD_URL = "http://uploadUrl";

    private static final String SECRET = "secret";
    private static DynamoDbTable<RequestConversionEntity> requestConversionEntityDynamoDbAsyncTable;
    private static DynamoDbTable<PdfConversionEntity> pdfConversionEntityDynamoDbAsyncTable;

    @BeforeEach
    void cleanup() {
        // Logica di cleanup per evitare che alcuni test influiscano su altri.
        ReflectionTestUtils.setField(cartaceoService, "idSaved", null);
        requestConversionEntityDynamoDbAsyncTable = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.richiesteConversioneRequestName(), TableSchema.fromBean(RequestConversionEntity.class));
        pdfConversionEntityDynamoDbAsyncTable = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.richiesteConversionePdfName(), TableSchema.fromBean(PdfConversionEntity.class));
        for (var page : requestConversionEntityDynamoDbAsyncTable.scan()) {
            for (var item : page.items()) {
                requestConversionEntityDynamoDbAsyncTable.deleteItem(item);
            }
        }
        for (var page : pdfConversionEntityDynamoDbAsyncTable.scan()) {
            for (var item : page.items()) {
                pdfConversionEntityDynamoDbAsyncTable.deleteItem(item);
            }
        }
    }

    private CartaceoPresaInCaricoInfo createCartaceoPresaInCaricoInfo() {
        return CartaceoPresaInCaricoInfo.builder()
                .requestIdx(REQUEST_ID)
                .xPagopaExtchCxId(CLIENT_ID)
                .paperEngageRequest(new PaperEngageRequest())
                .build();
    }

    private CartaceoPresaInCaricoInfo createCartaceoPresaInCaricoInfoPdfRaster() {
        return CartaceoPresaInCaricoInfo.builder().requestIdx(REQUEST_ID)
                .xPagopaExtchCxId(CLIENT_ID)
                .paperEngageRequest(PaperEngageRequestFactory.createDtoPaperRequestPdfRaster("requestPaId"))
                .build();
    }

    @Test
    void gestioneRetryCartaceoScheduler() {
        SqsServiceImpl mockSqsService = mock(SqsServiceImpl.class);
        when(mockSqsService.getOneMessage(cartaceoSqsQueueName.errorName(), CartaceoPresaInCaricoInfo.class))
                .thenReturn(Mono.empty());

        // chiamare il metodo sotto test
        cartaceoService.gestioneRetryCartaceoScheduler();

        // verificare che non sia stata eseguita alcuna operazione sul mock SQSService
        verify(mockSqsService, never()).deleteMessageFromQueue(eq(message), anyString());

    }

    /**
     * Test di successo per ogni tentativo disponibile di retry.
     * Esecuzione di tutti gli step.
     *
     * @param retryStep   lo step di retry corrente
     * @param timeElapsed il tempo passato dall'ultima retry
     */
    @ParameterizedTest
    @MethodSource("gestioneRetryOkArgsProvider")
    void gestioneRetryCartaceo_RetryOk(BigDecimal retryStep, long timeElapsed) {

        //GIVEN
        RequestDto requestDto = buildRequestDto();
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = createCartaceoPresaInCaricoInfo();

        if (retryStep != null) {
            requestDto.getRequestMetadata().getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(timeElapsed));
            requestDto.getRequestMetadata().getRetry().setRetryStep(retryStep);
        } else requestDto.getRequestMetadata().setRetry(null);

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();

        //WHEN
        mockGestoreRepository(clientId, requestId, requestDto);
        // Mock di una generica putRequest.
        when(paperMessageCall.putRequest(any(it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest.class))).thenReturn(Mono.just(new OperationResultCodeResponse().resultCode(OK_CODE)));
        mockSqsService();

        //THEN
        Mono<SqsResponse> response = cartaceoService.gestioneRetryCartaceo(cartaceoPresaInCaricoInfo, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(cartaceoPresaInCaricoInfo),eq(SENT.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(any(Message.class));

    }

    /**
     * Test di KO per la gestione retry. La chiamata putRequest() va in errore con un'eccezione generica.
     *
     */
    @Test
    void gestioneRetryCartaceo_Retry_PutRequestKo() {

        //GIVEN
        RequestDto requestDto = buildRequestDto();
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = createCartaceoPresaInCaricoInfo();
        requestDto.getRequestMetadata().setRetry(null);

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();

        //WHEN
        mockGestoreRepository(clientId, requestId, requestDto);
        when(paperMessageCall.putRequest(any(it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest.class))).thenReturn(Mono.error(new RuntimeException("KO")));
        mockSqsService();

        //THEN
        Mono<SqsResponse> response = cartaceoService.gestioneRetryCartaceo(cartaceoPresaInCaricoInfo, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnErrorQueue(cartaceoPresaInCaricoInfo);
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(message);
    }

    /**
     * Test di KO per la gestione retry. Superamento dei tentativi massimi disponibili.
     */
    @Test
    void gestioneRetryCartaceo_Retry_PutRequestKo_MaxRetriesExceeded() {

        //GIVEN
        RequestDto requestDto = buildRequestDto();
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = createCartaceoPresaInCaricoInfo();

        requestDto.getRequestMetadata().getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(45));
        requestDto.getRequestMetadata().getRetry().setRetryStep(BigDecimal.valueOf(2));

        String requestId = requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();

        //WHEN
        mockGestoreRepository(clientId, requestId, requestDto);
        when(paperMessageCall.putRequest(any(it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest.class))).thenReturn(Mono.error(new RuntimeException("KO")));
        mockSqsService();

        //THEN
        Mono<SqsResponse> response = cartaceoService.gestioneRetryCartaceo(cartaceoPresaInCaricoInfo, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(cartaceoPresaInCaricoInfo), eq(ERROR.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
        verify(cartaceoService, times(1)).sendNotificationOnDlqErrorQueue(cartaceoPresaInCaricoInfo);
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(message);
    }
    /**
     * Test di successo per la gestione retry nello step PDF_RASTER.
     * Gli altri step non vengono eseguiti.
     * Utilizzo di tutti i tentativi possibili.
     */
    @ParameterizedTest
    @MethodSource("gestioneRetryOkArgsProvider")
    void gestioneRetryCartaceoPdfRaster_RetryOk(BigDecimal retryStep, long timeElapsed) {

        //GIVEN
        RequestDto requestDto = buildRequestDto();

        String requestId = requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();

        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = createCartaceoPresaInCaricoInfoPdfRaster();
        cartaceoPresaInCaricoInfo.getPaperEngageRequest().setApplyRasterization(true);

        if (retryStep != null) {
            requestDto.getRequestMetadata().getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(timeElapsed));
            requestDto.getRequestMetadata().getRetry().setRetryStep(retryStep);
        } else requestDto.getRequestMetadata().setRetry(null);


        //WHEN
        mockGestoreRepository(clientId, requestId, requestDto);
        mockPdfRasterAttachmentSteps();
        mockSqsService();

        //THEN
        Mono<SqsResponse> response = cartaceoService.gestioneRetryCartaceo(cartaceoPresaInCaricoInfo, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();
        verify(dynamoPdfRasterService, times(1)).insertRequestConversion(any(RequestConversionDto.class));
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(message);
    }


    /**
     * Test di KO per la gestione retry nello step PDF_RASTER.
     * La chiamata getFile() va in errore con un'eccezione di tipo AttachmentNotAvailableException.
     *
     */
    @Test
    void gestioneRetryCartaceoPdfRaster_RetryKoGetFile() {

        //GIVEN
        RequestDto requestDto= buildRequestDto();

        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = createCartaceoPresaInCaricoInfoPdfRaster();
        cartaceoPresaInCaricoInfo.getPaperEngageRequest().setApplyRasterization(true);

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();
        requestDto.getRequestMetadata().setRetry(null);

        //WHEN
        mockGestoreRepository(clientId, requestId, requestDto);
        when(fileCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.error(new AttachmentNotAvailableException("fileKey")));
        mockSqsService();

        //THEN
        Mono<SqsResponse> response = cartaceoService.gestioneRetryCartaceo(cartaceoPresaInCaricoInfo, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnErrorQueue(cartaceoPresaInCaricoInfo);
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(message);

    }

    /**
     * Test di KO per la gestione retry nello step PDF_RASTER.
     * La chiamata downloadFile() va in errore con un'eccezione generica
     *
     */
    @Test
    void gestioneRetryCartaceoPdfRaster_RetryKoPostFile() {

        //GIVEN
        RequestDto requestDto= buildRequestDto();

        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = createCartaceoPresaInCaricoInfoPdfRaster();
        cartaceoPresaInCaricoInfo.getPaperEngageRequest().setApplyRasterization(true);

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();
        requestDto.getRequestMetadata().setRetry(null);

        //WHEN
        mockGestoreRepository(clientId, requestId, requestDto);
        mockPdfRasterAttachmentSteps();
        when(fileCall.postFile(anyString(), anyString(), any(FileCreationRequest.class))).thenReturn(Mono.error(new RuntimeException()));
        mockSqsService();

        //THEN
        Mono<SqsResponse> response = cartaceoService.gestioneRetryCartaceo(cartaceoPresaInCaricoInfo, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnErrorQueue(cartaceoPresaInCaricoInfo);
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(message);

    }

    /**
     * Test di KO per la gestione retry nello step PDF_RASTER.
     * La chiamata uploadCall() va in errore con un'eccezione generica
     *
     */
    @Test
    void gestioneRetryCartaceoPdfRaster_RetryKoUploadCall() {

        //GIVEN
        RequestDto requestDto= buildRequestDto();

        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = createCartaceoPresaInCaricoInfoPdfRaster();
        cartaceoPresaInCaricoInfo.getPaperEngageRequest().setApplyRasterization(true);

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();
        requestDto.getRequestMetadata().setRetry(null);

        //WHEN
        mockGestoreRepository(clientId, requestId, requestDto);
        mockPdfRasterAttachmentSteps();
        when(uploadCall.uploadFile(anyString(), anyString(), anyString(), anyString(), any(), anyString(), any(byte[].class))).thenReturn(Mono.error(new RuntimeException()));
        mockSqsService();

        //THEN
        Mono<SqsResponse> response = cartaceoService.gestioneRetryCartaceo(cartaceoPresaInCaricoInfo, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnErrorQueue(cartaceoPresaInCaricoInfo);
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(message);

    }

    /**
     * Test di KO per la gestione retry nello step PDF_RASTER.
     * La chiamata downloadFile() va in errore con un'eccezione generica
     *
     */
    @Test
    void gestioneRetryCartaceoPdfRaster_RetryKoDownloadCall() {

        //GIVEN
        RequestDto requestDto= buildRequestDto();

        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = createCartaceoPresaInCaricoInfoPdfRaster();
        cartaceoPresaInCaricoInfo.getPaperEngageRequest().setApplyRasterization(true);

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();
        requestDto.getRequestMetadata().setRetry(null);

        //WHEN
        mockGestoreRepository(clientId, requestId, requestDto);
        mockPdfRasterAttachmentSteps();
        when(downloadCall.downloadFile(DOWNLOAD_URL)).thenReturn(Mono.error(new RuntimeException()));
        mockSqsService();

        //THEN
        Mono<SqsResponse> response = cartaceoService.gestioneRetryCartaceo(cartaceoPresaInCaricoInfo, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnErrorQueue(cartaceoPresaInCaricoInfo);
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(message);

    }

    /**
     * Test di KO per la gestione retry nello step PDF_RASTER.
     * La chiamata insertRequestConversion() va in errore con un'eccezione di tipo DynamoDbException
     *
     */
    @Test
    void gestioneRetryCartaceoPdfRaster_RetryKoDynamo() {

        //GIVEN
        RequestDto requestDto= buildRequestDto();

        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = createCartaceoPresaInCaricoInfoPdfRaster();
        cartaceoPresaInCaricoInfo.getPaperEngageRequest().setApplyRasterization(true);

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();
        requestDto.getRequestMetadata().setRetry(null);

        //WHEN
        mockGestoreRepository(clientId, requestId, requestDto);
        mockPdfRasterAttachmentSteps();
        when(dynamoPdfRasterService.insertRequestConversion(any())).thenReturn(Mono.error(DynamoDbException.builder().build()));
        mockSqsService();

        //THEN
        Mono<SqsResponse> response = cartaceoService.gestioneRetryCartaceo(cartaceoPresaInCaricoInfo, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();
        verify(cartaceoService, times(1)).sendNotificationOnErrorQueue(cartaceoPresaInCaricoInfo);
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(message);

    }

    @ParameterizedTest
    @MethodSource("gestioneRetryOkArgsProvider")
    void gestioneRetryCartaceo_RetryAlreadyInSent(BigDecimal retryStep, long timeElapsed) {

        //GIVEN
        RequestDto requestDto = buildRequestDto();
        requestDto.getRequestMetadata().getEventsList().add(new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
                .statusDateTime(OffsetDateTime.now().minusMinutes(15)).status(SENT.getStatusTransactionTableCompliant())));
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = createCartaceoPresaInCaricoInfo();

        if (retryStep != null) {
            requestDto.getRequestMetadata().getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(timeElapsed));
            requestDto.getRequestMetadata().getRetry().setRetryStep(retryStep);
        } else requestDto.getRequestMetadata().setRetry(null);

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();

        //WHEN
        mockGestoreRepository(clientId, requestId, requestDto);
        // Mock di una generica putRequest.
        when(paperMessageCall.putRequest(any(it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest.class))).thenReturn(Mono.just(new OperationResultCodeResponse().resultCode(OK_CODE)));
        mockSqsService();

        //THEN
        Mono<SqsResponse> response = cartaceoService.gestioneRetryCartaceo(cartaceoPresaInCaricoInfo, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();
        verify(paperMessageCall, never()).putRequest(any(it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest.class));
        verify(cartaceoService, never()).sendNotificationOnStatusQueue(eq(cartaceoPresaInCaricoInfo),eq(SENT.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(any(Message.class));

    }



    private static RequestDto buildRequestDto()
    {
        Policy retryPolicies = new Policy();
        //RetryDTO
        RetryDto retryDto=new RetryDto();
        retryDto.setRetryPolicy(retryPolicies.getPolicy().get("PAPER"));
        ArrayList<EventsDto> eventsList = new ArrayList<>();
        eventsList.add(new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
                .status(RETRY.getStatusTransactionTableCompliant())
                .statusDateTime(OffsetDateTime.now().minusMinutes(10))));

        //RequestMetadataDTO
        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.paperRequestMetadata(new PaperRequestMetadataDto().requestPaId("requestPaId"));
        requestMetadata.setEventsList(eventsList);
        requestMetadata.setRetry(retryDto);

        //RequestDTO
        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(REQUEST_ID);
        requestDto.setxPagopaExtchCxId(CLIENT_ID);
        requestDto.setRequestMetadata(requestMetadata);

        return requestDto;
    }

    private static Stream<Arguments> gestioneRetryOkArgsProvider() {
        return Stream.of(Arguments.of(null, 0), Arguments.of(BigDecimal.ZERO, 15), Arguments.of(BigDecimal.ONE, 25), Arguments.of(BigDecimal.valueOf(2), 45));
    }

    private void mockPdfRasterAttachmentSteps() {
        String originalFileKey = randomAlphanumeric(10);
        FileDownloadInfo fileDownloadInfo = new FileDownloadInfo().url(DOWNLOAD_URL);
        when(fileCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.just(new FileDownloadResponse().key(originalFileKey).download(fileDownloadInfo).checksum("checksum").contentType("application/pdf")));

        when(downloadCall.downloadFile(DOWNLOAD_URL)).thenReturn(Mono.just(new ByteArrayOutputStream()));

        String newFileKey = randomAlphanumeric(10);
        when(fileCall.postFile(anyString(), anyString(), any(FileCreationRequest.class))).thenReturn(Mono.just(new FileCreationResponse().key(newFileKey).secret(SECRET).uploadUrl(UPLOAD_URL)));

        when(uploadCall.uploadFile(eq(newFileKey), eq(UPLOAD_URL), eq(SECRET), eq(APPLICATION_PDF), eq(DocumentTypeConfiguration.ChecksumEnum.SHA256), anyString(), any(byte[].class))).thenReturn(Mono.empty());
    }

    private void mockSqsService() {
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(cartaceoSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));
        doReturn(Mono.just(ChangeMessageVisibilityResponse.builder().build())).when(sqsService).changeMessageVisibility(any(), any(), any());
    }

    private void mockGestoreRepository(String clientId, String requestId, RequestDto requestDto) {
        // Mock di una generica getRichiesta.
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));

        // Mock di una generica patchRichiesta.
        when(gestoreRepositoryCall.patchRichiesta(eq(clientId), eq(requestId), any(PatchDto.class))).thenReturn(Mono.just(requestDto));
    }


}