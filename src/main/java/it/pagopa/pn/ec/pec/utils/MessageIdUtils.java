package it.pagopa.pn.ec.pec.utils;

import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.pec.exception.MessageIdException;
import org.springframework.util.Base64Utils;

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
            var splitAtPipe = messageId.split(SEPARATORE);
            var base64ClientId = splitAtPipe[0];
            var base64RequestId = splitAtPipe[1].split(String.valueOf(DOMAIN.charAt(0)))[0];
            return new PresaInCaricoInfo(String.format("%s%s%s",
                    new String(Base64Utils.decodeFromString(base64ClientId)),
                    SEPARATORE,
                    new String(Base64Utils.decodeFromString(base64RequestId))),
                    new String(Base64Utils.decodeFromString(base64ClientId)));
        } catch (Exception e) {
            throw new MessageIdException.DecodeMessageIdException();
        }
    }
}
