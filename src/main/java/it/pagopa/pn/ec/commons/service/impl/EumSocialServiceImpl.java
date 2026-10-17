package it.pagopa.pn.ec.commons.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.pagopa.pn.ec.commons.exception.whatsapp.WhatsappSendException;
import it.pagopa.pn.ec.commons.service.EumSocialService;
import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import it.pagopa.pn.ec.rest.v1.dto.WhatsappTemplate;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.socialmessaging.SocialMessagingAsyncClient;
import software.amazon.awssdk.services.socialmessaging.model.SendWhatsAppMessageRequest;
import software.amazon.awssdk.services.socialmessaging.model.SendWhatsAppMessageResponse;

import java.util.List;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@Service
@CustomLog
public class EumSocialServiceImpl implements EumSocialService {

    /** Lunghezza massima di biz_opaque_callback_data ammessa dalla WhatsApp Cloud API. */
    static final int MAX_CALLBACK_DATA_LENGTH = 512;

    private final SocialMessagingAsyncClient socialMessagingAsyncClient;
    private final PnEcConfig.Whatsapp whatsappProperties;
    private final ObjectMapper objectMapper;

    public EumSocialServiceImpl(SocialMessagingAsyncClient socialMessagingAsyncClient, PnEcConfig pnEcConfig, ObjectMapper objectMapper) {
        this.socialMessagingAsyncClient = socialMessagingAsyncClient;
        this.whatsappProperties = pnEcConfig.getWhatsapp();
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<String> sendTemplateMessage(String phoneNumber, WhatsappTemplate template, String callbackData) {
        log.info(CLIENT_METHOD_INVOCATION, EUM_SOCIAL_SEND);
        return Mono.fromCallable(() -> buildMessageObject(phoneNumber, template, callbackData))
                .map(message -> SendWhatsAppMessageRequest.builder()
                        .originationPhoneNumberId(whatsappProperties.getOriginationPhoneNumberId())
                        .metaApiVersion(whatsappProperties.getMetaApiVersion())
                        .message(SdkBytes.fromUtf8String(message))
                        .build())
                .flatMap(request -> Mono.fromFuture(() -> socialMessagingAsyncClient.sendWhatsAppMessage(request)))
                .map(SendWhatsAppMessageResponse::messageId)
                .onErrorResume(throwable -> {
                    log.error(EXCEPTION_IN_PROCESS, EUM_SOCIAL_SEND, throwable, throwable.getMessage());
                    return Mono.error(new WhatsappSendException("Error sending WhatsApp message: " + throwable.getMessage(), throwable));
                })
                .doOnSuccess(messageId -> log.debug(CLIENT_METHOD_RETURN, EUM_SOCIAL_SEND, messageId));
    }

    /**
     * Costruisce il <a href="https://developers.facebook.com/docs/whatsapp/cloud-api/reference/messages">Message object</a>
     * della WhatsApp Cloud API per un messaggio di tipo template.
     */
    String buildMessageObject(String phoneNumber, WhatsappTemplate template, String callbackData) throws JsonProcessingException {
        if (template == null || !StringUtils.hasText(template.getName()) || !StringUtils.hasText(template.getLanguage())) {
            throw new WhatsappSendException("WhatsApp template name and language are mandatory");
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.put("messaging_product", "whatsapp");
        root.put("recipient_type", "individual");
        root.put("to", normalizePhoneNumber(phoneNumber));
        root.put("type", "template");
        if (StringUtils.hasText(callbackData)) {
            root.put("biz_opaque_callback_data", callbackData.length() > MAX_CALLBACK_DATA_LENGTH
                    ? callbackData.substring(0, MAX_CALLBACK_DATA_LENGTH)
                    : callbackData);
        }

        ObjectNode templateNode = root.putObject("template");
        templateNode.put("name", template.getName());
        templateNode.putObject("language").put("code", template.getLanguage());

        ArrayNode components = templateNode.putArray("components");
        addTextParametersComponent(components.addObject().put("type", "body"), template.getBodyParameters());

        List<String> buttonParameters = template.getButtonParameters();
        if (buttonParameters != null) {
            for (int index = 0; index < buttonParameters.size(); index++) {
                ObjectNode button = components.addObject()
                        .put("type", "button")
                        .put("sub_type", "url")
                        .put("index", String.valueOf(index));
                addTextParametersComponent(button, List.of(buttonParameters.get(index)));
            }
        }
        if (template.getBodyParameters() == null || template.getBodyParameters().isEmpty()) {
            // il componente body senza parametri non è ammesso dalla Cloud API
            components.remove(0);
        }
        return objectMapper.writeValueAsString(root);
    }

    private static void addTextParametersComponent(ObjectNode component, List<String> values) {
        ArrayNode parameters = component.putArray("parameters");
        if (values != null) {
            values.forEach(value -> parameters.addObject().put("type", "text").put("text", value));
        }
    }

    /**
     * La Cloud API si aspetta il numero con prefisso internazionale e senza "+" o "00" iniziali.
     */
    static String normalizePhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            throw new WhatsappSendException("WhatsApp receiver phone number is mandatory");
        }
        String normalized = phoneNumber.trim().replaceAll("[\\s\\-()]", "");
        if (normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        } else if (normalized.startsWith("00")) {
            normalized = normalized.substring(2);
        }
        if (!normalized.matches("\\d{8,15}")) {
            throw new WhatsappSendException("WhatsApp receiver phone number is not valid");
        }
        return normalized;
    }
}
