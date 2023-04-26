package it.pagopa.pn.ec.email.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.ses.SesSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.AttachmentNotAvailableException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.AttachmentService;
import it.pagopa.pn.ec.commons.service.SesService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.email.configurationproperties.EmailSqsQueueName;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadInfo;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static it.pagopa.pn.ec.commons.constant.Status.RETRY;
import static it.pagopa.pn.ec.commons.constant.Status.SENT;
import static it.pagopa.pn.ec.email.testutils.DigitalCourtesyMailRequestFactory.createMailRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class EmailServiceTest {

    @SpyBean
    private EmailService emailService;

    @Autowired
    private EmailSqsQueueName emailSqsQueueName;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @SpyBean
    private SqsServiceImpl sqsService;

    @SpyBean
    private SesService sesService;

    @MockBean
    private FileCall fileCall;

    @SpyBean
    private AttachmentServiceImpl attachmentService;

    @MockBean
    private DownloadCall downloadCall;

    @Mock
    private Acknowledgment acknowledgment;

    private static final EmailPresaInCaricoInfo EMAIL_PRESA_IN_CARICO_INFO = EmailPresaInCaricoInfo.builder()
            .requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalCourtesyMailRequest(
                    createMailRequest(0))
            .build();

    private static final EmailPresaInCaricoInfo EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH = EmailPresaInCaricoInfo.builder()
            .requestIdx(
                    DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalCourtesyMailRequest(
                    createMailRequest(1))
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


    /**
     * <h3>EMAILLR.100.1</h3>
     * <b>Precondizione:</b>
     *   <ol>
     *     <li>Pull payload dalla coda MAIL</li>
     *     <li>Allegato assente</li>
     *   </ol>
     * <b>Passi aggiuntivi:</b>
     *   <ol>
     *     <li>Invio MAIL con SES -> OK</li>
     *   </ol>
     * <b>Risultato atteso:</b> Pubblicazione sulla coda Notification Tracker -> OK</li>
     */
    @Test
    void lavorazioneRichiestaOk() {

        var attachments = EMAIL_PRESA_IN_CARICO_INFO.getDigitalCourtesyMailRequest().getAttachmentsUrls();
        var clientId = EMAIL_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId();

        when(downloadCall.downloadFile(FILE_DOWNLOAD_RESPONSE.getDownload().getUrl())).thenReturn(Mono.just(new ByteArrayOutputStream()));

        when(sesService.send(any(EmailField.class))).thenReturn(Mono.just(SendRawEmailResponse.builder().build()));

        Mono<SendMessageResponse> lavorazioneRichiesta=emailService.lavorazioneRichiesta(EMAIL_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();

        verify(emailService, times(1)).sendNotificationOnStatusQueue(eq(EMAIL_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

    }

    /**
     * <h3>EMAILLR.100.2</h3>
     * <b>Precondizione:</b>
     *   <ol>
     *     <li>Pull payload dalla coda MAIL</li>
     *     <li>Allegato presente</li>
     *   </ol>
     * <b>Passi aggiuntivi:</b>
     *   <ol>
     *     <li>Servizio SS -> OK</li>
     *     <li>Download allegato -> OK</li>
     *     <li>Invio MAIL con SES -> OK</li>
     *   </ol>
     * <b>Risultato atteso:</b> Pubblicazione sulla coda Notification Tracker -> OK</li>
     */
    @Test
    void lavorazioneRichiestaWithAttachOk() {

        when(fileCall.getFile(any(), any(), eq(false))).thenReturn(Mono.just(FILE_DOWNLOAD_RESPONSE));

        when(downloadCall.downloadFile(FILE_DOWNLOAD_RESPONSE.getDownload().getUrl())).thenReturn(Mono.just(new ByteArrayOutputStream()));

        when(sesService.send(any(EmailField.class))).thenReturn(Mono.just(SendRawEmailResponse.builder().build()));

        Mono<SendMessageResponse> lavorazioneRichiesta=emailService.lavorazioneRichiesta(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();

        verify(emailService, times(1)).sendNotificationOnStatusQueue(eq(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

    }

    /**
     * <h3>EMAILLR.100.3</h3>
     * <b>Precondizione:</b>
     *   <ol>
     *     <li>Pull payload dalla coda MAIL</li>
     *     <li>Allegato presente</li>
     *   </ol>
     * <b>Passi aggiuntivi:</b>
     *   <ol>
     *     <li>Servizio SS -> OK</li>
     *     <li>Download allegato (retry > allowed) -> KO</li>
     *   </ol>
     * <b>Risultato atteso:</b> Pubblicazione sulla coda Errori MAIL -> OK</li>
     */
    @Test
    void lavorazioneRichiestaWithAttachDownKo() {

        when(fileCall.getFile(any(), any(), eq(false))).thenReturn(Mono.error(new AttachmentNotAvailableException("")));

        when(downloadCall.downloadFile(FILE_DOWNLOAD_RESPONSE.getDownload().getUrl())).thenReturn(Mono.just(new ByteArrayOutputStream()));

        when(sesService.send(any(EmailField.class))).thenReturn(Mono.just(SendRawEmailResponse.builder().build()));

        Mono<SendMessageResponse> lavorazioneRichiesta=emailService.lavorazioneRichiesta(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();

        verify(emailService, times(1)).sendNotificationOnStatusQueue(eq(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH), eq(RETRY.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        verify(emailService, times(1)).sendNotificationOnErrorQueue(eq(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH));

    }

    /**
     * <h3>EMAILLR.100.5</h3>
     * <b>Precondizione:</b>
     *   <ol>
     *     <li>Pull payload dalla coda MAIL</li>
     *     <li>Allegato assente</li>
     *   </ol>
     * <b>Passi aggiuntivi:</b>
     *   <ol>
     *     <li>Invio MAIL con SES -> OK</li>
     *     <li>Pubblicazione sulla coda Notification Tracker -> KO</li>
     *   </ol>
     * <b>Risultato atteso:</b> Pubblicazione sulla coda Errori MAIL -> OK</li>
     * <li><b>Note:</b> Il payload pubblicato sulla coda notificherà al flow di retry di riprovare solamente la pubblicazione sulla coda
     * "Notification Tracker"
     */
    @Test
    void lavorazioneRichiestaNtKo() {

        when(fileCall.getFile(any(), any(), eq(false))).thenReturn(Mono.just(FILE_DOWNLOAD_RESPONSE));

        when(downloadCall.downloadFile(FILE_DOWNLOAD_RESPONSE.getDownload().getUrl())).thenReturn(Mono.just(new ByteArrayOutputStream()));

        when(sesService.send(any(EmailField.class))).thenReturn(Mono.just(SendRawEmailResponse.builder().build()));

        when(sqsService.send(eq(notificationTrackerSqsName.statoEmailName()), any(NotificationTrackerQueueDto.class))).thenReturn(Mono.error(new SqsClientException("")));

        Mono<SendMessageResponse> lavorazioneRichiesta=emailService.lavorazioneRichiesta(EMAIL_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();

        verify(emailService, times(1)).sendNotificationOnErrorQueue(eq(EMAIL_PRESA_IN_CARICO_INFO));

    }

    /**
     * <h3>EMAILLR.100.6</h3>
     * <b>Precondizione:</b>
     *   <ol>
     *     <li>Pull payload dalla coda MAIL</li>
     *     <li>Allegato assente</li>
     *   </ol>
     * <b>Passi aggiuntivi:</b>
     *   <ol>
     *     <li>Invio MAIL con SES (retry > allowed) -> KO</li>
     *   </ol>
     * <b>Risultato atteso:</b> Pubblicazione sulla coda Errori MAIL -> OK</li>
     */
    @Test
    void lavorazioneRichiestaSesKo() {

        when(fileCall.getFile(any(), any(), eq(false))).thenReturn(Mono.just(FILE_DOWNLOAD_RESPONSE));

        when(downloadCall.downloadFile(FILE_DOWNLOAD_RESPONSE.getDownload().getUrl())).thenReturn(Mono.just(new ByteArrayOutputStream()));

        when(sesService.send(any(EmailField.class))).thenReturn(Mono.error(new SesSendException()));

        Mono<SendMessageResponse> lavorazioneRichiesta=emailService.lavorazioneRichiesta(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH);
        StepVerifier.create(lavorazioneRichiesta).expectNextCount(1).verifyComplete();

        verify(emailService, times(1)).sendNotificationOnErrorQueue(eq(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH));
        verify(emailService, times(1)).sendNotificationOnStatusQueue(eq(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH), eq(RETRY.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));

    }

}