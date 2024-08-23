package it.pagopa.pn.ec.cartaceo.service;

import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.cartaceo.testutils.PaperEngageRequestFactory;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.policy.Policy;
import it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.pdfraster.service.DynamoPdfRasterService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sms.service.SmsService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SqsResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.OK_CODE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class CartaceoRetryTest {

    @SpyBean
    SqsService sqsService;

    @Autowired
    private CartaceoSqsQueueName cartaceoSqsQueueName;

    @Autowired
    SmsService smsService;

    @SpyBean
    CartaceoService cartaceoService;

    @MockBean
    GestoreRepositoryCall gestoreRepositoryCall;

    @MockBean
    PaperMessageCall paperMessageCall;

    @SpyBean
    private DynamoPdfRasterService dynamoPdfRasterService;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    Message message = Message.builder().build();

    private static final String REQUEST_ID = "idTest";

    private static final String CLIENT_ID = "DEFAULT_ID_CLIENT_HEADER_VALUE";

    @AfterEach
    void cleanup() {
        ReflectionTestUtils.setField(cartaceoService, "idSaved", null);
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
                .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
                .paperEngageRequest(PaperEngageRequestFactory.createDtoPaperRequestPdfRaster(2))
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

    @ParameterizedTest
    @MethodSource("gestioneRetryOkArgsProvider")
    void gestioneRetryCartaceoPdfRaster_RetryOk(BigDecimal retryStep, long timeElapsed) {

        RequestDto requestDto= buildRequestDto();

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();

        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = createCartaceoPresaInCaricoInfoPdfRaster();

        if (retryStep != null) {
            requestDto.getRequestMetadata().getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(timeElapsed));
            requestDto.getRequestMetadata().getRetry().setRetryStep(retryStep);
        } else requestDto.getRequestMetadata().setRetry(null);


        // Mock di una generica getRichiesta.
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));

        // Mock di una generica patchRichiesta.
        when(gestoreRepositoryCall.patchRichiesta(eq(clientId), eq(requestId), any(PatchDto.class))).thenReturn(Mono.just(requestDto));

        doReturn(Mono.just(ChangeMessageVisibilityResponse.builder().build())).when(sqsService).changeMessageVisibility(any(), any(), any());

        Mono<SqsResponse> response = cartaceoService.gestioneRetryCartaceo(cartaceoPresaInCaricoInfo, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(cartaceoService, times(1)).sendNotificationOnErrorQueue(cartaceoPresaInCaricoInfo);
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(message);
        verify(dynamoPdfRasterService, times(1)).insertRequestConversion(any(RequestConversionDto.class));
    }

    @ParameterizedTest
    @MethodSource("gestioneRetryKoArgsProvider")
    void gestioneRetryCartaceoPdfRaster_RetryKoDynamo(BigDecimal retryStep, long timeElapsed) {

        RequestDto requestDto= buildRequestDto();

        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = createCartaceoPresaInCaricoInfoPdfRaster();

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();

        if (retryStep != null) {
            requestDto.getRequestMetadata().getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(timeElapsed));
            requestDto.getRequestMetadata().getRetry().setRetryStep(retryStep);
        } else requestDto.getRequestMetadata().setRetry(null);


        // Mock di una generica getRichiesta.
        when(gestoreRepositoryCall.getRichiesta(any(), any())).thenReturn(Mono.just(requestDto));

        // Mock di una generica patchRichiesta.
        when(gestoreRepositoryCall.patchRichiesta(eq(clientId), eq(requestId), any(PatchDto.class))).thenReturn(Mono.just(requestDto));

        when(dynamoPdfRasterService.insertRequestConversion(any())).thenReturn(Mono.error(DynamoDbException.builder().build()));

        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(cartaceoSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));
        doReturn(Mono.just(ChangeMessageVisibilityResponse.builder().build())).when(sqsService).changeMessageVisibility(any(), any(), any());

        Mono<SqsResponse> response = cartaceoService.gestioneRetryCartaceo(cartaceoPresaInCaricoInfo, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(cartaceoService, times(1)).sendNotificationOnErrorQueue(cartaceoPresaInCaricoInfo);
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(message);

    }

    private static RequestDto buildRequestDto()
    {
        Policy retryPolicies = new Policy();
        //RetryDTO
        RetryDto retryDto=new RetryDto();
        retryDto.setRetryPolicy(retryPolicies.getPolicy().get("PAPER"));

        //RequestMetadataDTO
        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.setEventsList(List.of(new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
                .status(RETRY.getStatusTransactionTableCompliant())
                .statusDateTime(OffsetDateTime.now().minusMinutes(10)))));
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

    private static Stream<Arguments> gestioneRetryKoArgsProvider() {
        return Stream.of(Arguments.of(null, 0), Arguments.of(BigDecimal.ZERO, 15), Arguments.of(BigDecimal.ONE, 25));
    }

    /**
     * Test di successo per ogni tentativo disponibile di retry.
     *
     * @param retryStep   lo step di retry corrente
     * @param timeElapsed il tempo passato dall'ultima retry
     */
    @ParameterizedTest
    @MethodSource("gestioneRetryOkArgsProvider")
    void gestioneRetryCartaceo_RetryOk(BigDecimal retryStep, long timeElapsed) {

        RequestDto requestDto = buildRequestDto();
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = createCartaceoPresaInCaricoInfo();

        if (retryStep != null) {
            requestDto.getRequestMetadata().getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(timeElapsed));
            requestDto.getRequestMetadata().getRetry().setRetryStep(retryStep);
        } else requestDto.getRequestMetadata().setRetry(null);

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();

        // Mock di una generica getRichiesta.
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));

        // Mock di una generica patchRichiesta.
        when(gestoreRepositoryCall.patchRichiesta(eq(clientId), eq(requestId), any(PatchDto.class))).thenReturn(Mono.just(requestDto));

        // Mock di una generica putRequest.
        when(paperMessageCall.putRequest(any(it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest.class))).thenReturn(Mono.just(new OperationResultCodeResponse().resultCode(OK_CODE)));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(cartaceoSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<SqsResponse> response = cartaceoService.gestioneRetryCartaceo(cartaceoPresaInCaricoInfo, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(cartaceoPresaInCaricoInfo),eq(SENT.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(any(Message.class));

    }

    /**
     * Test di KO per la gestione retry. La chiamata putRequest() va in errore con un'eccezione generica.
     * Utilizzo di tutti i tentativi possibili.
     *
     * @param retryStep   lo step di retry corrente
     * @param timeElapsed il tempo passato dall'ultima retry
     */
    @ParameterizedTest
    @MethodSource({"gestioneRetryKoArgsProvider"})
    void gestioneRetryCartaceo_Retry_PutRequestKo(BigDecimal retryStep, long timeElapsed) {

        RequestDto requestDto = buildRequestDto();
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = createCartaceoPresaInCaricoInfo();

        if (retryStep != null) {
            requestDto.getRequestMetadata().getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(timeElapsed));
            requestDto.getRequestMetadata().getRetry().setRetryStep(retryStep);
        } else requestDto.getRequestMetadata().setRetry(null);

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();

        // Mock di una generica getRichiesta.
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));

        // Mock di una generica patchRichiesta.
        when(gestoreRepositoryCall.patchRichiesta(eq(clientId), eq(requestId), any(PatchDto.class))).thenReturn(Mono.just(requestDto));

        // Mock di una generica putRequest che va in errore.
        when(paperMessageCall.putRequest(any(it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest.class))).thenReturn(Mono.error(new RuntimeException("KO")));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(cartaceoSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));
        doReturn(Mono.just(ChangeMessageVisibilityResponse.builder().build())).when(sqsService).changeMessageVisibility(any(), any(), any());

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

        RequestDto requestDto = buildRequestDto();
        CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo = createCartaceoPresaInCaricoInfo();

        requestDto.getRequestMetadata().getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(45));
        requestDto.getRequestMetadata().getRetry().setRetryStep(BigDecimal.valueOf(2));

        String requestId = requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();

        // Mock di una generica getRichiesta.
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));

        // Mock di una generica patchRichiesta.
        when(gestoreRepositoryCall.patchRichiesta(eq(clientId), eq(requestId), any(PatchDto.class))).thenReturn(Mono.just(requestDto));

        // Mock di una generica putRequest che va in errore.
        when(paperMessageCall.putRequest(any(it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest.class))).thenReturn(Mono.error(new RuntimeException("KO")));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(cartaceoSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));
        doReturn(Mono.just(ChangeMessageVisibilityResponse.builder().build())).when(sqsService).changeMessageVisibility(any(), any(), any());

        Mono<SqsResponse> response = cartaceoService.gestioneRetryCartaceo(cartaceoPresaInCaricoInfo, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(cartaceoPresaInCaricoInfo), eq(ERROR.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
        verify(cartaceoService, times(1)).sendNotificationOnDlqErrorQueue(cartaceoPresaInCaricoInfo);
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(message);
    }

}