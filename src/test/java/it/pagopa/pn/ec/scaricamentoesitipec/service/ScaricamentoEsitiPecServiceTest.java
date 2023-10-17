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
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.mail.MessagingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.encodeMessageId;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.scaricamentoesitipec.utils.PecUtils.generateDaticertAccettazione;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
public class ScaricamentoEsitiPecServiceTest {
    @SpyBean
    private LavorazioneEsitiPecService lavorazioneEsitiPecService;
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
        Mono<Void> testMono = lavorazioneEsitiPecService.lavorazioneEsitiPec(ricezioneEsitiPecDto, acknowledgment);
        var request = pecRequest();

        when(gestoreRepositoryCall.getRichiesta(eq(CLIENT_ID), eq(PEC_REQUEST_IDX))).thenReturn(Mono.just(request));
        Mockito.doReturn(Mono.just("location")).when(lavorazioneEsitiPecService).generateLocation(eq(PEC_REQUEST_IDX), eq(ricezioneEsitiPecDto.getMessage()));

        StepVerifier.create(testMono).expectComplete().verify();
        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoPecName()), any(NotificationTrackerQueueDto.class));
    }

    @Test
    void lavorazioneEsitiPecKo() throws IOException, MessagingException {

        RicezioneEsitiPecDto ricezioneEsitiPecDto = buildRicezioneEsitiPecDto("certificato");
        Mono<Void> testMono = lavorazioneEsitiPecService.lavorazioneEsitiPec(ricezioneEsitiPecDto, acknowledgment);

        StepVerifier.create(testMono).expectError().verify();
    }

    private RequestDto pecRequest() {
        var requestPersonal = new RequestPersonalDto().digitalRequestPersonal(new DigitalRequestPersonalDto());
        var requestMetadata = new RequestMetadataDto().digitalRequestMetadata(new DigitalRequestMetadataDto().channel(DigitalRequestMetadataDto.ChannelEnum.PEC));
        return new RequestDto().requestIdx(PEC_REQUEST_IDX).xPagopaExtchCxId(CLIENT_ID).requestPersonal(requestPersonal).requestMetadata(requestMetadata);
    }

    private RicezioneEsitiPecDto buildRicezioneEsitiPecDto(String tipoDestinatario) throws MessagingException, IOException {
        String msgId = "-" + encodeMessageId(CLIENT_ID, PEC_REQUEST_IDX) + "-";
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
