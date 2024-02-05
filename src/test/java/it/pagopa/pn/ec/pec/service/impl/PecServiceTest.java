package it.pagopa.pn.ec.pec.service.impl;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.model.pojo.pec.PnPostacert;
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
import lombok.CustomLog;
import org.aspectj.lang.annotation.Before;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.annotation.BeforeTestExecution;
import org.springframework.test.context.event.annotation.BeforeTestMethod;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;


import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.ChannelEnum.PEC;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@CustomLog
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
    @Value("${pn.ec.pec.attachment-rule}")
    private String pecAttachmentRule;
    @Value("${pn.ec.pec.max-message-size-mb}")
    private Integer maxMessageSizeMb;

    private static final DigitalNotificationRequest digitalNotificationRequest = new DigitalNotificationRequest();
    private static final ClientConfigurationDto clientConfigurationDto = new ClientConfigurationDto();
    private static final RequestDto requestDto = new RequestDto();
    private static final String ATTACHMENT_PREFIX = "safestorage://";
    private static final String defaultAttachmentUrl = "safestorage://prova.pdf";
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
    void lavorazionePecMaxAttachmentsSizeExceeded_Limit() {
        var requestDto = buildRequestDto();
        var clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        var sendMailResponse = new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        var file1 = new FileDownloadResponse().download(new FileDownloadInfo().url("safestorage://url1")).key("key1");
        var file2 = new FileDownloadResponse().download(new FileDownloadInfo().url("safestorage://url2")).key("key2");

        var file1ByteArray = new byte[1024];
        var file2ByteArray = new byte[maxMessageSizeMb * 1000000];

        var outputStream1 = new ByteArrayOutputStream();
        var outputStream2 = new ByteArrayOutputStream();

        outputStream1.writeBytes(file1ByteArray);
        outputStream2.writeBytes(file2ByteArray);

        when(pnPecConfigurationProperties.getAttachmentRule()).thenReturn("LIMIT");
        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(file1, file2));
        when(downloadCall.downloadFile(file1.getDownload().getUrl())).thenReturn(Mono.just(outputStream1));
        when(downloadCall.downloadFile(file2.getDownload().getUrl())).thenReturn(Mono.just(outputStream2));
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        ArgumentCaptor<SendMail> argumentCaptor = ArgumentCaptor.forClass(SendMail.class);
        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        verify(arubaCall, times(1)).sendMail(argumentCaptor.capture());
        assertTrue(argumentCaptor.getValue().getData().getBytes().length < maxMessageSizeMb);
    }

    @Test
    void lavorazionePecMaxAttachmentsSizeExceeded_First() {
        var requestDto = buildRequestDto();
        var clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        var sendMailResponse = new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        var file1 = new FileDownloadResponse().download(new FileDownloadInfo().url("safestorage://url1")).key("key1");
        var file2 = new FileDownloadResponse().download(new FileDownloadInfo().url("safestorage://url2")).key("key2");

        var file1ByteArray = new byte[1024];
        var file2ByteArray = new byte[maxMessageSizeMb * 1000000];

        var outputStream1 = new ByteArrayOutputStream();
        var outputStream2 = new ByteArrayOutputStream();

        outputStream1.writeBytes(file1ByteArray);
        outputStream2.writeBytes(file2ByteArray);

        when(pnPecConfigurationProperties.getAttachmentRule()).thenReturn("FIRST");
        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(file1, file2));
        when(downloadCall.downloadFile(file1.getDownload().getUrl())).thenReturn(Mono.just(outputStream1));
        when(downloadCall.downloadFile(file2.getDownload().getUrl())).thenReturn(Mono.just(outputStream2));
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        ArgumentCaptor<SendMail> argumentCaptor = ArgumentCaptor.forClass(SendMail.class);
        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        verify(arubaCall, times(1)).sendMail(argumentCaptor.capture());
        assertTrue(argumentCaptor.getValue().getData().getBytes().length < maxMessageSizeMb);
    }

    @Test
    void lavorazionePecMaxAttachmentsSizeExceeded_Ko() {
        var requestDto = buildRequestDto();
        var clientId = PEC_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();
        var requestId = PEC_PRESA_IN_CARICO_INFO.getRequestIdx();

        var sendMailResponse = new SendMailResponse();
        sendMailResponse.setErrstr("errorstr");

        var file = new FileDownloadResponse().download(new FileDownloadInfo().url("safestorage://url1")).key("key");
        var fileByteArray = new byte[(maxMessageSizeMb * 1000000) + 1024];
        var outputStream = new ByteArrayOutputStream();
        outputStream.writeBytes(fileByteArray);

        when(attachmentService.getAllegatiPresignedUrlOrMetadata(anyList(), any(), eq(false))).thenReturn(Flux.just(file));
        when(downloadCall.downloadFile(file.getDownload().getUrl())).thenReturn(Mono.just(outputStream));
        when(arubaCall.sendMail(any(SendMail.class))).thenReturn(Mono.just(sendMailResponse));
        when(gestoreRepositoryCall.setMessageIdInRequestMetadata(clientId, requestId)).thenReturn(Mono.just(requestDto));

        Mono<SendMessageResponse> response = pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(pecService, times(1)).sendNotificationOnStatusQueue(eq(PEC_PRESA_IN_CARICO_INFO), eq(RETRY.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

}