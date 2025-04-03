package it.pagopa.pn.ec.scaricamentoesitipec.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.S3Service;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.utils.EmailUtils;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.RicezioneEsitiPecDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pn.library.pec.model.pojo.ArubaPostacert;
import it.pagopa.pn.library.pec.model.pojo.PnPostacert;
import it.pagopa.pn.library.pec.service.DaticertService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import jakarta.mail.MessagingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static it.pagopa.pn.ec.commons.utils.EmailUtils.getAttachmentFromMimeMessage;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.encodeMessageId;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.*;
import static it.pagopa.pn.ec.scaricamentoesitipec.utils.PecUtils.generateDaticertAccettazione;
import static it.pagopa.pn.library.pec.utils.PnPecUtils.ARUBA_PROVIDER;
import static it.pagopa.pn.library.pec.utils.PnPecUtils.NAMIRIAL_PROVIDER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class ScaricamentoEsitiPecServiceTest {
    @SpyBean
    private LavorazioneEsitiPecService lavorazioneEsitiPecService;
    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;
    @MockBean
    private Acknowledgment acknowledgment;
    @MockBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @MockBean
    private AuthService authService;
    @SpyBean
    private SqsService sqsService;
    @SpyBean
    private DaticertService daticertService;
    @SpyBean
    private S3Service s3Service;
    @Value("${pn.ec.storage.sqs.messages.staging.bucket}")
    String storageSqsMessagesStagingBucket;
    @Value("${aruba.pec.username}")
    private String pecUsername;
    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String PEC_REQUEST_IDX = "PEC_REQUEST_IDX";

    @BeforeEach
    void initialize() {
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(new ClientConfigurationInternalDto()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"certificato", "esterno"})
    void lavorazioneEsitiPecOk(String tipoDestinatario) throws IOException, MessagingException {

        RicezioneEsitiPecDto ricezioneEsitiPecDto = buildRicezioneEsitiPecDto(ACCETTAZIONE, tipoDestinatario, ARUBA_PROVIDER);
        var request = pecRequest();

        when(gestoreRepositoryCall.getRichiesta(CLIENT_ID, PEC_REQUEST_IDX)).thenReturn(Mono.just(request));
        Mockito.doReturn(Mono.just("location")).when(lavorazioneEsitiPecService).generateLocation(eq(PEC_REQUEST_IDX), eq(ricezioneEsitiPecDto.getMessage()));

        Mono<Void> testMono = lavorazioneEsitiPecService.lavorazioneEsitiPec(ricezioneEsitiPecDto, acknowledgment);
        StepVerifier.create(testMono).expectComplete().verify();
        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoPecName()), any(NotificationTrackerQueueDto.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"certificato", "esterno"})
    void lavorazioneEsitiPec_S3Payload_Ok(String tipoDestinatario) throws IOException, MessagingException {

        String pointerFileKey = s3Service.convertAndPutObject(storageSqsMessagesStagingBucket, buildRicezioneEsitiPecDto(ACCETTAZIONE, tipoDestinatario, ARUBA_PROVIDER)).block();
        RicezioneEsitiPecDto ricezioneEsitiPecDto = RicezioneEsitiPecDto.builder().pointerFileKey(pointerFileKey).build();
        var request = pecRequest();

        when(gestoreRepositoryCall.getRichiesta(CLIENT_ID, PEC_REQUEST_IDX)).thenReturn(Mono.just(request));
        Mockito.doReturn(Mono.just("location")).when(lavorazioneEsitiPecService).generateLocation(eq(PEC_REQUEST_IDX), any());

        Mono<Void> testMono = lavorazioneEsitiPecService.lavorazioneEsitiPec(ricezioneEsitiPecDto, acknowledgment);
        StepVerifier.create(testMono).expectComplete().verify();
        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoPecName()), any(NotificationTrackerQueueDto.class));
        verify(s3Service, times(1)).getObjectAndConvert(pointerFileKey, storageSqsMessagesStagingBucket, RicezioneEsitiPecDto.class);

    }

    @Test
    void lavorazioneEsitiPec_S3Payload_NoSuchKeyKo() {

        String fileKey = "pointerFileKey.eml";
        RicezioneEsitiPecDto ricezioneEsitiPecDto = RicezioneEsitiPecDto.builder().pointerFileKey(fileKey).build();
        var request = pecRequest();

        when(gestoreRepositoryCall.getRichiesta(CLIENT_ID, PEC_REQUEST_IDX)).thenReturn(Mono.just(request));
        Mockito.doReturn(Mono.just("location")).when(lavorazioneEsitiPecService).generateLocation(eq(PEC_REQUEST_IDX), any());

        Mono<Void> testMono = lavorazioneEsitiPecService.lavorazioneEsitiPec(ricezioneEsitiPecDto, acknowledgment);
        StepVerifier.create(testMono).expectError(NoSuchKeyException.class).verify();
        verify(sqsService, never()).send(anyString(), any(NotificationTrackerQueueDto.class));
        verify(s3Service, times(1)).getObjectAndConvert(fileKey, storageSqsMessagesStagingBucket, RicezioneEsitiPecDto.class);
    }

    @Test
    void lavorazioneEsitiPecDeliveryWarn24h() throws IOException, MessagingException {

        RicezioneEsitiPecDto ricezioneEsitiPecDto = buildRicezioneEsitiPecDto(PREAVVISO_ERRORE_CONSEGNA, "certificato", ARUBA_PROVIDER);
        var request = pecRequest();

        when(gestoreRepositoryCall.getRichiesta(CLIENT_ID, PEC_REQUEST_IDX)).thenReturn(Mono.just(request));
        Mockito.doReturn(Mono.just("location")).when(lavorazioneEsitiPecService).generateLocation(eq(PEC_REQUEST_IDX), eq(ricezioneEsitiPecDto.getMessage()));

        Mono<Void> testMono = lavorazioneEsitiPecService.lavorazioneEsitiPec(ricezioneEsitiPecDto, acknowledgment);
        StepVerifier.create(testMono).expectComplete().verify();
        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoPecName()), any(NotificationTrackerQueueDto.class));

    }

    @Test
    void correzioneTipoArubaPecTest() throws MessagingException, IOException {
        RicezioneEsitiPecDto ricezioneEsitiPecDto = buildRicezioneEsitiPecDto(PREAVVISO_ERRORE_CONSEGNA, "certificato", ARUBA_PROVIDER);
        ArubaPostacert postacert = (ArubaPostacert) daticertService.getPostacertFromByteArray(getAttachmentFromMimeMessage(EmailUtils.getMimeMessage(ricezioneEsitiPecDto.getMessage()), "daticert.xml"), ARUBA_PROVIDER);
        Assertions.assertEquals(ERRORE_CONSEGNA, postacert.getTipo());
    }

    @Test
    void correzioneTipoNamirialPecTest() throws MessagingException, IOException {
        RicezioneEsitiPecDto ricezioneEsitiPecDto = buildRicezioneEsitiPecDto(PREAVVISO_ERRORE_CONSEGNA, "certificato", NAMIRIAL_PROVIDER);
        PnPostacert postacert = (PnPostacert) daticertService.getPostacertFromByteArray(getAttachmentFromMimeMessage(EmailUtils.getMimeMessage(ricezioneEsitiPecDto.getMessage()), "daticert.xml"), NAMIRIAL_PROVIDER);
        Assertions.assertEquals(PREAVVISO_ERRORE_CONSEGNA, postacert.getTipo());
    }

    @Test
    void lavorazioneEsitiPecKo() throws IOException, MessagingException {

        RicezioneEsitiPecDto ricezioneEsitiPecDto = buildRicezioneEsitiPecDto(ACCETTAZIONE, "certificato", ARUBA_PROVIDER);
        when(gestoreRepositoryCall.getRichiesta(CLIENT_ID, PEC_REQUEST_IDX)).thenReturn(Mono.error(new RepositoryManagerException.RequestNotFoundException(PEC_REQUEST_IDX)));

        Mono<Void> testMono = lavorazioneEsitiPecService.lavorazioneEsitiPec(ricezioneEsitiPecDto, acknowledgment);
        StepVerifier.create(testMono).expectError().verify();
    }



    private RequestDto pecRequest() {
        var requestPersonal = new RequestPersonalDto().digitalRequestPersonal(new DigitalRequestPersonalDto());
        var requestMetadata = new RequestMetadataDto().digitalRequestMetadata(new DigitalRequestMetadataDto().channel(DigitalRequestMetadataDto.ChannelEnum.PEC));
        return new RequestDto().requestIdx(PEC_REQUEST_IDX).xPagopaExtchCxId(CLIENT_ID).requestPersonal(requestPersonal).requestMetadata(requestMetadata);
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