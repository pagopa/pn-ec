package it.pagopa.pn.ec.pec.service.impl;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.rest.call.aruba.ArubaCallImpl;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.pec.configurationproperties.PecSqsQueueName;
import it.pagopa.pn.ec.pec.configurationproperties.PnPecConfigurationProperties;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pec.bridgews.SendMail;
import it.pec.bridgews.SendMailResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.utils.EmailUtils.*;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.ChannelEnum.PEC;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PecServiceTest {

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;
    @Autowired
    private PecSqsQueueName pecSqsQueueName;
    @MockBean
    private FileCall uriBuilderCall;
    @MockBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @MockBean
    private AuthService authService;
    @SpyBean
    private SqsServiceImpl sqsService;
    @MockBean
    private ArubaCallImpl arubaCall;
    @MockBean
    private AttachmentServiceImpl attachmentService;
    @MockBean
    private DownloadCall downloadCall;
    @SpyBean
    private PecService pecService;
    @SpyBean
    private PnPecConfigurationProperties pnPecConfigurationProperties;
    @Mock
    private Acknowledgment acknowledgment;
    private static final DigitalNotificationRequest digitalNotificationRequest = new DigitalNotificationRequest();
    private static final ClientConfigurationDto clientConfigurationDto = new ClientConfigurationDto();
    private static final RequestDto requestDto = new RequestDto();
    private static final String ATTACHMENT_PREFIX = "safestorage://";
    private static final String defaultAttachmentUrl = "safestorage://prova.pdf";
    private static Integer MAX_MESSAGE_SIZE_KB;
    private String TIPO_RICEVUTA_BREVE_DEFAULT;
    public static DigitalNotificationRequest createDigitalNotificationRequest() {
//        Mock an existing request. Set the requestIdx
        requestDto.setRequestIdx("requestIdx");

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
    private static final PecPresaInCaricoInfo PEC_PRESA_IN_CARICO_INFO = PecPresaInCaricoInfo.builder()
            .requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalNotificationRequest(createDigitalNotificationRequest())
            .build();

    private static final PecPresaInCaricoInfo PEC_PRESA_IN_CARICO_INFO_TEST = PecPresaInCaricoInfo.builder()
            .requestIdx("idTest")
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalNotificationRequest(new DigitalNotificationRequest())
            .build();

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

    @BeforeAll
    static void beforeAll(@Autowired PnPecConfigurationProperties pnPecConfigurationProperties) {
        MAX_MESSAGE_SIZE_KB = pnPecConfigurationProperties.getMaxMessageSizeMb() * MB_TO_BYTES;
    }
    @BeforeEach
    void setUp() {
        TIPO_RICEVUTA_BREVE_DEFAULT = (String) ReflectionTestUtils.getField(pnPecConfigurationProperties, "tipoRicevutaBreve");
    }
    @AfterEach
    void afterEach() {
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "tipoRicevutaBreve", TIPO_RICEVUTA_BREVE_DEFAULT);
    }

    @Test
    void lavorazionePec_Ok(){

        var requestDto=buildRequestDto();
        var clientId=PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId=PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        var sendMailResponse=new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(new FileDownloadResponse()));
        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

    }

    @Test
    void lavorazionePec_MaxRetriesExceeded() {

        var requestDto=buildRequestDto();
        var clientId=PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId=PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        var sendMailResponse=new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(new FileDownloadResponse()));
        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.error(new RuntimeException()));

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(RETRY.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

    }
    @Test
    void lavorazionePecMaxAttachmentsSizeExceeded_MoreAttachments_Limit() {
        var requestDto = buildRequestDto();
        var clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        var sendMailResponse = new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        mockAttachmentsWithLastInOffset(3);
        when(pnPecConfigurationProperties.getAttachmentRule()).thenReturn("LIMIT");
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

        var mimeMessageStr = extractSendMailData();
        var mimeMessage = getMimeMessage(mimeMessageStr.getBytes());
        var multipart=getMultipartFromMimeMessage(mimeMessage);

        assertTrue(mimeMessageStr.getBytes().length < MAX_MESSAGE_SIZE_KB);
        //Body del messaggio + allegati
        assertEquals(3, getMultipartCount(multipart));
    }

    @Test
    void lavorazionePecMaxAttachmentsSizeExceeded_MoreAttachments_First()  {
        var requestDto = buildRequestDto();
        var clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        var sendMailResponse = new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        mockAttachmentsWithLastInOffset(3);
        when(pnPecConfigurationProperties.getAttachmentRule()).thenReturn("FIRST");
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

        var mimeMessageStr = extractSendMailData();
        var mimeMessage = getMimeMessage(mimeMessageStr.getBytes());
        var multipart=getMultipartFromMimeMessage(mimeMessage);

        assertTrue(mimeMessageStr.getBytes().length < MAX_MESSAGE_SIZE_KB);
        //Body del messaggio + 1 allegato
        assertEquals(2, getMultipartCount(multipart));
    }

    @Test
    void lavorazionePecMaxAttachmentsSizeExceeded_OneAttachment() {
        var requestDto = buildRequestDto();
        var clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        var sendMailResponse = new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        mockAttachmentsWithLastInOffset(1);
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(RETRY.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        verify(arubaCall, never()).sendMail(any(SendMail.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "true;2100-02-01T10:00:00Z;false"})
    void lavorazionePec_XTipoRicevutaHeaderInserted_Ok(String headerValue) {
        var requestDto=buildRequestDto();
        var clientId=PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId=PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        var sendMailResponse=new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(new FileDownloadResponse()));
        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "tipoRicevutaBreve", headerValue);

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

        String mimeMessageStr = extractSendMailData();
        var mimeMessage = getMimeMessage(mimeMessageStr.getBytes());
        var xTipoRicevutaHeader = getHeaderFromMimeMessage(mimeMessage, pnPecConfigurationProperties.getTipoRicevutaHeaderName());
        assertNotNull(xTipoRicevutaHeader);
        assertTrue(getHeaderFromMimeMessage(mimeMessage, pnPecConfigurationProperties.getTipoRicevutaHeaderName()).length > 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "true;2023-02-01T10:00:00Z;false"})
    void lavorazionePec_XTipoRicevutaHeaderNotInserted_Ok(String headerValue) {
        var requestDto = buildRequestDto();
        var clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        var sendMailResponse = new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(new FileDownloadResponse()));
        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "tipoRicevutaBreve", headerValue);

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

        String mimeMessageStr = extractSendMailData();
        var mimeMessage = getMimeMessage(mimeMessageStr.getBytes());
        var xTipoRicevutaHeader = getHeaderFromMimeMessage(mimeMessage, pnPecConfigurationProperties.getTipoRicevutaHeaderName());
        assertNull(xTipoRicevutaHeader);
    }

    private String extractSendMailData() {
        ArgumentCaptor<SendMail> argumentCaptor = ArgumentCaptor.forClass(SendMail.class);
        verify(arubaCall, times(1)).sendMail(argumentCaptor.capture());
        var sendMail = argumentCaptor.getValue();
        return getMimeMessageFromCDATATag(sendMail.getData());
    }

    private void mockAttachmentsWithLastInOffset(int numOfAttachments) {
        List<FileDownloadResponse> fileDownloadResponseList = new ArrayList<>();
        int sizeForAttachment = MAX_MESSAGE_SIZE_KB / numOfAttachments;
        for (int i = 0; i < numOfAttachments; i++) {
            var file = new FileDownloadResponse().download(new FileDownloadInfo().url("safestorage://url" + i)).key("key" + i);
            fileDownloadResponseList.add(file);
            byte[] fileByteArray;
            if (i != numOfAttachments - 1)
                fileByteArray = new byte[sizeForAttachment];
            else fileByteArray = new byte[sizeForAttachment + 10000];
            var outputStream = new ByteArrayOutputStream();
            outputStream.writeBytes(fileByteArray);
            when(downloadCall.downloadFile(file.getDownload().getUrl())).thenReturn(Mono.just(outputStream));
        }
        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.fromIterable(fileDownloadResponseList));
    }

}