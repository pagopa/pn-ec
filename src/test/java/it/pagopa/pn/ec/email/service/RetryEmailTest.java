package it.pagopa.pn.ec.email.service;

import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
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

import static it.pagopa.pn.ec.commons.constant.Status.ERROR;
import static it.pagopa.pn.ec.commons.constant.Status.INTERNAL_ERROR;
import static it.pagopa.pn.ec.commons.constant.Status.SENT;
import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.NOTIFICATION_TRACKER_STEP;
import static it.pagopa.pn.ec.email.testutils.DigitalCourtesyMailRequestFactory.createMailRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private static final String BODY_REQUEST_IDX = "idTest";
    private static final List<BigDecimal> PRODUCTION_POLICY = List.of(BigDecimal.valueOf(5), BigDecimal.valueOf(10), BigDecimal.valueOf(20), BigDecimal.valueOf(40));
    private static final int PRODUCTION_POLICY_SIZE = PRODUCTION_POLICY.size();

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
            .requestIdx(BODY_REQUEST_IDX)
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

    private static RequestDto buildProductionPolicyRequestDto(String requestId, int storedStep) {
        RetryDto retryDto = new RetryDto();
        retryDto.setLastRetryTimestamp(OffsetDateTime.now().minusMinutes(60));
        retryDto.setRetryStep(BigDecimal.valueOf(storedStep));
        retryDto.setRetryPolicy(new ArrayList<>(PRODUCTION_POLICY));

        RequestMetadataDto requestMetadata = new RequestMetadataDto();
        requestMetadata.setRetry(retryDto);

        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx(requestId);
        requestDto.setxPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE);
        requestDto.setRequestMetadata(requestMetadata);
        return requestDto;
    }

    private static EmailPresaInCaricoInfo buildPresaInCarico(String requestId, int attachNum, StepError stepError) {
        return EmailPresaInCaricoInfo.builder()
                .requestIdx(requestId)
                .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
                .digitalCourtesyMailRequest(createMailRequest(attachNum))
                .stepError(stepError)
                .build();
    }

    private static StepError notificationTrackerStepError() {
        return StepError.builder()
                .step(NOTIFICATION_TRACKER_STEP)
                .generatedMessageDto(new GeneratedMessageDto().id("messageId"))
                .build();
    }

    private void mockRetryRepository(RequestDto requestDto) {
        doReturn(Mono.just(requestDto)).when(gestoreRepositoryCall).getRichiesta(requestDto.getxPagopaExtchCxId(), requestDto.getRequestIdx());
        doReturn(Mono.just(requestDto)).when(gestoreRepositoryCall).patchRichiesta(anyString(), anyString(), any(PatchDto.class));
        doReturn(Mono.just(DeleteMessageResponse.builder().build())).when(sqsService)
                .deleteMessageFromQueue(any(Message.class), eq(emailSqsQueueName().getErrorName()));
    }

    private void mockAttachmentsDownload() {
        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(fileCall.getFile(any(), any(), eq(false))).thenReturn(Mono.just(FILE_DOWNLOAD_RESPONSE));
    }

    private void mockSesFailure() {
        when(sesService.send(any(EmailField.class))).thenReturn(Mono.error(new RuntimeException("ses send failure")));
    }

    private void mockFirstTrackerPublicationFailure() {
        doReturn(Mono.error(new RuntimeException("tracker publication failure"))).doCallRealMethod().when(sqsService)
                .send(anyString(), any());
    }

    private void executeRetry(EmailPresaInCaricoInfo presaInCaricoInfo) {
        Mono<DeleteMessageResponse> response = emailService.gestioneRetryEmail(presaInCaricoInfo, message, QUEUE_NAME);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();
    }

    private List<String> publishedStatuses() {
        ArgumentCaptor<NotificationTrackerQueueDto> captor = ArgumentCaptor.forClass(NotificationTrackerQueueDto.class);
        verify(sqsService, atLeast(0)).send(eq(pnEcConfig.getNotificationTracker().getSqsQueue().getStatoEmailName()), captor.capture());
        return captor.getAllValues().stream().map(NotificationTrackerQueueDto::getNextStatus).toList();
    }

    private void verifyMessageDeletedOnce() {
        verify(sqsService, times(1)).deleteMessageFromQueue(any(Message.class), eq(emailSqsQueueName().getErrorName()));
    }

    @Test
    void gestioneRetryEmail_LastRetryFailsWithAttachments_PublishesErrorAndDeletesMessage() {
        var requestDto = buildProductionPolicyRequestDto(DEFAULT_REQUEST_IDX, PRODUCTION_POLICY_SIZE - 1);
        var presaInCaricoInfo = buildPresaInCarico(DEFAULT_REQUEST_IDX, 1, null);
        mockRetryRepository(requestDto);
        mockAttachmentsDownload();
        mockSesFailure();

        executeRetry(presaInCaricoInfo);

        assertEquals(List.of(ERROR.getStatusTransactionTableCompliant()), publishedStatuses());
        verifyMessageDeletedOnce();
    }

    @Test
    void gestioneRetryEmail_LastRetryFailsOnlyBody_PublishesErrorAndDeletesMessage() {
        var requestDto = buildProductionPolicyRequestDto(BODY_REQUEST_IDX, PRODUCTION_POLICY_SIZE - 1);
        var presaInCaricoInfo = buildPresaInCarico(BODY_REQUEST_IDX, 0, null);
        mockRetryRepository(requestDto);
        mockSesFailure();

        executeRetry(presaInCaricoInfo);

        assertEquals(List.of(ERROR.getStatusTransactionTableCompliant()), publishedStatuses());
        verifyMessageDeletedOnce();
    }

    @Test
    void gestioneRetryEmail_ExhaustedStepWithAttachments_PublishesErrorWithoutSendingNorPatching() {
        var requestDto = buildProductionPolicyRequestDto(DEFAULT_REQUEST_IDX, PRODUCTION_POLICY_SIZE);
        var presaInCaricoInfo = buildPresaInCarico(DEFAULT_REQUEST_IDX, 1, null);
        mockRetryRepository(requestDto);

        executeRetry(presaInCaricoInfo);

        assertEquals(List.of(ERROR.getStatusTransactionTableCompliant()), publishedStatuses());
        verify(sesService, never()).send(any(EmailField.class));
        verify(downloadCall, never()).downloadFile(any());
        verify(gestoreRepositoryCall, never()).patchRichiesta(anyString(), anyString(), any(PatchDto.class));
        verifyMessageDeletedOnce();
    }

    @Test
    void gestioneRetryEmail_ExhaustedStepOnlyBody_PublishesErrorWithoutSendingNorPatching() {
        var requestDto = buildProductionPolicyRequestDto(BODY_REQUEST_IDX, PRODUCTION_POLICY_SIZE);
        var presaInCaricoInfo = buildPresaInCarico(BODY_REQUEST_IDX, 0, null);
        mockRetryRepository(requestDto);

        executeRetry(presaInCaricoInfo);

        assertEquals(List.of(ERROR.getStatusTransactionTableCompliant()), publishedStatuses());
        verify(sesService, never()).send(any(EmailField.class));
        verify(gestoreRepositoryCall, never()).patchRichiesta(anyString(), anyString(), any(PatchDto.class));
        verifyMessageDeletedOnce();
    }

    @Test
    void gestioneRetryEmail_NotificationTrackerStepWithAttachmentsFailsOnLastStep_PublishesErrorAndDeletesMessage() {
        var requestDto = buildProductionPolicyRequestDto(DEFAULT_REQUEST_IDX, PRODUCTION_POLICY_SIZE - 1);
        var presaInCaricoInfo = buildPresaInCarico(DEFAULT_REQUEST_IDX, 1, notificationTrackerStepError());
        mockRetryRepository(requestDto);
        mockFirstTrackerPublicationFailure();

        executeRetry(presaInCaricoInfo);

        assertEquals(List.of(SENT.getStatusTransactionTableCompliant(), ERROR.getStatusTransactionTableCompliant()), publishedStatuses());
        verifyMessageDeletedOnce();
    }

    @Test
    void gestioneRetryEmail_NotificationTrackerStepOnlyBodyFailsOnLastStep_PublishesErrorAndDeletesMessage() {
        var requestDto = buildProductionPolicyRequestDto(BODY_REQUEST_IDX, PRODUCTION_POLICY_SIZE - 1);
        var presaInCaricoInfo = buildPresaInCarico(BODY_REQUEST_IDX, 0, notificationTrackerStepError());
        mockRetryRepository(requestDto);
        mockFirstTrackerPublicationFailure();

        executeRetry(presaInCaricoInfo);

        assertEquals(List.of(SENT.getStatusTransactionTableCompliant(), ERROR.getStatusTransactionTableCompliant()), publishedStatuses());
        verifyMessageDeletedOnce();
    }

    @Test
    void gestioneRetryEmail_NotificationTrackerStepWithAttachmentsAndExhaustedStep_PublishesSentOnly() {
        var requestDto = buildProductionPolicyRequestDto(DEFAULT_REQUEST_IDX, PRODUCTION_POLICY_SIZE);
        var presaInCaricoInfo = buildPresaInCarico(DEFAULT_REQUEST_IDX, 1, notificationTrackerStepError());
        mockRetryRepository(requestDto);

        executeRetry(presaInCaricoInfo);

        assertEquals(List.of(SENT.getStatusTransactionTableCompliant()), publishedStatuses());
        verifyMessageDeletedOnce();
    }

    @Test
    void gestioneRetryEmail_NotificationTrackerStepOnlyBodyAndExhaustedStep_PublishesSentOnly() {
        var requestDto = buildProductionPolicyRequestDto(BODY_REQUEST_IDX, PRODUCTION_POLICY_SIZE);
        var presaInCaricoInfo = buildPresaInCarico(BODY_REQUEST_IDX, 0, notificationTrackerStepError());
        mockRetryRepository(requestDto);

        executeRetry(presaInCaricoInfo);

        assertEquals(List.of(SENT.getStatusTransactionTableCompliant()), publishedStatuses());
        verifyMessageDeletedOnce();
    }
}
