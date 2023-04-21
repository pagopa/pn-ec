package it.pagopa.pn.ec.sms.service;

import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
import it.pagopa.pn.ec.commons.model.pojo.sqs.SqsMessageWrapper;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sms.configurationproperties.SmsSqsQueueName;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.NOTIFICATION_TRACKER_STEP;
import static it.pagopa.pn.ec.sms.testutils.DigitalCourtesySmsRequestFactory.createSmsRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class SmsRetryTest {

    @Autowired
    private SmsSqsQueueName smsSqsQueueName;

    @Autowired
    SmsService smsService;

    @Autowired
    SmsService smsServiceMock;

    @SpyBean
    GestoreRepositoryCall gestoreRepositoryCall;

    Message message = Message.builder().build();

    private static final SmsPresaInCaricoInfo SMS_PRESA_IN_CARICO_INFO = SmsPresaInCaricoInfo.builder()
            .requestIdx("idTest")
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalCourtesySmsRequest(createSmsRequest())
            .build();

    private static final StepError STEP_ERROR = StepError.builder()
            .generatedMessageDto(new GeneratedMessageDto().id("1221313223"))
            .notificationTrackerError(NOTIFICATION_TRACKER_STEP)
            .build();
    private static final SmsPresaInCaricoInfo SMS_PRESA_IN_CARICO_INFO1 = SmsPresaInCaricoInfo.builder()
            .requestIdx("idTest")
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .stepError(STEP_ERROR)
            .digitalCourtesySmsRequest(createSmsRequest())
            .build();

    private final SqsMessageWrapper<SmsPresaInCaricoInfo> sqsPresaInCaricoInfo = new SqsMessageWrapper<>(message, SMS_PRESA_IN_CARICO_INFO);

    @Test
    void testGestioneRetrySmsScheduler_NoMessages() {
        // mock SQSService per restituire un Mono vuoto quando viene chiamato getOneMessage
        SqsServiceImpl mockSqsService = mock(SqsServiceImpl.class);
        when(mockSqsService.getOneMessage(smsSqsQueueName.errorName(), SmsPresaInCaricoInfo.class))
                .thenReturn(Mono.empty());

        // chiamare il metodo sotto test
        smsService.gestioneRetrySmsScheduler();

        // verificare che non sia stata eseguita alcuna operazione sul mock SQSService
        verify(mockSqsService, never()).deleteMessageFromQueue(eq(message), anyString());
    }

    /*@Test
    void gestioneRetrySms_RetryOk() {

        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx("idTest");


        when(gestoreRepositoryCall.getRichiesta(eq(requestDto.getRequestIdx()))).thenReturn(Mono.just(requestDto));
        when(snsServiceMock.send(eq(SMS_PRESA_IN_CARICO_INFO.getDigitalCourtesySmsRequest().getReceiverDigitalAddress()),
                eq(SMS_PRESA_IN_CARICO_INFO.getDigitalCourtesySmsRequest().getMessageText())))
                .thenReturn(Mono.just(PublishResponse.builder().build()));

        gestoreRepositoryCall.getRichiesta(requestDto.getRequestIdx());
        smsService.gestioneRetrySms(SMS_PRESA_IN_CARICO_INFO, message);
        snsService.send(SMS_PRESA_IN_CARICO_INFO.getDigitalCourtesySmsRequest().getReceiverDigitalAddress(), SMS_PRESA_IN_CARICO_INFO.getDigitalCourtesySmsRequest().getMessageText());

        verify(gestoreRepositoryCall, times(1)).getRichiesta(requestDto.getRequestIdx());
        verify(snsServiceMock, times(1)).send(SMS_PRESA_IN_CARICO_INFO.getDigitalCourtesySmsRequest().getReceiverDigitalAddress(), SMS_PRESA_IN_CARICO_INFO.getDigitalCourtesySmsRequest().getMessageText());

    }*/


    @Test
    void gestionreRetrySms_GenericError(){

        String requestId = "idTest";
        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(requestId);
        String clientId = DEFAULT_ID_CLIENT_HEADER_VALUE;
        requestDto.setxPagopaExtchCxId(clientId);
        PatchDto patchDto = new PatchDto();

        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.setRetry(new RetryDto());
        requestMetadata.getRetry().setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(7));
        requestDto.setRequestMetadata(requestMetadata);
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        //when(gestoreRepositoryCall.patchRichiesta(eq(requestId), eq(patchDto)).thenReturn(Mono.just(requestDto)));


//        DeleteMessageResponse response =
                smsService.gestioneRetrySms(SMS_PRESA_IN_CARICO_INFO, message).block();

//        Assertions.assertNull(response);
    }

    // se l'evento si era interrotto durante la lavorazione nella chiamata a SNS, decommentare riga "smsService.gestioneRetrySms(SMS_PRESA_IN_CARICO_INFO, message).block();"
    // se l'evento si era interrotto durante la lavorazione nella chiamata a SQS, decommentare riga "smsService.gestioneRetrySms(SMS_PRESA_IN_CARICO_INFO1, message).block();"
    @Test
    void gestionreRetrySms_Retry_Ok(){

        String requestId = "idTest";
        List<BigDecimal> retries = new ArrayList<>();
        retries.add(0, BigDecimal.valueOf(5));
        retries.add(1, BigDecimal.valueOf(10));
        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(requestId);
        Mono<RequestDto> monoRequest = new Mono<RequestDto>() {
            @Override
            public void subscribe(CoreSubscriber<? super RequestDto> actual) {

            }
        };
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


        smsService.gestioneRetrySms(SMS_PRESA_IN_CARICO_INFO, message).block();
//        smsService.gestioneRetrySms(SMS_PRESA_IN_CARICO_INFO1, message).block();

    }

}