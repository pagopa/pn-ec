package it.pagopa.pn.ec.sms.service;

import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.SnsService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sms.configurationproperties.SmsSqsQueueName;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static it.pagopa.pn.ec.commons.constant.Status.DELETED;
import static it.pagopa.pn.ec.commons.constant.Status.INTERNAL_ERROR;
import static it.pagopa.pn.ec.commons.constant.Status.SENT;
import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.NOTIFICATION_TRACKER_STEP;
import static it.pagopa.pn.ec.sms.testutils.DigitalCourtesySmsRequestFactory.createSmsRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class SmsRetryTest {

    @Autowired
    private SmsSqsQueueName smsSqsQueueName;

    @MockitoSpyBean
    SmsService smsService;

    @MockitoSpyBean
    GestoreRepositoryCall gestoreRepositoryCall;

    @MockitoSpyBean
    SnsService snsService;

    @MockitoSpyBean
    SqsService sqsService;

    Message message = Message.builder().build();

    private static final SmsPresaInCaricoInfo SMS_PRESA_IN_CARICO_INFO = SmsPresaInCaricoInfo.builder()
            .requestIdx("idTest")
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalCourtesySmsRequest(createSmsRequest())
            .build();

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
        requestDto.setRequestIdx(SMS_PRESA_IN_CARICO_INFO.getRequestIdx());
        requestDto.setxPagopaExtchCxId(SMS_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId());
        requestDto.setRequestMetadata(requestMetadata);

        return requestDto;
    }
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

    @Test
    void gestioneRetrySms_Retry_Ok() {

        var requestDto=buildRequestDto();

        var clientId = requestDto.getxPagopaExtchCxId();
        var requestId = requestDto.getRequestIdx();

        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());

        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(smsSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));


        Mono<DeleteMessageResponse> response =  smsService.gestioneRetrySms(SMS_PRESA_IN_CARICO_INFO, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(smsService, times(1)).sendNotificationOnStatusQueue(eq(SMS_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    @Test
    void gestioneRetrySms_GenericError() {

        var requestDto=buildRequestDto();

        var clientId = requestDto.getxPagopaExtchCxId();
        var requestId = requestDto.getRequestIdx();

        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.error(new RuntimeException()));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(smsSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response =  smsService.gestioneRetrySms(SMS_PRESA_IN_CARICO_INFO, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(smsService, times(1)).sendNotificationOnStatusQueue(eq(SMS_PRESA_IN_CARICO_INFO), eq(INTERNAL_ERROR.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    private static RequestDto buildToDeleteRequestDto() {
        RequestDto requestDto = buildRequestDto();
        requestDto.setStatusRequest("toDelete");
        return requestDto;
    }

    private static void assertPatchPayloadIsContractValid(DigitalProgressStatusDto digitalProgressStatusDto, String status) {
        digitalProgressStatusDto.setEventTimestamp(OffsetDateTime.now());
        digitalProgressStatusDto.setStatus(status);

        PatchDto patchDto = new PatchDto().event(new EventsDto().digProgrStatus(digitalProgressStatusDto));

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<PatchDto>> violations = validator.validate(patchDto);

            assertTrue(violations.isEmpty(),
                    () -> "Il payload dello stato '" + status + "' viola il contratto OpenAPI (causa del 400 -> DLQ): "
                            + violations.stream()
                            .map(v -> v.getPropertyPath() + " " + v.getMessage())
                            .toList());
        }
    }


    @Test
    void gestioneRetrySms_StatusToDelete_DeletedPayloadIsContractValid() {

        var requestDto = buildToDeleteRequestDto();

        var clientId = requestDto.getxPagopaExtchCxId();
        var requestId = requestDto.getRequestIdx();

        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(smsSqsQueueName.errorName())))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = smsService.gestioneRetrySms(SMS_PRESA_IN_CARICO_INFO, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        ArgumentCaptor<DigitalProgressStatusDto> captor = ArgumentCaptor.forClass(DigitalProgressStatusDto.class);
        verify(smsService, times(1)).sendNotificationOnStatusQueue(
                eq(SMS_PRESA_IN_CARICO_INFO),
                eq(DELETED.getStatusTransactionTableCompliant()),
                captor.capture());

        assertPatchPayloadIsContractValid(captor.getValue(), DELETED.getStatusTransactionTableCompliant());
    }


    @Test
    void gestioneRetrySms_RetriesExhausted_TerminalPayloadIsContractValid() {

        var requestDto = buildRequestDto();
        var retry = requestDto.getRequestMetadata().getRetry();
        retry.setRetryStep(BigDecimal.valueOf(retry.getRetryPolicy().size()));

        var clientId = requestDto.getxPagopaExtchCxId();
        var requestId = requestDto.getRequestIdx();

        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(smsSqsQueueName.errorName())))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = smsService.gestioneRetrySms(SMS_PRESA_IN_CARICO_INFO, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<DigitalProgressStatusDto> payloadCaptor = ArgumentCaptor.forClass(DigitalProgressStatusDto.class);
        verify(smsService, times(1)).sendNotificationOnStatusQueue(
                eq(SMS_PRESA_IN_CARICO_INFO),
                statusCaptor.capture(),
                payloadCaptor.capture());

        assertPatchPayloadIsContractValid(payloadCaptor.getValue(), statusCaptor.getValue());
    }

}