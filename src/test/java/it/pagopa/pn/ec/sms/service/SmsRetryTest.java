package it.pagopa.pn.ec.sms.service;

import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.SnsService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pn.ec.util.LogSanitizer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static it.pagopa.pn.ec.commons.constant.Status.DELETED;
import static it.pagopa.pn.ec.commons.constant.Status.ERROR;
import static it.pagopa.pn.ec.commons.constant.Status.INTERNAL_ERROR;
import static it.pagopa.pn.ec.commons.constant.Status.SENT;
import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.NOTIFICATION_TRACKER_STEP;
import static it.pagopa.pn.ec.commons.utils.LogUtils.GESTIONE_RETRY_SMS;
import static it.pagopa.pn.ec.sms.testutils.DigitalCourtesySmsRequestFactory.createSmsRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class SmsRetryTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    private PnEcConfig.Sms.SqsQueue smsSqsQueueName() {
        return pnEcConfig.getSms().getSqsQueue();
    }

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
        when(mockSqsService.getOneMessage(smsSqsQueueName().getErrorName(), SmsPresaInCaricoInfo.class))
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
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(smsSqsQueueName().getErrorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));


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
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(smsSqsQueueName().getErrorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

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
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(smsSqsQueueName().getErrorName())))
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
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(smsSqsQueueName().getErrorName())))
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

    @AfterEach
    void resetIdSaved() {
        ReflectionTestUtils.setField(smsService, "idSaved", null);
    }

    private static SmsPresaInCaricoInfo newNotificationTrackerStepInfo() {
        return SmsPresaInCaricoInfo.builder()
                .requestIdx("idTest")
                .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
                .digitalCourtesySmsRequest(createSmsRequest())
                .stepError(StepError.builder()
                        .step(NOTIFICATION_TRACKER_STEP)
                        .generatedMessageDto(new GeneratedMessageDto().id("messageId").system("system"))
                        .build())
                .build();
    }

    private static RequestDto buildExhaustedRequestDto() {
        return buildRequestDtoWithRetryStep(buildRequestDto().getRequestMetadata().getRetry().getRetryPolicy().size() + 1L);
    }

    @Test
    void gestioneRetrySms_NotificationTrackerStepRetriesExhausted_errorEventHasSanitizedErrorDetails() {

        var info = newNotificationTrackerStepInfo();
        var requestDto = buildRequestDto();
        var clientId = requestDto.getxPagopaExtchCxId();
        var requestId = requestDto.getRequestIdx();

        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        doReturn(Mono.just(buildExhaustedRequestDto())).when(gestoreRepositoryCall).patchRichiesta(eq(clientId), eq(requestId), any(PatchDto.class));
        doReturn(Mono.error(new SqsClientException("queue for +393331234567"))).when(smsService)
                .sendNotificationOnStatusQueue(any(), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(smsSqsQueueName().getErrorName())))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        StepVerifier.create(smsService.gestioneRetrySms(info, message)).expectNextCount(1).verifyComplete();

        ArgumentCaptor<DigitalProgressStatusDto> captor = ArgumentCaptor.forClass(DigitalProgressStatusDto.class);
        verify(smsService, times(1)).sendNotificationOnStatusQueue(eq(info), eq(ERROR.getStatusTransactionTableCompliant()), captor.capture());
        String errorDetails = captor.getValue().getErrorDetails();
        assertNotNull(errorDetails);
        assertTrue(errorDetails.contains(GESTIONE_RETRY_SMS));
        assertTrue(errorDetails.contains("An error occurred during client operation"));
        assertEquals(2, StringUtils.countMatches(errorDetails, LogSanitizer.DEFAULT_PHONE));
        assertFalse(errorDetails.contains("+39 000 +39"));
        assertFalse(errorDetails.contains("3331234567"));
        assertFalse(errorDetails.contains("\tat "));
    }

    @Test
    void gestioneRetrySms_SnsRetriesExhausted_errorEventContainsSnsCause() {

        var info = newSmsInfo();
        var requestDto = buildRequestDto();
        var clientId = requestDto.getxPagopaExtchCxId();
        var requestId = requestDto.getRequestIdx();

        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        doReturn(Mono.just(buildExhaustedRequestDto())).when(gestoreRepositoryCall).patchRichiesta(eq(clientId), eq(requestId), any(PatchDto.class));
        doReturn(Mono.error(new SnsSendException("sns provider failure"))).when(snsService).send(anyString(), anyString());
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(smsSqsQueueName().getErrorName())))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        StepVerifier.create(smsService.gestioneRetrySms(info, message)).expectNextCount(1).verifyComplete();

        ArgumentCaptor<DigitalProgressStatusDto> captor = ArgumentCaptor.forClass(DigitalProgressStatusDto.class);
        verify(smsService, times(1)).sendNotificationOnStatusQueue(eq(info), eq(ERROR.getStatusTransactionTableCompliant()), captor.capture());
        String errorDetails = captor.getValue().getErrorDetails();
        assertNotNull(errorDetails);
        assertTrue(errorDetails.contains(GESTIONE_RETRY_SMS));
        assertTrue(errorDetails.contains("sns provider failure"));
    }

    @Test
    void gestioneRetrySms_GenericError_internalErrorEventHasErrorDetails() {

        var info = newSmsInfo();
        var requestDto = buildRequestDto();

        when(gestoreRepositoryCall.getRichiesta(requestDto.getxPagopaExtchCxId(), requestDto.getRequestIdx()))
                .thenReturn(Mono.error(new RuntimeException("repository failure for mario.rossi@example.com")));
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(smsSqsQueueName().getErrorName())))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        StepVerifier.create(smsService.gestioneRetrySms(info, message)).expectNextCount(1).verifyComplete();

        ArgumentCaptor<DigitalProgressStatusDto> captor = ArgumentCaptor.forClass(DigitalProgressStatusDto.class);
        verify(smsService, times(1)).sendNotificationOnStatusQueue(eq(info), eq(INTERNAL_ERROR.getStatusTransactionTableCompliant()), captor.capture());
        String errorDetails = captor.getValue().getErrorDetails();
        assertNotNull(errorDetails);
        assertTrue(errorDetails.contains(GESTIONE_RETRY_SMS));
        assertTrue(errorDetails.contains("repository failure"));
        assertTrue(errorDetails.contains("xxx@example.com"));
        assertFalse(errorDetails.contains("mario.rossi"));
    }

    @Test
    void gestioneRetrySms_StatusToDelete_deletedEventHasNoErrorDetails() {

        var requestDto = buildToDeleteRequestDto();
        when(gestoreRepositoryCall.getRichiesta(requestDto.getxPagopaExtchCxId(), requestDto.getRequestIdx())).thenReturn(Mono.just(requestDto));
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(smsSqsQueueName().getErrorName())))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        StepVerifier.create(smsService.gestioneRetrySms(SMS_PRESA_IN_CARICO_INFO, message)).expectNextCount(1).verifyComplete();

        ArgumentCaptor<DigitalProgressStatusDto> captor = ArgumentCaptor.forClass(DigitalProgressStatusDto.class);
        verify(smsService, times(1)).sendNotificationOnStatusQueue(eq(SMS_PRESA_IN_CARICO_INFO), eq(DELETED.getStatusTransactionTableCompliant()), captor.capture());
        assertNull(captor.getValue().getErrorDetails());
    }

    @Test
    void gestioneRetrySms_RetriesExhausted_errorPayloadWithErrorDetailsIsContractValid() {

        var info = newNotificationTrackerStepInfo();
        var requestDto = buildRequestDto();
        var clientId = requestDto.getxPagopaExtchCxId();
        var requestId = requestDto.getRequestIdx();

        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        doReturn(Mono.just(buildExhaustedRequestDto())).when(gestoreRepositoryCall).patchRichiesta(eq(clientId), eq(requestId), any(PatchDto.class));
        doReturn(Mono.error(new SqsClientException("queue"))).when(smsService)
                .sendNotificationOnStatusQueue(any(), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(smsSqsQueueName().getErrorName())))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        StepVerifier.create(smsService.gestioneRetrySms(info, message)).expectNextCount(1).verifyComplete();

        ArgumentCaptor<DigitalProgressStatusDto> captor = ArgumentCaptor.forClass(DigitalProgressStatusDto.class);
        verify(smsService, times(1)).sendNotificationOnStatusQueue(eq(info), eq(ERROR.getStatusTransactionTableCompliant()), captor.capture());
        assertNotNull(captor.getValue().getErrorDetails());
        assertTrue(captor.getValue().getErrorDetails().length() <= 1000);
        assertPatchPayloadIsContractValid(captor.getValue(), ERROR.getStatusTransactionTableCompliant());
    }

    @Test
    void patchPayload_errorDetailsOfMaxLength_isContractValid() {
        DigitalProgressStatusDto dto = new DigitalProgressStatusDto().errorDetails("e".repeat(1000));

        assertPatchPayloadIsContractValid(dto, ERROR.getStatusTransactionTableCompliant());
    }

    private static SmsPresaInCaricoInfo newSmsInfo() {
        return SmsPresaInCaricoInfo.builder()
                .requestIdx("idTest")
                .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
                .digitalCourtesySmsRequest(createSmsRequest())
                .build();
    }

    private static RequestDto buildRequestDtoWithRetryStep(long retryStep) {
        var requestDto = buildRequestDto();
        requestDto.getRequestMetadata().getRetry().setRetryStep(BigDecimal.valueOf(retryStep));
        return requestDto;
    }

    @Test
    void checkTentativiEccessiviSms_retryStepOverPolicySize_errorEventHasSuppliedErrorDetails() {
        var info = newSmsInfo();
        var requestDto = buildExhaustedRequestDto();
        String expected = "Exception in 'gestioneRetrySms()' - java.lang.RuntimeException: queue failure, queue failure";
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(smsSqsQueueName().getErrorName())))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        StepVerifier.create(smsService.checkTentativiEccessiviSms(info.getRequestIdx(), requestDto, info, message, () -> expected))
                    .expectNextCount(1)
                    .verifyComplete();

        ArgumentCaptor<DigitalProgressStatusDto> captor = ArgumentCaptor.forClass(DigitalProgressStatusDto.class);
        verify(smsService, times(1)).sendNotificationOnStatusQueue(eq(info), eq(ERROR.getStatusTransactionTableCompliant()), captor.capture());
        assertEquals(expected, captor.getValue().getErrorDetails());
    }

    @Test
    void checkTentativiEccessiviSms_retryStepOverPolicySize_supplierIsInvokedOnlyOnce() {
        var info = newSmsInfo();
        var requestDto = buildExhaustedRequestDto();
        AtomicInteger supplierCalls = new AtomicInteger();
        Supplier<String> supplier = () -> {
            supplierCalls.incrementAndGet();
            return "text";
        };
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(smsSqsQueueName().getErrorName())))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        StepVerifier.create(smsService.checkTentativiEccessiviSms(info.getRequestIdx(), requestDto, info, message, supplier))
                    .expectNextCount(1)
                    .verifyComplete();

        assertEquals(1, supplierCalls.get());
    }

    @Test
    void checkTentativiEccessiviSms_retryStepEqualToPolicySize_documentsThatErrorIsNotPublishedAndTextIsNotBuilt() {
        var info = newSmsInfo();
        var requestDto = buildRequestDtoWithRetryStep(buildRequestDto().getRequestMetadata().getRetry().getRetryPolicy().size());
        AtomicInteger supplierCalls = new AtomicInteger();
        Supplier<String> supplier = () -> {
            supplierCalls.incrementAndGet();
            return "text";
        };

        StepVerifier.create(smsService.checkTentativiEccessiviSms(info.getRequestIdx(), requestDto, info, message, supplier))
                    .verifyComplete();

        verify(smsService, never()).sendNotificationOnStatusQueue(eq(info), eq(ERROR.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        assertEquals(0, supplierCalls.get());
    }

}
