package it.pagopa.pn.ec.pec.service.impl;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
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
import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.pec.service.ArubaService;
import it.pagopa.pn.library.pec.service.PnEcPecService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
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
import java.util.stream.Stream;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.utils.EmailUtils.*;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.ChannelEnum.PEC;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static it.pagopa.pn.library.pec.utils.PnPecUtils.ARUBA_PROVIDER;
import static it.pagopa.pn.library.pec.utils.PnPecUtils.NAMIRIAL_PROVIDER;
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
    @MockBean(name = "arubaServiceImpl")
    private ArubaService arubaService;
    @MockBean(name = "namirialService")
    private com.namirial.pec.library.service.PnPecServiceImpl namirialService;
    @MockBean
    private AttachmentServiceImpl attachmentService;
    @MockBean
    private DownloadCall downloadCall;
    @SpyBean
    private PecService pecService;
    @SpyBean
    private PnPecConfigurationProperties pnPecConfigurationProperties;
    @SpyBean
    private PnEcPecService pnPecService;
    @Mock
    private Acknowledgment acknowledgment;
    @Value("${aruba.pec.sender}")
    private String arubaPecSender;
    @Value("${namirial.pec.sender}")
    private String namirialPecSender;
    private static final DigitalNotificationRequest digitalNotificationRequest = new DigitalNotificationRequest();
    private static final RequestDto REQUEST_DTO = new RequestDto();
    private static final String DEFAULT_ATTACHMENT_URL = "safestorage://prova.pdf";
    private static Integer maxMessageSizeKb;
    private String tipoRicevutaBreveDefault;
    private String providerSwitchWriteDefault;
    private static final String PROVIDER_SWITCH_WRITE_ARUBA = "1970-01-01T00:00:00Z;aruba";
    private static final String PROVIDER_SWITCH_WRITE_NAMIRIAL = "1970-01-01T00:00:00Z;namirial";
    public static DigitalNotificationRequest createDigitalNotificationRequest() {
//        Mock an existing request. Set the requestIdx
        REQUEST_DTO.setRequestIdx("requestIdx");

        List<String> defaultListAttachmentUrls = new ArrayList<>();
        defaultListAttachmentUrls.add(DEFAULT_ATTACHMENT_URL);

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
        maxMessageSizeKb = pnPecConfigurationProperties.getMaxMessageSizeMb() * MB_TO_BYTES;
    }
    @BeforeEach
    void setUp() {
        tipoRicevutaBreveDefault = (String) ReflectionTestUtils.getField(pnPecConfigurationProperties, "tipoRicevutaBreve");
        providerSwitchWriteDefault = (String) ReflectionTestUtils.getField(pnPecConfigurationProperties, "pnPecProviderSwitchWrite");
    }
    @AfterEach
    void afterEach() {
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "tipoRicevutaBreve", tipoRicevutaBreveDefault);
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitchWrite", providerSwitchWriteDefault);
    }

    @Test
    void lavorazionePec_Ok(){

        var requestDto=buildRequestDto();
        var clientId=PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId=PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(new FileDownloadResponse()));
        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(arubaService.sendMail(any())).thenReturn(Mono.just("errorstr"));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

    }

    @Test
    void lavorazionePec_BadAddressKo() {

        var requestDto = buildRequestDto();

        var clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(new FileDownloadResponse()));
        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(arubaService.sendMail(any())).thenReturn(Mono.error(new PnSpapiPermanentErrorException("class jakarta.mail.internet.AddressException Local address starts with dot")));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(ADDRESS_ERROR.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

    }

    @Test
    void lavorazionePec_MaxRetriesExceeded() {

        var clientId=PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId=PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(new FileDownloadResponse()));
        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(arubaService.sendMail(any())).thenReturn(Mono.just("errorstr"));
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

        mockAttachmentsWithLastInOffset(3);
        when(pnPecConfigurationProperties.getAttachmentRule()).thenReturn("LIMIT");
        when(arubaService.sendMail(any())).thenReturn(Mono.just("errorstr"));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

        var mimeMessageBytes = extractSendMailData();
        var mimeMessage = getMimeMessage(mimeMessageBytes);
        var multipart=getMultipartFromMimeMessage(mimeMessage);

        assertTrue(mimeMessageBytes.length < maxMessageSizeKb);
        //Body del messaggio + allegati
        assertEquals(3, getMultipartCount(multipart));
    }

    @Test
    void lavorazionePecMaxAttachmentsSizeExceeded_MoreAttachments_First()  {
        var requestDto = buildRequestDto();
        var clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        mockAttachmentsWithLastInOffset(3);
        when(pnPecConfigurationProperties.getAttachmentRule()).thenReturn("FIRST");
        when(arubaService.sendMail(any())).thenReturn(Mono.just("errorstr"));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

        var mimeMessageBytes = extractSendMailData();
        var mimeMessage = getMimeMessage(mimeMessageBytes);
        var multipart=getMultipartFromMimeMessage(mimeMessage);

        assertTrue(mimeMessageBytes.length < maxMessageSizeKb);
        //Body del messaggio + 1 allegato
        assertEquals(2, getMultipartCount(multipart));
    }

    @Test
    void lavorazionePecMaxAttachmentsSizeExceeded_OneAttachment() {
        var requestDto = buildRequestDto();
        var clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        mockAttachmentsWithLastInOffset(1);
        when(arubaService.sendMail(any())).thenReturn(Mono.just("errorstr"));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(RETRY.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        verify(arubaService, never()).sendMail(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "true;2100-02-01T10:00:00Z;false"})
    void lavorazionePec_XTipoRicevutaHeaderInserted_Ok(String headerValue) {
        var requestDto=buildRequestDto();
        var clientId=PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId=PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(new FileDownloadResponse()));
        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(arubaService.sendMail(any())).thenReturn(Mono.just("errorstr"));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "tipoRicevutaBreve", headerValue);

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

        byte[] mimeMessageBytes = extractSendMailData();
        var mimeMessage = getMimeMessage(mimeMessageBytes);
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

        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(new FileDownloadResponse()));
        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(arubaService.sendMail(any())).thenReturn(Mono.just("errorstr"));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "tipoRicevutaBreve", headerValue);

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

        byte[] mimeMessageBytes = extractSendMailData();
        var mimeMessage = getMimeMessage(mimeMessageBytes);
        var xTipoRicevutaHeader = getHeaderFromMimeMessage(mimeMessage, pnPecConfigurationProperties.getTipoRicevutaHeaderName());
        assertNull(xTipoRicevutaHeader);
    }

    private static Stream<Arguments> providerSource() {
        return Stream.of(Arguments.of(PROVIDER_SWITCH_WRITE_ARUBA, ARUBA_PROVIDER), Arguments.of(PROVIDER_SWITCH_WRITE_NAMIRIAL, NAMIRIAL_PROVIDER));
    }

    @ParameterizedTest
    @MethodSource("providerSource")
    void lavorazionePec_SenderSwitch_Ok(String providerSwitch, String providerName) {
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitchWrite", providerSwitch);

        var requestDto=buildRequestDto();
        var clientId=PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId=PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(new FileDownloadResponse()));
        when(downloadCall.downloadFile(any())).thenReturn(Mono.just(new ByteArrayOutputStream()));
        String messageID = "messageID";
        when(arubaService.sendMail(any())).thenReturn(Mono.just(messageID));
        when(namirialService.sendMail(any())).thenReturn(Mono.just(messageID));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        verifyPecProviderSwitch(providerName);
    }

    private byte[] extractSendMailData() {
        ArgumentCaptor<byte[]> argumentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(arubaService, times(1)).sendMail(argumentCaptor.capture());
        return argumentCaptor.getValue();
    }

    private void mockAttachmentsWithLastInOffset(int numOfAttachments) {
        List<FileDownloadResponse> fileDownloadResponseList = new ArrayList<>();
        int sizeForAttachment = maxMessageSizeKb / numOfAttachments;
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

    private void verifyPecProviderSwitch(String providerName) {
        ArgumentCaptor<byte[]> argumentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(pnPecService, times(1)).sendMail(argumentCaptor.capture());
        var mimeMessage = getMimeMessage(argumentCaptor.getValue());
        var sender = getSenderFromMimeMessage(mimeMessage);
        if (providerName.equals(ARUBA_PROVIDER)) {
            verify(arubaService, times(1)).sendMail(argumentCaptor.capture());
            assertEquals(arubaPecSender, sender);
        } else {
            verify(namirialService, times(1)).sendMail(argumentCaptor.capture());
            assertEquals(namirialPecSender, sender);
        }
    }

}