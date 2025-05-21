package it.pagopa.pn.ec.email.service;

import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.service.SesService;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadInfo;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pn.ec.sqs.SqsTimeoutProvider;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeoutException;

import static it.pagopa.pn.ec.commons.constant.Status.SENT;
import static it.pagopa.pn.ec.email.testutils.DigitalCourtesyMailRequestFactory.createMailRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@SpringBootTestWebEnv

class EmailServiceTimeoutTest {

    @SpyBean
    private EmailService emailService;

    @SpyBean
    private SesService sesService;

    @MockBean
    private DownloadCall downloadCall;

    @MockBean
    private SqsTimeoutProvider sqsTimeoutProvider;


    private static final String QUEUE_NAME = "queue";
    private static final Duration TIMEOUT_INACTIVE_DURATION = Duration.ofSeconds(86400);


    private static final EmailPresaInCaricoInfo EMAIL_PRESA_IN_CARICO_INFO = EmailPresaInCaricoInfo.builder()
            .requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalCourtesyMailRequest(
                    createMailRequest(0))
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

    @Test
    void lavorazioneRichiestaShortTimeout() {
        when(downloadCall.downloadFile(FILE_DOWNLOAD_RESPONSE.getDownload().getUrl()))
                .thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(sesService.send(any(EmailField.class)))
                .thenReturn(Mono.delay(Duration.ofSeconds(2))
                        .then(Mono.just(SendRawEmailResponse.builder().build())));
        when(sqsTimeoutProvider.getTimeoutForQueue(anyString()))
                .thenReturn(Duration.ofMillis(1));

        Mono<SendMessageResponse> lavorazioneRichiesta = emailService.lavorazioneRichiesta(EMAIL_PRESA_IN_CARICO_INFO, QUEUE_NAME);
        StepVerifier.create(lavorazioneRichiesta).expectErrorMatches(throwable -> throwable instanceof TimeoutException).verify();
    }

    @Test
    void lavorazioneRichiestaNoTimeout() {
        when(downloadCall.downloadFile(FILE_DOWNLOAD_RESPONSE.getDownload().getUrl()))
                .thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(sesService.send(any(EmailField.class)))
                .thenReturn(Mono.delay(Duration.ofSeconds(2))
                        .then(Mono.just(SendRawEmailResponse.builder().build())));

        when(sqsTimeoutProvider.getTimeoutForQueue(anyString()))
                .thenReturn(TIMEOUT_INACTIVE_DURATION);
        Mono<SendMessageResponse> lavorazioneRichiesta = emailService.lavorazioneRichiesta(EMAIL_PRESA_IN_CARICO_INFO, QUEUE_NAME);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();

        verify(emailService, times(1)).sendNotificationOnStatusQueue(eq(EMAIL_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

    }

    @Test
    void lavorazioneRichiestaLongTimeout() {
        when(downloadCall.downloadFile(FILE_DOWNLOAD_RESPONSE.getDownload().getUrl()))
                .thenReturn(Mono.just(new ByteArrayOutputStream()));
        when(sesService.send(any(EmailField.class)))
                .thenReturn(Mono.delay(Duration.ofSeconds(2))
                        .then(Mono.just(SendRawEmailResponse.builder().build())));

        when(sqsTimeoutProvider.getTimeoutForQueue(anyString()))
                .thenReturn(Duration.ofSeconds(60));
        Mono<SendMessageResponse> lavorazioneRichiesta = emailService.lavorazioneRichiesta(EMAIL_PRESA_IN_CARICO_INFO, QUEUE_NAME);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();

        verify(emailService, times(1)).sendNotificationOnStatusQueue(eq(EMAIL_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

    }
}


