package it.pagopa.pn.ec.pec.service.impl;

import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
import it.pagopa.pn.ec.commons.rest.call.aruba.ArubaCall;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.pec.configurationproperties.PecSqsQueueName;
import it.pagopa.pn.ec.pec.configurationproperties.PnPecConfigurationProperties;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pec.bridgews.SendMail;
import it.pec.bridgews.SendMailResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.ByteArrayOutputStream;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.model.pojo.request.StepError.StepErrorEnum.NOTIFICATION_TRACKER_STEP;
import static it.pagopa.pn.ec.commons.utils.EmailUtils.*;
import static it.pagopa.pn.ec.commons.utils.EmailUtils.getHeaderFromMimeMessage;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.ChannelEnum.PEC;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PecRetryTest {

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;
    @Autowired
    private PecSqsQueueName pecSqsQueueName;
    @SpyBean
    private SqsService sqsService;
    @MockBean
    private ArubaCall arubaCall;
    @MockBean
    private DownloadCall downloadCall;
    @SpyBean
    private AttachmentServiceImpl attachmentService;
    @SpyBean
    private PecService pecService;
    @SpyBean
    private PnPecConfigurationProperties pnPecConfigurationProperties;
    @MockBean
    private FileCall fileCall;
    @SpyBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    Message message = Message.builder().build();
    private static final DigitalNotificationRequest digitalNotificationRequest = new DigitalNotificationRequest();
    private static final ClientConfigurationDto clientConfigurationDto = new ClientConfigurationDto();
    private static final String ATTACHMENT_PREFIX = "safestorage://";
    private static final String defaultAttachmentUrl = "safestorage://prova.pdf";
    public static DigitalNotificationRequest createDigitalNotificationRequest() {

        List<String> defaultListAttachmentUrls = new ArrayList<>();
        defaultListAttachmentUrls.add(defaultAttachmentUrl);

        digitalNotificationRequest.setRequestId("requestIdx");
        digitalNotificationRequest.eventType("string");
        digitalNotificationRequest.setClientRequestTimeStamp(OffsetDateTime.now());
        digitalNotificationRequest.setQos(INTERACTIVE);
        digitalNotificationRequest.setReceiverDigitalAddress("pippo@pec.it");
        digitalNotificationRequest.setMessageText("string");
        digitalNotificationRequest.channel(PEC);
        digitalNotificationRequest.setSubjectText("prova testo");
        digitalNotificationRequest.setMessageContentType(PLAIN);
        digitalNotificationRequest.setAttachmentUrls(defaultListAttachmentUrls);
        return digitalNotificationRequest;
    }

    private static final StepError STEP_ERROR = StepError.builder()
            .generatedMessageDto(new GeneratedMessageDto().id("1221313223"))
            .step(NOTIFICATION_TRACKER_STEP)
            .build();
    private static final PecPresaInCaricoInfo PEC_PRESA_IN_CARICO_INFO = PecPresaInCaricoInfo.builder()
            .requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .stepError(STEP_ERROR)
            .digitalNotificationRequest(createDigitalNotificationRequest())
            .build();

    private static final PecPresaInCaricoInfo PEC_PRESA_IN_CARICO_INFO_NO_STEP_ERROR = PecPresaInCaricoInfo.builder()
            .requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalNotificationRequest(createDigitalNotificationRequest())
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
        requestDto.setRequestIdx(PEC_PRESA_IN_CARICO_INFO.getRequestIdx());
        requestDto.setxPagopaExtchCxId(PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId());
        requestDto.setRequestMetadata(requestMetadata);

        return requestDto;
    }

    @Test
    void testGestioneRetryPecScheduler_NoMessages() {
        // mock SQSService per restituire un Mono vuoto quando viene chiamato getOneMessage
        SqsServiceImpl mockSqsService = mock(SqsServiceImpl.class);
        when(mockSqsService.getOneMessage(pecSqsQueueName.errorName(), PecPresaInCaricoInfo.class))
                .thenReturn(Mono.empty());

        // chiamare il metodo sotto test
        pecService.gestioneRetryPecScheduler();

        // verificare che non sia stata eseguita alcuna operazione sul mock SQSService
        verify(mockSqsService, never()).deleteMessageFromQueue(eq(message), anyString());
    }


    @Test
    void gestionreRetryPec_GenericError() {

        String requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();
        String clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestDto=buildRequestDto();

        var sendMailResponse=new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));
        when(fileCall.getFile(any(), any(), eq(false))).thenReturn(Mono.just(FILE_DOWNLOAD_RESPONSE));

        //Gestore repository mocks.
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.error(new RuntimeException()));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(pecSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = pecService.gestioneRetryPec(PEC_PRESA_IN_CARICO_INFO, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(INTERNAL_ERROR.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

    }

    @Test
    void gestionreRetryPec_Retry_Ok(){

        String requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();
        String clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestDto=buildRequestDto();

        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());

        var sendMailResponse=new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));
        when(fileCall.getFile(any(), any(), eq(false))).thenReturn(Mono.just(FILE_DOWNLOAD_RESPONSE));

        //Gestore repository mocks.
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(pecSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = pecService.gestioneRetryPec(PEC_PRESA_IN_CARICO_INFO, message);
//        Mono<DeleteMessageResponse> response = pecService.gestioneRetryPec(PEC_PRESA_IN_CARICO_INFO_STEP_ERROR, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
//        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO_STEP_ERROR), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    @Test
    void gestionreRetryPec_MaxAttachmentsSizeExceeded_Limit(){

        String requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();
        String clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestDto=buildRequestDto();

        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());

        var sendMailResponse=new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        var file1 = new FileDownloadResponse().download(new FileDownloadInfo().url("safestorage://url1")).key("key1");
        var file2 = new FileDownloadResponse().download(new FileDownloadInfo().url("safestorage://url2")).key("key2");

        var file1ByteArray = new byte[1024];
        var file2ByteArray = new byte[pnPecConfigurationProperties.getMaxMessageSizeMb() * 1000000];

        var outputStream1 = new ByteArrayOutputStream();
        var outputStream2 = new ByteArrayOutputStream();

        outputStream1.writeBytes(file1ByteArray);
        outputStream2.writeBytes(file2ByteArray);

        when(pnPecConfigurationProperties.getAttachmentRule()).thenReturn("LIMIT");
        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(file1, file2));
        when(downloadCall.downloadFile(file1.getDownload().getUrl())).thenReturn(Mono.just(outputStream1));
        when(downloadCall.downloadFile(file2.getDownload().getUrl())).thenReturn(Mono.just(outputStream2));
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));

        //Gestore repository mocks.
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(pecSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = pecService.gestioneRetryPec(PEC_PRESA_IN_CARICO_INFO_NO_STEP_ERROR, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO_NO_STEP_ERROR), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

        var mimeMessageStr = extractSendMailData();
        assertTrue(mimeMessageStr.getBytes().length < pnPecConfigurationProperties.getMaxMessageSizeMb());
    }

    @Test
    void gestionreRetryPec_MaxAttachmentsSizeExceeded_First() {

        String requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();
        String clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestDto = buildRequestDto();

        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());

        var sendMailResponse = new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        var file1 = new FileDownloadResponse().download(new FileDownloadInfo().url("safestorage://url1")).key("key1");
        var file2 = new FileDownloadResponse().download(new FileDownloadInfo().url("safestorage://url2")).key("key2");

        var file1ByteArray = new byte[1024];
        var file2ByteArray = new byte[pnPecConfigurationProperties.getMaxMessageSizeMb() * 1000000];

        var outputStream1 = new ByteArrayOutputStream();
        var outputStream2 = new ByteArrayOutputStream();

        outputStream1.writeBytes(file1ByteArray);
        outputStream2.writeBytes(file2ByteArray);

        when(pnPecConfigurationProperties.getAttachmentRule()).thenReturn("FIRST");
        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(file1, file2));
        when(downloadCall.downloadFile(file1.getDownload().getUrl())).thenReturn(Mono.just(outputStream1));
        when(downloadCall.downloadFile(file2.getDownload().getUrl())).thenReturn(Mono.just(outputStream2));
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));

        //Gestore repository mocks.
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(pecSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = pecService.gestioneRetryPec(PEC_PRESA_IN_CARICO_INFO_NO_STEP_ERROR, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO_NO_STEP_ERROR), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

        var mimeMessageStr = extractSendMailData();
        assertTrue(mimeMessageStr.getBytes().length < pnPecConfigurationProperties.getMaxMessageSizeMb());
    }

    @Test
    void gestionreRetryPec_MaxAttachmentsSizeExceeded_Ko() {

        String requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();
        String clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestDto = buildRequestDto();

        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());

        var sendMailResponse = new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        var file = new FileDownloadResponse().download(new FileDownloadInfo().url("safestorage://url1")).key("key");
        var fileByteArray = new byte[(pnPecConfigurationProperties.getMaxMessageSizeMb() * 1000000) + 1024];
        var outputStream = new ByteArrayOutputStream();
        outputStream.writeBytes(fileByteArray);

        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(file));
        when(downloadCall.downloadFile(file.getDownload().getUrl())).thenReturn(Mono.just(outputStream));
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

        //Gestore repository mocks.
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class), eq(pecSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = pecService.gestioneRetryPec(PEC_PRESA_IN_CARICO_INFO_NO_STEP_ERROR, message);
        StepVerifier.create(response).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO_NO_STEP_ERROR), eq(RETRY.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "true;2100-02-01T10:00:00Z;false"})
    void gestionreRetryPec_XTipoRicevutaHeaderInserted_Ok(String headerValue){

        String requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();
        String clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestDto=buildRequestDto();

        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());

        var sendMailResponse=new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        var file = new FileDownloadResponse().download(new FileDownloadInfo().url("safestorage://url1")).key("key");
        var fileByteArray = new byte[1024];
        var outputStream = new ByteArrayOutputStream();
        outputStream.writeBytes(fileByteArray);

        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(file));
        when(downloadCall.downloadFile(file.getDownload().getUrl())).thenReturn(Mono.just(outputStream));
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));

        //Gestore repository mocks.
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));

        when(pnPecConfigurationProperties.getTipoRicevutaBreve()).thenReturn(headerValue);

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(pecSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = pecService.gestioneRetryPec(PEC_PRESA_IN_CARICO_INFO_NO_STEP_ERROR, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO_NO_STEP_ERROR), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

        String mimeMessageStr = extractSendMailData();
        var mimeMessage = getMimeMessage(mimeMessageStr.getBytes());
        var xTipoRicevutaHeader = getHeaderFromMimeMessage(mimeMessage, "X-TipoRicevuta");
        assertNotNull(xTipoRicevutaHeader);
        assertTrue(getHeaderFromMimeMessage(mimeMessage, "X-TipoRicevuta").length > 0);
    }
    @ParameterizedTest
    @ValueSource(strings = {"false", "true;2023-02-01T10:00:00Z;false"})
    void gestionreRetryPec_XTipoRicevutaHeaderNotInserted_Ok(String headerValue){

        String requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();
        String clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestDto=buildRequestDto();

        PatchDto patchDto = new PatchDto();
        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());

        var sendMailResponse=new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        var file = new FileDownloadResponse().download(new FileDownloadInfo().url("safestorage://url1")).key("key");
        var fileByteArray = new byte[1024];
        var outputStream = new ByteArrayOutputStream();
        outputStream.writeBytes(fileByteArray);

        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(file));
        when(downloadCall.downloadFile(file.getDownload().getUrl())).thenReturn(Mono.just(outputStream));
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));

        //Gestore repository mocks.
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.getRichiesta(clientId, requestId)).thenReturn(Mono.just(requestDto));
        when(gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto)).thenReturn(Mono.just(requestDto));

        when(pnPecConfigurationProperties.getTipoRicevutaBreve()).thenReturn(headerValue);

        // Mock dell'eliminazione di una generica notifica dalla coda degli errori.
        when(sqsService.deleteMessageFromQueue(any(Message.class),eq(pecSqsQueueName.errorName()))).thenReturn(Mono.just(DeleteMessageResponse.builder().build()));

        Mono<DeleteMessageResponse> response = pecService.gestioneRetryPec(PEC_PRESA_IN_CARICO_INFO_NO_STEP_ERROR, message);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO_NO_STEP_ERROR), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        String mimeMessageStr = extractSendMailData();
        var mimeMessage = getMimeMessage(mimeMessageStr.getBytes());
        var xTipoRicevutaHeader = getHeaderFromMimeMessage(mimeMessage, "X-TipoRicevuta");
        assertNull(xTipoRicevutaHeader);
        fail();
    }

    @Test
    void testGestioneRetryPecSchedulerBach_NoMessages() {
        // mock SQSService per restituire un Mono vuoto quando viene chiamato getOneMessage
        SqsServiceImpl mockSqsService = mock(SqsServiceImpl.class);
        when(mockSqsService.getOneMessage(pecSqsQueueName.batchName(), PecPresaInCaricoInfo.class))
                .thenReturn(Mono.empty());

        // chiamare il metodo sotto test
        pecService.lavorazioneRichiestaBatch();

        // verificare che non sia stata eseguita alcuna operazione sul mock SQSService
        verify(mockSqsService, never()).deleteMessageFromQueue(eq(message), anyString());

    }

    private String extractSendMailData() {
        ArgumentCaptor<SendMail> argumentCaptor = ArgumentCaptor.forClass(SendMail.class);
        verify(arubaCall, times(1)).sendMail(argumentCaptor.capture());
        var sendMail = argumentCaptor.getValue();
        return getMimeMessageFromCDATATag(sendMail.getData());
    }

}