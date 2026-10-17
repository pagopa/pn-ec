package it.pagopa.pn.ec.commons.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.exception.whatsapp.WhatsappSendException;
import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import it.pagopa.pn.ec.rest.v1.dto.WhatsappTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.socialmessaging.SocialMessagingAsyncClient;
import software.amazon.awssdk.services.socialmessaging.model.SendWhatsAppMessageRequest;
import software.amazon.awssdk.services.socialmessaging.model.SendWhatsAppMessageResponse;
import software.amazon.awssdk.services.socialmessaging.model.ThrottledRequestException;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EumSocialServiceImplTest {

    private static final String PHONE_NUMBER_ID = "phone-number-id-01234567890123456789012345678901";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SocialMessagingAsyncClient client;
    private EumSocialServiceImpl service;

    @BeforeEach
    void setUp() {
        client = mock(SocialMessagingAsyncClient.class);
        PnEcConfig config = new PnEcConfig();
        config.getWhatsapp().setOriginationPhoneNumberId(PHONE_NUMBER_ID);
        config.getWhatsapp().setMetaApiVersion("v20.0");
        service = new EumSocialServiceImpl(client, config, objectMapper);
    }

    @Test
    void buildMessageObjectForUtilityTemplate() throws Exception {
        WhatsappTemplate template = new WhatsappTemplate()
                .name("send_avviso_notifica_digitale")
                .language("it")
                .bodyParameters(List.of("Comune di \"Sant'Angelo\"", "IUN-1"));

        JsonNode message = objectMapper.readTree(service.buildMessageObject("+393331234567", template, "cxId~req1"));

        assertEquals("whatsapp", message.get("messaging_product").asText());
        assertEquals("393331234567", message.get("to").asText());
        assertEquals("template", message.get("type").asText());
        assertEquals("cxId~req1", message.get("biz_opaque_callback_data").asText());
        assertEquals("send_avviso_notifica_digitale", message.at("/template/name").asText());
        assertEquals("it", message.at("/template/language/code").asText());
        JsonNode components = message.at("/template/components");
        assertEquals(1, components.size());
        assertEquals("body", components.get(0).get("type").asText());
        assertEquals("Comune di \"Sant'Angelo\"", components.get(0).at("/parameters/0/text").asText());
        assertEquals("IUN-1", components.get(0).at("/parameters/1/text").asText());
    }

    @Test
    void buildMessageObjectForAuthenticationTemplateWithCopyCodeButton() throws Exception {
        WhatsappTemplate template = new WhatsappTemplate()
                .name("send_codice_verifica")
                .language("en")
                .bodyParameters(List.of("12345"))
                .buttonParameters(List.of("12345"));

        JsonNode components = objectMapper.readTree(service.buildMessageObject("00393331234567", template, null))
                .at("/template/components");

        assertEquals(2, components.size());
        assertEquals("button", components.get(1).get("type").asText());
        assertEquals("url", components.get(1).get("sub_type").asText());
        assertEquals("0", components.get(1).get("index").asText());
        assertEquals("12345", components.get(1).at("/parameters/0/text").asText());
    }

    @Test
    void buildMessageObjectWithoutBodyParametersOmitsBodyComponent() throws Exception {
        WhatsappTemplate template = new WhatsappTemplate().name("send_test").language("it");
        JsonNode message = objectMapper.readTree(service.buildMessageObject("393331234567", template, ""));
        assertEquals(0, message.at("/template/components").size());
        assertFalse(message.has("biz_opaque_callback_data"));
    }

    @Test
    void callbackDataIsTruncatedToCloudApiLimit() throws Exception {
        WhatsappTemplate template = new WhatsappTemplate().name("send_test").language("it").bodyParameters(List.of("x"));
        String longCallback = "a".repeat(EumSocialServiceImpl.MAX_CALLBACK_DATA_LENGTH + 10);
        JsonNode message = objectMapper.readTree(service.buildMessageObject("393331234567", template, longCallback));
        assertEquals(EumSocialServiceImpl.MAX_CALLBACK_DATA_LENGTH, message.get("biz_opaque_callback_data").asText().length());
    }

    @ParameterizedTest
    @CsvSource({"+393331234567,393331234567", "00393331234567,393331234567", "39 333 123-4567,393331234567"})
    void normalizePhoneNumber(String input, String expected) {
        assertEquals(expected, EumSocialServiceImpl.normalizePhoneNumber(input));
    }

    @Test
    void normalizePhoneNumberRejectsInvalidValues() {
        assertThrows(WhatsappSendException.class, () -> EumSocialServiceImpl.normalizePhoneNumber("abc"));
        assertThrows(WhatsappSendException.class, () -> EumSocialServiceImpl.normalizePhoneNumber(""));
    }

    @Test
    void sendTemplateMessageOk() {
        when(client.sendWhatsAppMessage(any(SendWhatsAppMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(SendWhatsAppMessageResponse.builder().messageId("msg-1").build()));

        WhatsappTemplate template = new WhatsappTemplate().name("send_test").language("it").bodyParameters(List.of("x"));
        StepVerifier.create(service.sendTemplateMessage("+393331234567", template, "cb")).expectNext("msg-1").verifyComplete();

        ArgumentCaptor<SendWhatsAppMessageRequest> captor = ArgumentCaptor.forClass(SendWhatsAppMessageRequest.class);
        verify(client).sendWhatsAppMessage(captor.capture());
        assertEquals(PHONE_NUMBER_ID, captor.getValue().originationPhoneNumberId());
        assertEquals("v20.0", captor.getValue().metaApiVersion());
        assertTrue(captor.getValue().message().asUtf8String().contains("\"to\":\"393331234567\""));
    }

    @Test
    void sendTemplateMessageKoIsWrapped() {
        when(client.sendWhatsAppMessage(any(SendWhatsAppMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(ThrottledRequestException.builder().message("throttled").build()));

        WhatsappTemplate template = new WhatsappTemplate().name("send_test").language("it").bodyParameters(List.of("x"));
        StepVerifier.create(service.sendTemplateMessage("+393331234567", template, "cb"))
                .expectError(WhatsappSendException.class)
                .verify();
    }

    @Test
    void sendTemplateMessageWithoutTemplateFails() {
        StepVerifier.create(service.sendTemplateMessage("+393331234567", null, "cb"))
                .expectError(WhatsappSendException.class)
                .verify();
        verifyNoInteractions(client);
    }
}
