package it.pagopa.pn.ec.pec.utils;

import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.model.pojo.request.StepError;
import it.pagopa.pn.ec.pec.exception.MessageIdException;
import org.springframework.util.Base64Utils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class MessageIdUtils {

    private static final String SEPARATORE = "~";
    public static final String DOMAIN = "@pagopa.it";

    private MessageIdUtils() {
        throw new IllegalStateException("MessageIdUtils is a utility class");
    }

    public static String encodeMessageId(String concatRequestId) {
        String[] parts = concatRequestId.split(SEPARATORE);
        String clientId = parts[0];
        String requestId = parts[1];
        try {
            return String.format("%s%s%s%s",
                                 Base64Utils.encodeToString(clientId.getBytes()),
                                 SEPARATORE,
                                 Base64Utils.encodeToString(requestId.getBytes()),
                                 DOMAIN);
        } catch (Exception e) {
            throw new MessageIdException.EncodeMessageIdException();
        }
    }

    public static String encodeMessageId(String idClient, String idRequest) {
        try {
            return String.format("%s%s%s%s",
                                 Base64Utils.encodeToString(idClient.getBytes()),
                                 SEPARATORE,
                                 Base64Utils.encodeToString(idRequest.getBytes()),
                                 DOMAIN);
        } catch (Exception e) {
            throw new MessageIdException.EncodeMessageIdException();
        }
    }

    /**
     * @param messageId base64RequestId~base64ClientId@pagopa.it
     * @return The decoded requestId
     */
    public static PresaInCaricoInfo decodeMessageId(String messageId) {
        try {
            var splitAtPipe = removeBracketsFromMessageId(messageId).split(SEPARATORE);
            var base64ClientId = splitAtPipe[0];
            var base64RequestId = splitAtPipe[1].split(String.valueOf(DOMAIN.charAt(0)))[0];
            var decodedClientId = new String(Base64Utils.decodeFromString(base64ClientId));
            var decodedRequestId = new String(Base64Utils.decodeFromString(base64RequestId));
            return new PresaInCaricoInfo(decodedRequestId, decodedClientId, new StepError());
        } catch (Exception e) {
            throw new MessageIdException.DecodeMessageIdException();
        }
    }

    public static String removeBracketsFromMessageId(String messageId) {
        //Rimuove le parentesi angolari dal messageID.
        messageId = URLDecoder.decode(messageId, StandardCharsets.UTF_8);
        if (messageId.startsWith("<") && messageId.endsWith(">"))
            messageId = messageId.substring(1, messageId.length() - 1);
        return messageId;
    }
}
