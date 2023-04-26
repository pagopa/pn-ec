package it.pagopa.pn.ec.cartaceo.service;

import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
import it.pagopa.pn.ec.commons.model.pojo.sqs.SqsMessageWrapper;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sms.service.SmsService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.NOTIFICATION_TRACKER_STEP;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class CartaceoRetryTest {

    @Autowired
    private CartaceoSqsQueueName cartaceoSqsQueueName;

    @Autowired
    SmsService smsService;

    @Autowired
    CartaceoService cartaceoService;

    @SpyBean
    GestoreRepositoryCall gestoreRepositoryCall;

    Message message = Message.builder().build();


    private static final CartaceoPresaInCaricoInfo CARTACEO_PRESA_IN_CARICO_INFO = CartaceoPresaInCaricoInfo.builder()
            .requestIdx("idTest")
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .paperEngageRequest(new PaperEngageRequest())
            .build();

    private static final StepError STEP_ERROR = StepError.builder()
            .notificationTrackerError(NOTIFICATION_TRACKER_STEP)
            .operationResultCodeResponse(new OperationResultCodeResponse().resultCode("C00"))
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

    @Test
    void gestioneRetryCartaceo_RetryOk() {

        String requestId = "idTest";
        List<BigDecimal> retries = new ArrayList<>();
        retries.add(0, BigDecimal.valueOf(5));
        retries.add(1, BigDecimal.valueOf(10));
        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(requestId);
        String clientId = DEFAULT_ID_CLIENT_HEADER_VALUE;
        requestDto.setxPagopaExtchCxId(clientId);

        PatchDto patchDto = new PatchDto();
        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.setRetry(new RetryDto());
        requestMetadata.getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(7));
        requestMetadata.getRetry().setRetryStep(BigDecimal.valueOf(0));
        requestDto.setRequestMetadata(requestMetadata);
        requestDto.getRequestMetadata().getRetry().setRetryPolicy(retries);
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));


//        Mono<DeleteMessageResponse> response =
//                cartaceoService.gestioneRetryCartaceo(CARTACEO_PRESA_IN_CARICO_INFO, message).block();
                cartaceoService.gestioneRetryCartaceo(CARTACEO_PRESA_IN_CARICO_INFO_STEP_ERROR, message).block();

//        Assert.assertNotNull(response);
    }
}