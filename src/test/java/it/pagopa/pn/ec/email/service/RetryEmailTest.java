package it.pagopa.pn.ec.email.service;

import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.exception.ses.SesSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
import it.pagopa.pn.ec.commons.model.pojo.sqs.SqsMessageWrapper;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.SesService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.ByteArrayOutputStream;
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
import static it.pagopa.pn.ec.commons.utils.LogUtils.GESTIONE_RETRY_EMAIL;
import static it.pagopa.pn.ec.commons.utils.LogUtils.PROCESS_ONLY_BODY_RETRY;
import static it.pagopa.pn.ec.commons.utils.LogUtils.PROCESS_WITH_ATTACH_RETRY;
import static it.pagopa.pn.ec.email.testutils.DigitalCourtesyMailRequestFactory.createMailRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RetryEmailTest {

    @MockitoSpyBean
    private EmailService emailService;
    @Autowired
    private PnEcConfig pnEcConfig;

    private PnEcConfig.Email.SqsQueue emailSqsQueueName() {
        return pnEcConfig.getEmail().getSqsQueue();
    }
    @MockitoSpyBean
    private SqsServiceImpl sqsService;
    @MockitoBean
    private FileCall fileCall;
    @MockitoSpyBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @MockitoBean
    private SesService sesService;
    @MockitoBean
    private DownloadCall downloadCall;
    private static final String QUEUE_NAME="queue";

    Message message = Message.builder().build();
    private static final EmailPresaInCaricoInfo EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH = EmailPresaInCaricoInfo.builder()
            .requestIdx(
                    DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalCourtesyMailRequest(
                    createMailRequest(1))
            .build();

    private static final EmailPresaInCaricoInfo EMAIL_PRESA_IN_CARICO_INFO = EmailPresaInCaricoInfo.builder()
            .requestIdx("idTest")
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalCourtesyMailRequest(createMailRequest(0))
            .build();

    private static final FileDownloadResponse FILE_DOWNLOAD_RESPONSE = new FileDownloadResponse()
            .download(new FileDownloadInfo()
                    .url("url"))
            .key("key")
            .checksum("checksum")
            .contentLength(BigDecimal.TEN)
            .contentType("contentType")
            .versionId("versionId")
            .documentStatus("documentStatus")
            .documentType("documentType")
            .retentionUntil(OffsetDateTime.parse("2023-04-18T05:08:27.101Z"));

    private static RequestDto buildRequestDto()
    {
        //RetryDto
        RetryDto retryDto=new RetryDto();
        List<BigDecimal> retries = new ArrayList<>();
        retries.add(0, BigDecimal.valueOf(5));
        retries.add(1, BigDecimal.valueOf(10));
        retryDto.setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(7));
        retryDto.setRetryStep(BigDecimal.valueOf(0));
        retryDto.setRetryPolicy(retries);

        //RequestMetadataDto
        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.setRetry(retryDto);

        //RequestDto
        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(EMAIL_PRESA_IN_CARICO_INFO.getRequestIdx());
        requestDto.setxPagopaExtchCxId(EMAIL_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId());
        requestDto.setRequestMetadata(requestMetadata);

        return requestDto;
    }
    private static RequestDto buildToDeleteRequestDto() {
        RequestDto requestDto = buildRequestDto();
        requestDto.setStatusRequest("toDelete");
        return requestDto;
    }


    @Test
    void gestioneRetryEmail_StatusToDelete_TerminalPayloadIsContractValid() {

        String clientId = EMAIL_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        String requestId = EMAIL_PRESA_IN_CARICO_INFO.getRequestIdx();

        var requestDto = buildToDeleteRequestDto();

        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(emailSqsQueueName().getErrorName())))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO, message, QUEUE_NAME);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<DigitalProgressStatusDto> payloadCaptor = ArgumentCaptor.forClass(DigitalProgressStatusDto.class);
        verify(emailService, times(1)).sendNotificationOnStatusQueue(
                eq(EMAIL_PRESA_IN_CARICO_INFO),
                statusCaptor.capture(),
                payloadCaptor.capture());

        DigitalProgressStatusDto sentDigitalProgressStatus = payloadCaptor.getValue();
        sentDigitalProgressStatus.setEventTimestamp(OffsetDateTime.now());
        sentDigitalProgressStatus.setStatus(statusCaptor.getValue());

        PatchDto patchDto = new PatchDto().event(new EventsDto().digProgrStatus(sentDigitalProgressStatus));

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<PatchDto>> violations = validator.validate(patchDto);

            assertTrue(violations.isEmpty(),
                    () -> "Il payload dello stato '" + statusCaptor.getValue()
                            + "' viola il contratto OpenAPI (causa del 400 -> DLQ): "
                            + violations.stream()
                            .map(v -> v.getPropertyPath() + " " + v.getMessage())
                            .toList());
        }
    }

    @Test
    void testGestioneRetryEmailScheduler_NoMessages() {
        // mock SQSService per restituire un Mono vuoto quando viene chiamato getOneMessage
        SqsServiceImpl mockSqsService = mock(SqsServiceImpl.class);
        when(mockSqsService.getOneMessage(emailSqsQueueName().getErrorName(), EmailPresaInCaricoInfo.class))
                .thenReturn(Mono.empty());

        // chiamare il metodo sotto test
        emailService.gestioneRetryEmailScheduler();

        // verificare che non sia stata eseguita alcuna operazione sul mock SQSService
        verify(mockSqsService, never()).deleteMessageFromQueue(eq(message), anyString());

    }
    @Test
    void gestionreRetryEmail_Retry_Ok(){

        String clientId = EMAIL_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        String requestId=EMAIL_PRESA_IN_CARICO_INFO.getRequestIdx();

        var requestDto=buildRequestDto();

        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(fileCall.getFile(any(), any(), eq(false))).thenReturn(Mono.just(FILE_DOWNLOAD_RESPONSE));
        when(sesService.send(any(EmailField.class))).thenReturn(Mono.just(SendRawEmailResponse.builder().messageId("messageId").build()));

        //Gestore repository mocks.
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(emailSqsQueueName().getErrorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));

        Mono<DeleteMessageResponse> response = emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO, message,QUEUE_NAME);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(emailService, times(1)).sendNotificationOnStatusQueue(eq(EMAIL_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    @Test
    void gestionreRetryEmail_Retry_Attach_NULL(){

        String clientId = EMAIL_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        String requestId=EMAIL_PRESA_IN_CARICO_INFO.getRequestIdx();

        var requestDto=buildRequestDto();

        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(fileCall.getFile(any(), any(), eq(false))).thenReturn(Mono.just(FILE_DOWNLOAD_RESPONSE));
        when(sesService.send(any(EmailField.class))).thenReturn(Mono.just(SendRawEmailResponse.builder().messageId("messageId").build()));

        //Gestore repository mocks.
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(emailSqsQueueName().getErrorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));

        EMAIL_PRESA_IN_CARICO_INFO.getDigitalCourtesyMailRequest().setAttachmentUrls(null);

        Mono<DeleteMessageResponse> response = emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO, message,QUEUE_NAME);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(emailService, times(1)).sendNotificationOnStatusQueue(eq(EMAIL_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    @Test
    void gestionreRetryEmail_GenericError() {

        String clientId = EMAIL_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        String requestId=EMAIL_PRESA_IN_CARICO_INFO.getRequestIdx();

        var requestDto=buildRequestDto();

        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(fileCall.getFile(any(), any(), eq(false))).thenReturn(Mono.just(FILE_DOWNLOAD_RESPONSE));
        when(sesService.send(any(EmailField.class))).thenReturn(Mono.just(SendRawEmailResponse.builder().messageId("messageId").build()));
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));

        //Gestore repository mocks.
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.error(new RuntimeException()));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(emailSqsQueueName().getErrorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));


        Mono<DeleteMessageResponse> response = emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO, message,QUEUE_NAME);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(emailService, times(1)).sendNotificationOnStatusQueue(eq(EMAIL_PRESA_IN_CARICO_INFO), eq(INTERNAL_ERROR.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

    }
    @Test
    void gestionreRetryEmailAttachment_Retry_Ok(){

        String clientId = EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH.getXPagopaExtchCxId();
        String requestId=EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH.getRequestIdx();

        var requestDto=buildRequestDto();
        requestDto.setRequestIdx(requestId);
        requestDto.setxPagopaExtchCxId(clientId);

        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(fileCall.getFile(any(), any(), eq(false))).thenReturn(Mono.just(FILE_DOWNLOAD_RESPONSE));
        when(sesService.send(any(EmailField.class))).thenReturn(Mono.just(SendRawEmailResponse.builder().messageId("messageId").build()));

        //Gestore repository mocks.
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(emailSqsQueueName().getErrorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH, message,QUEUE_NAME);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(emailService, times(1)).sendNotificationOnStatusQueue(eq(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

    }

    @Test
    void gestionreRetryEmailAttachment_GenericError(){

        String clientId = EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH.getXPagopaExtchCxId();
        String requestId=EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH.getRequestIdx();

        var requestDto=buildRequestDto();
        requestDto.setRequestIdx(requestId);
        requestDto.setxPagopaExtchCxId(clientId);

        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());


        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(fileCall.getFile(any(), any(), eq(false))).thenReturn(Mono.just(FILE_DOWNLOAD_RESPONSE));
        when(sesService.send(any(EmailField.class))).thenReturn(Mono.just(SendRawEmailResponse.builder().messageId("messageId").build()));

        //Gestore repository mocks.
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.error(new RuntimeException()));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(emailSqsQueueName().getErrorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH, message,QUEUE_NAME);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(emailService, times(1)).sendNotificationOnStatusQueue(eq(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH), eq(INTERNAL_ERROR.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

    }
    @Test
    void testGestioneRetryEmailSchedulerBach_NoMessages() {
        // mock SQSService per restituire un Mono vuoto quando viene chiamato getOneMessage
        SqsServiceImpl mockSqsService = mock(SqsServiceImpl.class);
        when(mockSqsService.getOneMessage(emailSqsQueueName().getBatchName(), EmailPresaInCaricoInfo.class))
                .thenReturn(Mono.empty());

        // chiamare il metodo sotto test
        emailService.lavorazioneRichiestaBatch();

        // verificare che non sia stata eseguita alcuna operazione sul mock SQSService
        verify(mockSqsService, never()).deleteMessageFromQueue(eq(message), anyString());

    }

    private static EmailPresaInCaricoInfo newInfo(int attachments, boolean notificationTrackerStep) {
        var builder = EmailPresaInCaricoInfo.builder()
                .requestIdx("idTest")
                .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
                .digitalCourtesyMailRequest(createMailRequest(attachments));
        if (notificationTrackerStep) {
            builder.stepError(StepError.builder()
                    .step(NOTIFICATION_TRACKER_STEP)
                    .generatedMessageDto(new GeneratedMessageDto().id("messageId").system("system"))
                    .build());
        }
        return builder.build();
    }

    private void mockExhaustedRetry(EmailPresaInCaricoInfo info) {
        var requestDto = buildRequestDto();
        var exhaustedRequestDto = buildRequestDto();
        exhaustedRequestDto.getRequestMetadata().getRetry().setRetryStep(BigDecimal.valueOf(exhaustedRequestDto.getRequestMetadata().getRetry().getRetryPolicy().size() + 1L));
        String clientId = info.getXPagopaExtchCxId();
        String requestId = info.getRequestIdx();

        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        doReturn(Mono.just(exhaustedRequestDto)).when(gestoreRepositoryCall).patchRichiesta(eq(clientId), eq(requestId), any(PatchDto.class));
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(emailSqsQueueName().getErrorName())))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));
    }

    private DigitalProgressStatusDto captureStatusDto(EmailPresaInCaricoInfo info, Status status) {
        ArgumentCaptor<DigitalProgressStatusDto> captor = ArgumentCaptor.forClass(DigitalProgressStatusDto.class);
        verify(emailService, times(1)).sendNotificationOnStatusQueue(eq(info), eq(status.getStatusTransactionTableCompliant()), captor.capture());
        return captor.getValue();
    }

    @Test
    void gestioneRetryEmail_OnlyBodyNotificationTrackerStepRetriesExhausted_errorEventHasSanitizedErrorDetails() {
        var info = newInfo(0, true);
        mockExhaustedRetry(info);
        doReturn(Mono.error(new SqsClientException("queue for mario.rossi@example.com"))).doCallRealMethod()
                .when(sqsService).send(eq(pnEcConfig.getNotificationTracker().getSqsQueue().getStatoEmailName()), any(NotificationTrackerQueueDto.class));

        StepVerifier.create(emailService.gestioneRetryEmail(info, message, QUEUE_NAME)).expectNextCount(1).verifyComplete();

        String errorDetails = captureStatusDto(info, ERROR).getErrorDetails();
        assertNotNull(errorDetails);
        assertTrue(errorDetails.contains(PROCESS_ONLY_BODY_RETRY));
        assertTrue(errorDetails.contains("An error occurred during client operation"));
        assertTrue(errorDetails.contains("xxx@example.com"));
        assertFalse(errorDetails.contains("mario.rossi"));
        assertFalse(errorDetails.contains("\tat "));
    }

    @Test
    void gestioneRetryEmail_WithAttachNotificationTrackerStepRetriesExhausted_errorEventHasErrorDetails() {
        var info = newInfo(1, true);
        mockExhaustedRetry(info);
        doReturn(Mono.error(new SqsClientException("queue"))).when(emailService)
                .sendNotificationOnStatusQueue(any(), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

        StepVerifier.create(emailService.gestioneRetryEmail(info, message, QUEUE_NAME)).expectNextCount(1).verifyComplete();

        String errorDetails = captureStatusDto(info, ERROR).getErrorDetails();
        assertNotNull(errorDetails);
        assertTrue(errorDetails.contains(PROCESS_WITH_ATTACH_RETRY));
        assertTrue(errorDetails.contains("An error occurred during client operation"));
        assertFalse(errorDetails.contains("\tat "));
    }

    @Test
    void gestioneRetryEmail_OnlyBodySesRetriesExhausted_errorEventContainsSesCause() {
        var info = newInfo(0, false);
        mockExhaustedRetry(info);
        when(sesService.send(any(EmailField.class))).thenReturn(Mono.error(new SesSendException("ses provider failure")));

        StepVerifier.create(emailService.gestioneRetryEmail(info, message, QUEUE_NAME)).expectNextCount(1).verifyComplete();

        String errorDetails = captureStatusDto(info, ERROR).getErrorDetails();
        assertNotNull(errorDetails);
        assertTrue(errorDetails.contains(PROCESS_ONLY_BODY_RETRY));
        assertTrue(errorDetails.contains("SesMaxRetriesExceededException"));
        assertTrue(errorDetails.contains("ses provider failure"));
    }

    @Test
    void gestioneRetryEmail_WithAttachSesRetriesExhausted_errorEventContainsSesCause() {
        var info = newInfo(1, false);
        mockExhaustedRetry(info);
        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(fileCall.getFile(any(), any(), eq(false))).thenReturn(Mono.just(FILE_DOWNLOAD_RESPONSE));
        when(sesService.send(any(EmailField.class))).thenReturn(Mono.error(new SesSendException("ses provider failure")));

        StepVerifier.create(emailService.gestioneRetryEmail(info, message, QUEUE_NAME)).expectNextCount(1).verifyComplete();

        String errorDetails = captureStatusDto(info, ERROR).getErrorDetails();
        assertNotNull(errorDetails);
        assertTrue(errorDetails.contains(PROCESS_WITH_ATTACH_RETRY));
        assertTrue(errorDetails.contains("ses provider failure"));
    }

    @Test
    void gestioneRetryEmail_GenericError_internalErrorEventHasErrorDetails() {
        var info = newInfo(0, false);
        when(gestoreRepositoryCall.getRichiesta(info.getXPagopaExtchCxId(), info.getRequestIdx()))
                .thenReturn(Mono.error(new RuntimeException("repository failure for mario.rossi@example.com")));
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(emailSqsQueueName().getErrorName())))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        StepVerifier.create(emailService.gestioneRetryEmail(info, message, QUEUE_NAME)).expectNextCount(1).verifyComplete();

        String errorDetails = captureStatusDto(info, INTERNAL_ERROR).getErrorDetails();
        assertNotNull(errorDetails);
        assertTrue(errorDetails.contains(GESTIONE_RETRY_EMAIL));
        assertTrue(errorDetails.contains("repository failure"));
        assertTrue(errorDetails.contains("xxx@example.com"));
        assertFalse(errorDetails.contains("mario.rossi"));
    }

    @Test
    void gestioneRetryEmailAttachment_GenericError_internalErrorEventHasErrorDetails() {
        var info = newInfo(1, false);
        when(gestoreRepositoryCall.getRichiesta(info.getXPagopaExtchCxId(), info.getRequestIdx()))
                .thenReturn(Mono.error(new RuntimeException("repository failure")));
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(emailSqsQueueName().getErrorName())))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        StepVerifier.create(emailService.gestioneRetryEmail(info, message, QUEUE_NAME)).expectNextCount(1).verifyComplete();

        String errorDetails = captureStatusDto(info, INTERNAL_ERROR).getErrorDetails();
        assertNotNull(errorDetails);
        assertTrue(errorDetails.contains(GESTIONE_RETRY_EMAIL));
        assertTrue(errorDetails.contains("repository failure"));
    }

    @Test
    void gestioneRetryEmail_StatusToDelete_documentsInternalErrorInsteadOfDeleted() {
        var info = newInfo(0, false);
        when(gestoreRepositoryCall.getRichiesta(info.getXPagopaExtchCxId(), info.getRequestIdx())).thenReturn(Mono.just(buildToDeleteRequestDto()));
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(emailSqsQueueName().getErrorName())))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        StepVerifier.create(emailService.gestioneRetryEmail(info, message, QUEUE_NAME)).expectNextCount(1).verifyComplete();

        verify(emailService, never()).sendNotificationOnStatusQueue(eq(info), eq(DELETED.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        verify(emailService, times(1)).sendNotificationOnStatusQueue(eq(info), eq(INTERNAL_ERROR.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    @Test
    void gestioneRetryEmail_RetriesExhausted_errorPayloadWithErrorDetailsIsContractValid() {
        var info = newInfo(0, true);
        mockExhaustedRetry(info);
        doReturn(Mono.error(new SqsClientException("queue"))).doCallRealMethod()
                .when(sqsService).send(eq(pnEcConfig.getNotificationTracker().getSqsQueue().getStatoEmailName()), any(NotificationTrackerQueueDto.class));

        StepVerifier.create(emailService.gestioneRetryEmail(info, message, QUEUE_NAME)).expectNextCount(1).verifyComplete();

        DigitalProgressStatusDto dto = captureStatusDto(info, ERROR);
        assertNotNull(dto.getErrorDetails());
        assertTrue(dto.getErrorDetails().length() <= 1000);
        dto.setEventTimestamp(OffsetDateTime.now());
        dto.setStatus(ERROR.getStatusTransactionTableCompliant());
        PatchDto patchDto = new PatchDto().event(new EventsDto().digProgrStatus(dto));
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Set<ConstraintViolation<PatchDto>> violations = factory.getValidator().validate(patchDto);
            assertTrue(violations.isEmpty(), () -> violations.toString());
        }
    }

    private RequestDto buildRequestDtoWithRetryStep(long retryStep) {
        var requestDto = buildRequestDto();
        requestDto.getRequestMetadata().getRetry().setRetryStep(BigDecimal.valueOf(retryStep));
        return requestDto;
    }

    private RequestDto buildExhaustedRequestDto() {
        return buildRequestDtoWithRetryStep(buildRequestDto().getRequestMetadata().getRetry().getRetryPolicy().size() + 1L);
    }

    @Test
    void checkTentativiEccessiviEmail_retryStepOverPolicySize_errorEventHasSuppliedErrorDetails() {
        var info = newInfo(0, false);
        var requestDto = buildExhaustedRequestDto();
        String expected = "Exception in 'gestioneRetryEmail()' - java.lang.RuntimeException: queue failure, queue failure";
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(emailSqsQueueName().getErrorName())))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        StepVerifier.create(emailService.checkTentativiEccessiviEmail(info.getRequestIdx(), requestDto, info, message, () -> expected))
                    .expectNextCount(1)
                    .verifyComplete();

        assertEquals(expected, captureStatusDto(info, ERROR).getErrorDetails());
    }

    @Test
    void checkTentativiEccessiviEmail_retryStepOverPolicySize_supplierIsInvokedOnlyOnce() {
        var info = newInfo(0, false);
        var requestDto = buildExhaustedRequestDto();
        AtomicInteger supplierCalls = new AtomicInteger();
        Supplier<String> supplier = () -> {
            supplierCalls.incrementAndGet();
            return "text";
        };
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(emailSqsQueueName().getErrorName())))
                .thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        StepVerifier.create(emailService.checkTentativiEccessiviEmail(info.getRequestIdx(), requestDto, info, message, supplier))
                    .expectNextCount(1)
                    .verifyComplete();

        assertEquals(1, supplierCalls.get());
    }

    @Test
    void checkTentativiEccessiviEmail_retryStepEqualToPolicySize_documentsThatErrorIsNotPublishedAndTextIsNotBuilt() {
        var info = newInfo(0, false);
        var requestDto = buildRequestDtoWithRetryStep(buildRequestDto().getRequestMetadata().getRetry().getRetryPolicy().size());
        AtomicInteger supplierCalls = new AtomicInteger();
        Supplier<String> supplier = () -> {
            supplierCalls.incrementAndGet();
            return "text";
        };

        StepVerifier.create(emailService.checkTentativiEccessiviEmail(info.getRequestIdx(), requestDto, info, message, supplier))
                    .verifyComplete();

        verify(emailService, never()).sendNotificationOnStatusQueue(eq(info), eq(ERROR.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        assertEquals(0, supplierCalls.get());
    }

}
