package it.pagopa.pn.ec.email.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
import it.pagopa.pn.ec.commons.model.pojo.sqs.SqsMessageWrapper;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.SesService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.email.configurationproperties.EmailSqsQueueName;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
import static it.pagopa.pn.ec.commons.constant.Status.INTERNAL_ERROR;
import static it.pagopa.pn.ec.commons.constant.Status.SENT;

import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.NOTIFICATION_TRACKER_STEP;
import static it.pagopa.pn.ec.email.testutils.DigitalCourtesyMailRequestFactory.createMailRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class RetryEmailTest {

    @SpyBean
    private EmailService emailService;
    @Autowired
    private EmailSqsQueueName emailSqsQueueName;
    @SpyBean
    private SqsServiceImpl sqsService;
    @MockBean
    private FileCall fileCall;
    @SpyBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @MockBean
    private SesService sesService;
    @MockBean
    private DownloadCall downloadCall;
    EmailPresaInCaricoInfo emailPresaInCaricoInfo = new EmailPresaInCaricoInfo();

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

    private static final StepError STEP_ERROR = StepError.builder()
            .generatedMessageDto(new GeneratedMessageDto().id("1221313223"))
            .notificationTrackerError(NOTIFICATION_TRACKER_STEP)
            .build();

    private static final EmailPresaInCaricoInfo EMAIL_PRESA_IN_CARICO_INFO_STEP_ERROR = EmailPresaInCaricoInfo.builder()
            .requestIdx("idTestStepError")
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .stepError(STEP_ERROR)
            .digitalCourtesyMailRequest(createMailRequest(0))
            .build();


    private final SqsMessageWrapper<EmailPresaInCaricoInfo> sqsPresaInCaricoInfo = new SqsMessageWrapper<>(message, EMAIL_PRESA_IN_CARICO_INFO);

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
    @Test
    void testGestioneRetryEmailScheduler_NoMessages() {
        // mock SQSService per restituire un Mono vuoto quando viene chiamato getOneMessage
        SqsServiceImpl mockSqsService = mock(SqsServiceImpl.class);
        when(mockSqsService.getOneMessage(emailSqsQueueName.errorName(), EmailPresaInCaricoInfo.class))
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
        when(gestoreRepositoryCall.getRichiesta(eq(clientId), eq(requestId))).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(emailSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        //when(gestoreRepositoryCall.patchRichiesta(eq(requestId), eq(patchDto)).thenReturn(Mono.just(requestDto)));

        Mono<DeleteMessageResponse> response = emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(emailService, times(1)).sendNotificationOnStatusQueue(eq(EMAIL_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

    }

    @Test
    void gestionreRetryEmail_GenericError(){

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
        when(gestoreRepositoryCall.getRichiesta(eq(clientId), eq(requestId))).thenReturn(Mono.error(new RuntimeException()));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(emailSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));
//        DeleteMessageResponse response =
//                emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO, message).block();
                emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO_STEP_ERROR, message).block();

        Mono<DeleteMessageResponse> response = emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();
//        Assert.assertNull(response);
    }

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
        when(gestoreRepositoryCall.getRichiesta(eq(clientId), eq(requestId))).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(emailSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH, message);
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
        when(gestoreRepositoryCall.getRichiesta(eq(clientId), eq(requestId))).thenReturn(Mono.error(new RuntimeException()));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(emailSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = emailService.gestioneRetryEmail(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(emailService, times(1)).sendNotificationOnStatusQueue(eq(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH), eq(INTERNAL_ERROR.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

    }
    @Test
    void testGestioneRetryEmailSchedulerBach_NoMessages() {
        // mock SQSService per restituire un Mono vuoto quando viene chiamato getOneMessage
        SqsServiceImpl mockSqsService = mock(SqsServiceImpl.class);
        when(mockSqsService.getOneMessage(emailSqsQueueName.batchName(), EmailPresaInCaricoInfo.class))
                .thenReturn(Mono.empty());

        // chiamare il metodo sotto test
        emailService.lavorazioneRichiestaBatch();

        // verificare che non sia stata eseguita alcuna operazione sul mock SQSService
        verify(mockSqsService, never()).deleteMessageFromQueue(eq(message), anyString());

    }
}