package it.pagopa.pn.ec.scaricamentoesitipec.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.utils.EmailUtils;
import it.pagopa.pn.ec.pec.model.pojo.ArubaSecretValue;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.RicezioneEsitiPecDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.mail.MessagingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.encodeMessageId;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
public class ScaricamentoEsitiPecServiceTest {
    @SpyBean
    private ScaricamentoEsitiPecService scaricamentoEsitiPecService;
    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;
    @Autowired
    private ArubaSecretValue arubaSecretValue;
    @MockBean
    private Acknowledgment acknowledgment;
    @MockBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @MockBean
    private AuthService authService;
    @SpyBean
    private SqsService sqsService;

    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String PEC_REQUEST_IDX = "PEC_REQUEST_IDX";

    @BeforeEach
    public void initialize() {
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(new ClientConfigurationInternalDto()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"certificato", "esterno"})
    void lavorazioneEsitiPecOk(String tipoDestinatario) throws IOException, MessagingException {

        RicezioneEsitiPecDto ricezioneEsitiPecDto = buildRicezioneEsitiPecDto(tipoDestinatario);
        Mono<Void> testMono = scaricamentoEsitiPecService.lavorazioneEsitiPec(ricezioneEsitiPecDto, acknowledgment);
        var request = pecRequest();

        when(gestoreRepositoryCall.getRichiesta(eq(CLIENT_ID), eq(PEC_REQUEST_IDX))).thenReturn(Mono.just(request));
        Mockito.doReturn(Mono.just("location")).when(scaricamentoEsitiPecService).generateLocation(eq(PEC_REQUEST_IDX), eq(ricezioneEsitiPecDto.getMessage()));

        StepVerifier.create(testMono).expectComplete().verify();
        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoPecName()), any(NotificationTrackerQueueDto.class));
    }

    @Test
    void lavorazioneEsitiPecKo() throws IOException, MessagingException {

        RicezioneEsitiPecDto ricezioneEsitiPecDto = buildRicezioneEsitiPecDto("certificato");
        Mono<Void> testMono = scaricamentoEsitiPecService.lavorazioneEsitiPec(ricezioneEsitiPecDto, acknowledgment);

        StepVerifier.create(testMono).expectError().verify();
    }

    private RequestDto pecRequest() {
        var requestPersonal = new RequestPersonalDto().digitalRequestPersonal(new DigitalRequestPersonalDto());
        var requestMetadata = new RequestMetadataDto().digitalRequestMetadata(new DigitalRequestMetadataDto().channel(DigitalRequestMetadataDto.ChannelEnum.PEC));
        return new RequestDto().requestIdx(PEC_REQUEST_IDX).xPagopaExtchCxId(CLIENT_ID).requestPersonal(requestPersonal).requestMetadata(requestMetadata);
    }

    public static StringBuffer generateDaticertAccettazione(String from, String receiver, String replyTo, String subject, String gestoreMittente, String data, String orario, String messageId, String tipoDestinatario) {

        //Costruzione del daticert
        StringBuffer stringBufferContent = new StringBuffer();
        stringBufferContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");//popolare con daticert su note
        stringBufferContent.append("<postacert tipo=\"accettazione\" errore=\"nessuno\">");
        stringBufferContent.append("<intestazione>");
        stringBufferContent.append("<mittente>").append(from).append("</mittente>"); //mittente dell'email, sta nella mappa
        stringBufferContent.append("<destinatari tipo=").append("\"").append(tipoDestinatario).append("\"").append(">").append(receiver).append("</destinatari>"); //destinatario dell'email, sta nella mappa
        stringBufferContent.append("<risposte>").append(replyTo).append("</risposte>"); //nel messaggio che uso per popolare la mappa c'è un reply-to
        stringBufferContent.append("<oggetto>").append(subject).append("</oggetto>"); //oggetto dell'email, sta nella mappa
        stringBufferContent.append("</intestazione>");
        stringBufferContent.append("<dati>");
        stringBufferContent.append("<gestore-emittente>").append(gestoreMittente).append("</gestore-emittente>"); //da inventare = "mock-pec" costante
        stringBufferContent.append("<data zona=\"+0200\">"); //lasciare così
        stringBufferContent.append("<giorno>").append(data).append("</giorno>"); //impostare in base all'ora
        stringBufferContent.append("<ora>").append(orario).append("</ora>"); //impostare in base all'ora
        stringBufferContent.append("</data>");
        stringBufferContent.append("<identificativo>").append(generateRandomString(64)).append("</identificativo>"); //stringa random 64 caratteri
        stringBufferContent.append("<msgid>").append(messageId).append("</msgid>"); //msgid della mappa, nella forma url encoded. fare url encode della stringa
        stringBufferContent.append("</dati>");
        stringBufferContent.append("</postacert>");

        return stringBufferContent;
    }

    public static String generateRandomString(int length) {
        Random random = new Random();

        // Use the nextBytes() method to generate a random sequence of bytes.
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);

        // Convert the bytes to a string using the Base64 encoding.
        return Base64.getEncoder().encodeToString(bytes);
    }

    private RicezioneEsitiPecDto buildRicezioneEsitiPecDto(String tipoDestinatario) throws MessagingException, IOException {
        String msgId = "-"+encodeMessageId(CLIENT_ID, PEC_REQUEST_IDX)+"-";
        var daticertBytes = generateDaticertAccettazione("from", "receiverAddress@pagopa.it", "replyTo", "subject", "gestoreMittente", "03/11/1999", "00:00:00", msgId, tipoDestinatario).toString().getBytes();
        ByteArrayOutputStream daticertOutput = new ByteArrayOutputStream();
        daticertOutput.write(daticertBytes);

        EmailField emailField = EmailField.builder()
                .msgId("messageId")
                .from(arubaSecretValue.getPecUsername())
                .to("to")
                .subject("subject")
                .text("text")
                .contentType(PLAIN.getValue())
                .emailAttachments(List.of(EmailAttachment.builder().nameWithExtension("daticert.xml").url("url").content(daticertOutput).build()))
                .build();


        var emailOutput = new ByteArrayOutputStream();
        EmailUtils.getMimeMessage(emailField).writeTo(emailOutput);

        return RicezioneEsitiPecDto.builder().retry(0).receiversDomain("receiverDomain@pagopa.it").messageID("messageID").message(emailOutput.toByteArray()).build();
    }

}
