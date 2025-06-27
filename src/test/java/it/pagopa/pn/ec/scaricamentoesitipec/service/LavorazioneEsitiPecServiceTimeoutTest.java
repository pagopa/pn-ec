package it.pagopa.pn.ec.scaricamentoesitipec.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.commons.utils.EmailUtils;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.RicezioneEsitiPecDto;
import it.pagopa.pn.ec.sqs.SqsTimeoutProvider;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import jakarta.mail.MessagingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.encodeMessageId;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.*;
import static it.pagopa.pn.ec.scaricamentoesitipec.utils.PecUtils.generateDaticertAccettazione;
import static it.pagopa.pn.library.pec.utils.PnPecUtils.ARUBA_PROVIDER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@DirtiesContext
class LavorazioneEsitiPecServiceTimeoutTest {
    @SpyBean
    private LavorazioneEsitiPecService lavorazioneEsitiPecService;
    @MockBean
    private Acknowledgment acknowledgment;
    @MockBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @MockBean
    private AuthService authService;
    @MockBean
    private SqsTimeoutProvider sqsTimeoutProvider;
    @MockBean
    private StatusPullService statusPullService;
    @Value("${pn.ec.storage.sqs.messages.staging.bucket}")
    String storageSqsMessagesStagingBucket;
    @Value("${aruba.pec.username}")
    private String pecUsername;
    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String PEC_REQUEST_IDX = "PEC_REQUEST_IDX";
    private static final Duration TIMEOUT_INACTIVE_DURATION = Duration.ofSeconds(86400);


    @BeforeEach
    void initialize() {
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(new ClientConfigurationInternalDto()));
    }

    @Test
    void lavorazioneEsitiPecShortTimeoutTest() throws IOException,MessagingException {
        RicezioneEsitiPecDto ricezioneEsitiPecDto = buildRicezioneEsitiPecDto(ACCETTAZIONE, "certificato", ARUBA_PROVIDER);

        when(statusPullService.pecPullService(any(), any()))
                .thenReturn(Mono.just(mock(LegalMessageSentDetails.class)));

        when(gestoreRepositoryCall.getRichiesta(any(), any()))
                .thenReturn(Mono.just(mock(RequestDto.class)));

        when(sqsTimeoutProvider.getTimeoutForQueue(anyString()))
                .thenReturn(Duration.ofMillis(1));

        doReturn(Mono.just("result")
                .delayElement(Duration.ofSeconds(2)))
                .when(lavorazioneEsitiPecService)
                .generateLocation(any(),any());

        StepVerifier.create(lavorazioneEsitiPecService.lavorazioneEsitiPec(ricezioneEsitiPecDto,acknowledgment))
                .expectErrorMatches(throwable -> throwable instanceof TimeoutException)
                .verify();
    }


    @Test
    void lavorazioneEsitiPecNoTimeoutTest() throws Exception {
        RicezioneEsitiPecDto dto = buildRicezioneEsitiPecDto(ACCETTAZIONE, "certificato", ARUBA_PROVIDER);


        when(statusPullService.pecPullService(any(), any()))
                .thenReturn(Mono.just(mock(LegalMessageSentDetails.class)));

        when(gestoreRepositoryCall.getRichiesta(any(), any()))
                .thenReturn(Mono.just(mock(RequestDto.class)));

        when(sqsTimeoutProvider.getTimeoutForQueue(anyString()))
                .thenReturn(TIMEOUT_INACTIVE_DURATION);

        doReturn(Mono.just("result"))
                .when(lavorazioneEsitiPecService)
                .generateLocation(any(), any());

        StepVerifier.create(lavorazioneEsitiPecService.lavorazioneEsitiPec(dto, acknowledgment))
                .verifyComplete();
    }

    @Test
    void lavorazioneEsitiPecLongTimeoutTest() throws Exception {
        RicezioneEsitiPecDto dto = buildRicezioneEsitiPecDto(ACCETTAZIONE, "certificato", ARUBA_PROVIDER);

        when(sqsTimeoutProvider.getTimeoutForQueue(anyString()))
                .thenReturn(Duration.ofSeconds(5));

        when(statusPullService.pecPullService(any(), any()))
                .thenReturn(Mono.just(mock(LegalMessageSentDetails.class)));

        when(gestoreRepositoryCall.getRichiesta(any(), any()))
                .thenReturn(Mono.just(mock(RequestDto.class)));


        doReturn(Mono.just("result")
                .delayElement(Duration.ofMillis(500)))
                .when(lavorazioneEsitiPecService)
                .generateLocation(any(), any());

        StepVerifier.create(lavorazioneEsitiPecService.lavorazioneEsitiPec(dto, acknowledgment))
                .verifyComplete();
    }

    private RicezioneEsitiPecDto buildRicezioneEsitiPecDto(String tipoPostacert, String tipoDestinatario, String providerName) throws MessagingException, IOException {
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
                .emailAttachments(List.of(EmailAttachment.builder().nameWithExtension("daticert.xml").url("url").content(daticertOutput).build()))
                .build();


        var emailOutput = new ByteArrayOutputStream();
        EmailUtils.getMimeMessage(emailField).writeTo(emailOutput);

        return RicezioneEsitiPecDto.builder().retry(0).receiversDomain("receiverDomain@pagopa.it").messageID("messageID").message(emailOutput.toByteArray()).providerName(providerName).build();
    }



}