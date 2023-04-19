package it.pagopa.pn.ec.cartaceo.service;

import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.sqs.SqsMessageWrapper;
import it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sms.service.SmsService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@SpringBootTestWebEnv
class CartaceoRetryTest {

    @Autowired
    private CartaceoSqsQueueName cartaceoSqsQueueName;

    @Autowired
    SmsService smsService;

    @Autowired
    CartaceoService cartaceoService;

    @MockBean
    GestoreRepositoryCall gestoreRepositoryCall;

    @MockBean
    PaperMessageCall paperMessageCall;

    @MockBean
    SqsService sqsService;
    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;


    Message message = Message.builder().build();


    private static final CartaceoPresaInCaricoInfo CARTACEO_PRESA_IN_CARICO_INFO = CartaceoPresaInCaricoInfo.builder()
            .requestIdx("idTest")
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .paperEngageRequest(new PaperEngageRequest())
            .build();

    private final SqsMessageWrapper<CartaceoPresaInCaricoInfo> sqsPresaInCaricoInfo = new SqsMessageWrapper<>(message, CARTACEO_PRESA_IN_CARICO_INFO);

    @Test
    void gestioneRetryCartaceoScheduler() {
        SqsServiceImpl mockSqsService = mock(SqsServiceImpl.class);
        when(mockSqsService.getOneMessage(eq(cartaceoSqsQueueName.errorName()), eq(CartaceoPresaInCaricoInfo.class)))
                .thenReturn(Mono.empty());

        // chiamare il metodo sotto test
        cartaceoService.gestioneRetryCartaceoScheduler();

        // verificare che non sia stata eseguita alcuna operazione sul mock SQSService
        verify(mockSqsService, never()).deleteMessageFromQueue(eq(message), anyString());

    }

    private static RequestDto buildRequestDto()
    {
        //RetryDTO
        RetryDto retryDto=new RetryDto();
        List<BigDecimal> retries = new ArrayList<>();
        retries.add(0, BigDecimal.valueOf(5));
        retries.add(1, BigDecimal.valueOf(10));
        retryDto.setRetryPolicy(retries);
        retryDto.setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(7));
        retryDto.setRetryStep(BigDecimal.valueOf(0));

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

        RequestDto requestDto= buildRequestDto();

        String requestId=requestDto.getRequestIdx();
        String clientId = requestDto.getxPagopaExtchCxId();

        // Mock di una generica getRichiesta.
        when(gestoreRepositoryCall.getRichiesta(eq(clientId), eq(requestId))).thenReturn(Mono.just(requestDto));

        // Mock di una generica patchRichiesta.
        when(gestoreRepositoryCall.patchRichiesta(eq(clientId), eq(requestId), any(PatchDto.class))).thenReturn(Mono.just(requestDto));

        // Mock di una generica putRequest.
        when(paperMessageCall.putRequest(any(org.openapitools.client.model.PaperEngageRequest.class))).thenReturn(Mono.just(new org.openapitools.client.model.OperationResultCodeResponse().resultCode("200.00")));

        // Mock della pubblicazione di una generica notifica sulla coda dello stato cartaceo.
        when(sqsService.send(eq(notificationTrackerSqsName.statoCartaceoName()), any(NotificationTrackerQueueDto.class))).thenReturn(Mono.just(SendMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = cartaceoService.gestioneRetryCartaceo(CARTACEO_PRESA_IN_CARICO_INFO, message);
        StepVerifier.create(response).expectNext();
        response.block();


        verify(gestoreRepositoryCall, times(1)).getRichiesta(eq(clientId), eq(requestId));
        verify(gestoreRepositoryCall, times(1)).patchRichiesta(eq(clientId), eq(requestId), any(PatchDto.class));
        verify(paperMessageCall, times(1)).putRequest(any(org.openapitools.client.model.PaperEngageRequest.class));
        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoCartaceoName()), any(NotificationTrackerQueueDto.class));

    }

}