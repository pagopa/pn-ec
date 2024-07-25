package it.pagopa.pn.ec.cartaceo.service;

import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.cartaceo.testutils.PaperEngageRequestFactory;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
import it.pagopa.pn.ec.commons.model.pojo.sqs.SqsMessageWrapper;
import it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.pdfraster.PdfRasterCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.pdfraster.model.dto.PdfRasterResponse;
import it.pagopa.pn.ec.pdfraster.model.dto.RequestConversionDto;
import it.pagopa.pn.ec.pdfraster.service.DynamoPdfRasterService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sms.service.SmsService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.commons.constant.Status.SENT;
import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.NOTIFICATION_TRACKER_STEP;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.OK_CODE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class CartaceoRetryTest {

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
    SqsService sqsService;

    @SpyBean
    private DynamoPdfRasterService dynamoPdfRasterService;

    @MockBean
    private PdfRasterCall pdfRasterCall;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;


    Message message = Message.builder().build();


    private static final CartaceoPresaInCaricoInfo CARTACEO_PRESA_IN_CARICO_INFO = CartaceoPresaInCaricoInfo.builder()
            .requestIdx("idTest")
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .paperEngageRequest(new PaperEngageRequest())
            .build();

    private static final CartaceoPresaInCaricoInfo CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER = CartaceoPresaInCaricoInfo.builder().requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
            .paperEngageRequest(PaperEngageRequestFactory.createDtoPaperRequestPdfRaster(2)).build();


    private static final StepError STEP_ERROR = StepError.builder()
            .step(NOTIFICATION_TRACKER_STEP)
            .operationResultCodeResponse(new OperationResultCodeResponse().resultCode(OK_CODE))
            .build();

    private static final CartaceoPresaInCaricoInfo CARTACEO_PRESA_IN_CARICO_INFO_STEP_ERROR = CartaceoPresaInCaricoInfo.builder()
            .requestIdx("idTest")
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .stepError(STEP_ERROR)
            .paperEngageRequest(new PaperEngageRequest())
            .build();

    private final SqsMessageWrapper<CartaceoPresaInCaricoInfo> sqsPresaInCaricoInfo = new SqsMessageWrapper<>(message, CARTACEO_PRESA_IN_CARICO_INFO);

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

    private static RequestDto buildRequestDto(int valueRetry)
    {
        //RetryDTO
        RetryDto retryDto=new RetryDto();
        List<BigDecimal> retries = new ArrayList<>();
        retries.add(0, BigDecimal.valueOf(5));
        retries.add(1, BigDecimal.valueOf(10));
        retryDto.setRetryPolicy(retries);
        retryDto.setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(7));
        retryDto.setRetryStep(BigDecimal.valueOf(valueRetry));

        //RequestMetadataDTO
        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.setRetry(retryDto);

        //RequestDTO
        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(CARTACEO_PRESA_IN_CARICO_INFO.getRequestIdx());
        requestDto.setxPagopaExtchCxId(CARTACEO_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId());
        requestDto.setRequestMetadata(requestMetadata);

        return requestDto;
    }

    @Test
    void gestioneRetryCartaceo_RetryOk() {

        RequestDto requestDto= buildRequestDto(0);

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();

        // Mock di una generica getRichiesta.
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));

        // Mock di una generica patchRichiesta.
        when(gestoreRepositoryCall.patchRichiesta(eq(clientId), eq(requestId), any(PatchDto.class))).thenReturn(Mono.just(requestDto));

        // Mock di una generica putRequest.
        when(paperMessageCall.putRequest(any(it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest.class))).thenReturn(Mono.just(new OperationResultCodeResponse().resultCode(OK_CODE)));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(cartaceoSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = cartaceoService.gestioneRetryCartaceo(CARTACEO_PRESA_IN_CARICO_INFO, message);
//        Mono<DeleteMessageResponse> response = cartaceoService.gestioneRetryCartaceo(CARTACEO_PRESA_IN_CARICO_INFO_STEP_ERROR, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO),eq(SENT.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
//        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO_STEP_ERROR),eq(SENT.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(any(Message.class));
    }

    @Test
    void gestioneRetryCartaceoPdfRaster_RetryOk() {

        RequestDto requestDto= buildRequestDto(3);

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();

        // Mock di una generica getRichiesta.
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));

        // Mock di una generica patchRichiesta.
        when(gestoreRepositoryCall.patchRichiesta(eq(clientId), eq(requestId), any(PatchDto.class))).thenReturn(Mono.just(requestDto));

        // Mock della chiamata a pdf raster
        when(pdfRasterCall.convertPdf(any())).thenReturn(Mono.just(PdfRasterResponse.builder().newFileKey("").build()));

        Mono<DeleteMessageResponse> response = cartaceoService.gestioneRetryCartaceo(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER, message);
//        Mono<DeleteMessageResponse> response = cartaceoService.gestioneRetryCartaceo(CARTACEO_PRESA_IN_CARICO_INFO_STEP_ERROR, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER),eq(SENT.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
//        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO_STEP_ERROR),eq(SENT.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(any(Message.class));

        verify(dynamoPdfRasterService, times(1)).insertRequestConversion(any(RequestConversionDto.class));
    }
    
    @Test
    void gestioneRetryCartaceoPdfRaster_RetryKoOverStep() {

        RequestDto requestDto= buildRequestDto(4);

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();

        // Mock di una generica getRichiesta.
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));

        // Mock di una generica patchRichiesta.
        when(gestoreRepositoryCall.patchRichiesta(eq(clientId), eq(requestId), any(PatchDto.class))).thenReturn(Mono.just(requestDto));

        Mono<DeleteMessageResponse> response = cartaceoService.gestioneRetryCartaceo(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER, message);
//        Mono<DeleteMessageResponse> response = cartaceoService.gestioneRetryCartaceo(CARTACEO_PRESA_IN_CARICO_INFO_STEP_ERROR, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER),eq(SENT.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
//        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO_STEP_ERROR),eq(SENT.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(any(Message.class));

        verify(dynamoPdfRasterService, times(1)).insertRequestConversion(any(RequestConversionDto.class));
    }
    
    @Test
    void gestioneRetryCartaceoPdfRaster_RetryKo() {

        RequestDto requestDto= buildRequestDto(2);

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();

        // Mock di una generica getRichiesta.
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));

        // Mock di una generica patchRichiesta.
        when(gestoreRepositoryCall.patchRichiesta(eq(clientId), eq(requestId), any(PatchDto.class))).thenReturn(Mono.just(requestDto));

        // Mock della chiamata a pdf raster
        when(pdfRasterCall.convertPdf(any())).thenReturn(Mono.just(PdfRasterResponse.builder().newFileKey("").build()));

        Mono<DeleteMessageResponse> response = cartaceoService.gestioneRetryCartaceo(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER, message);
//        Mono<DeleteMessageResponse> response = cartaceoService.gestioneRetryCartaceo(CARTACEO_PRESA_IN_CARICO_INFO_STEP_ERROR, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO_PDFRASTER),eq(SENT.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
//        verify(cartaceoService, times(1)).sendNotificationOnStatusQueue(eq(CARTACEO_PRESA_IN_CARICO_INFO_STEP_ERROR),eq(SENT.getStatusTransactionTableCompliant()), any(PaperProgressStatusDto.class));
        verify(cartaceoService, times(1)).deleteMessageFromErrorQueue(any(Message.class));

        verify(dynamoPdfRasterService, times(1)).insertRequestConversion(any(RequestConversionDto.class));
    }

}