package it.pagopa.pn.ec.scaricamentoesitipec.scheduler;

import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.utils.EmailUtils;
import it.pagopa.pn.library.pec.model.pojo.PnEcPecMessage;
import it.pagopa.pn.library.pec.service.DaticertService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pn.library.pec.service.ArubaService;
import it.pagopa.pn.library.pec.service.PnEcPecService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.encodeMessageId;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.scaricamentoesitipec.utils.PecUtils.generateDaticertAccettazione;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class ScaricamentoEsitiPecSchedulerTest {

    @MockBean
    private ArubaService arubaService;

    @MockBean
    private com.namirial.pec.library.service.PnPecServiceImpl namirialService;

    @SpyBean
    private PnEcPecService pnPecService;


    @SpyBean
    private DaticertService daticertService;

    @MockBean
    private GestoreRepositoryCall gestoreRepositoryCall;

    @Autowired
    private SqsService sqsService;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @Autowired
    private TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;
    @Autowired
    private ScaricamentoEsitiPecScheduler scaricamentoEsitiPecScheduler;

    @Value("${aruba.pec.username}")
    private String pecUsername;

    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String PEC_REQUEST_IDX = "PEC_REQUEST_IDX";
    private static final String UUID = "UUID";
    @Test
    void scaricamentoEsitiPecOk() {

    }


    @Test
    void lavorazioneArubaEsitoOk() throws MessagingException, IOException {

        PnEcPecMessage message = new PnEcPecMessage()
                .message(buildRicezioneEsitiPecDto("tipoPostacert", "tipoDestinatario", true).toByteArray())
                .providerName("aruba");

        when(arubaService.markMessageAsRead(any())).thenReturn(Mono.empty());

        StepVerifier.create(scaricamentoEsitiPecScheduler.lavorazioneEsito(message,UUID))
                .expectComplete()
                .verify();

        verify(pnPecService, times(1)).markMessageAsRead(any(),any());
    }
    @Test
    void lavorazioneEsitiNamirialEsitoOk() throws MessagingException, IOException {
        PnEcPecMessage message = new PnEcPecMessage()
                .message(buildRicezioneEsitiPecDto("tipoPostacert", "tipoDestinatario", true).toByteArray())
                .providerName("namirial");

        when(namirialService.markMessageAsRead(any())).thenReturn(Mono.empty());

        StepVerifier.create(scaricamentoEsitiPecScheduler.lavorazioneEsito(message,UUID))
                .expectComplete()
                .verify();

        verify(pnPecService, times(1)).markMessageAsRead(any(),any());
    }

    @Test
    void lavorazioneEsitoArubaNoDatiCert() throws MessagingException, IOException {
        PnEcPecMessage message = new PnEcPecMessage()
                .message(buildRicezioneEsitiPecDto("tipoPostacert", "tipoDestinatario", false).toByteArray())
                .providerName("aruba");

        when(arubaService.markMessageAsRead(any())).thenReturn(Mono.empty());
        StepVerifier.create(scaricamentoEsitiPecScheduler.lavorazioneEsito(message,UUID))
                .expectComplete()
                .verify();

        verify(pnPecService, times(1)).markMessageAsRead(any(),any());
    }

    @Test
    void lavorazioneEsitoNamirialNoDatiCert() throws MessagingException, IOException {
        PnEcPecMessage message = new PnEcPecMessage()
                .message(buildRicezioneEsitiPecDto("tipoPostacert", "tipoDestinatario", false).toByteArray())
                .providerName("namirial");

        when(namirialService.markMessageAsRead(any())).thenReturn(Mono.empty());
        StepVerifier.create(scaricamentoEsitiPecScheduler.lavorazioneEsito(message,UUID))
                .expectComplete()
                .verify();

        verify(pnPecService, times(1)).markMessageAsRead(any(),any());
    }

    @Test
    void lavorazioneEsitoNamirialMessageIdNull() throws MessagingException, IOException {
        ByteArrayOutputStream messageBA = buildRicezioneEsitiPecDto( "tipoPostacert", "tipoDestinatario", true);
        var mimeMessage = EmailUtils.getMimeMessage(messageBA.toByteArray());
        mimeMessage.removeHeader("Message-ID");
        var mimeMessageBA = EmailUtils.getMimeMessageByteArray(mimeMessage);

        PnEcPecMessage message = new PnEcPecMessage()
                .message(mimeMessageBA)
                .providerName("namirial");

        when(namirialService.markMessageAsRead(any())).thenReturn(Mono.empty());

        StepVerifier.create(scaricamentoEsitiPecScheduler.lavorazioneEsito(message,UUID))
                .expectComplete()
                .verify();

        verify(pnPecService, never()).markMessageAsRead(any(),any());
        verify(daticertService, never()).getPostacertFromByteArray(any());
    }

    private ByteArrayOutputStream buildRicezioneEsitiPecDto(String tipoPostacert, String tipoDestinatario, boolean hasDatiCert) throws MessagingException, IOException {
        String msgId = URLEncoder.encode("<" + encodeMessageId(CLIENT_ID, PEC_REQUEST_IDX) + ">", StandardCharsets.UTF_8);
        var daticertBytes = generateDaticertAccettazione(tipoPostacert,"sender@dgsspa.com", "receiverAddress@pagopa.it", "replyTo", "subject", "gestoreMittente", "03/11/1999", "00:00:00", msgId, tipoDestinatario).toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream daticertOutput = new ByteArrayOutputStream();
        daticertOutput.write(daticertBytes);

        EmailField emailField = EmailField.builder()
                .msgId("messageId")
                .from(pecUsername)
                .to("to")
                .subject("subject")
                .text("text")
                .contentType(PLAIN.getValue())
                .emailAttachments(hasDatiCert ? List.of(EmailAttachment.builder().nameWithExtension("daticert.xml").url("url").content(daticertOutput).build()) : List.of())
                .build();


        var emailOutput = new ByteArrayOutputStream();
        EmailUtils.getMimeMessage(emailField).writeTo(emailOutput);

        return emailOutput;

    }

}
