package it.pagopa.pn.ec.pec.utils;

import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import org.springframework.util.Base64Utils;

import java.nio.charset.StandardCharsets;

public class MessageIdUtils {

    private MessageIdUtils() {
        throw new IllegalStateException("MessageIdUtils is a utility class");
    }

    /**
     * @param messageId base64RequestId|base64ClientId@pagopa.it
     * @return The decoded requestId
     */
    public static PresaInCaricoInfo decodeMessageId(String messageId) {
        var splitAtPipe = messageId.split("\\|");
        var base64RequestId = splitAtPipe[0];
        var base64ClientId = splitAtPipe[1].split("@")[0];
        return new PresaInCaricoInfo(new String(Base64Utils.decodeFromString(base64RequestId), StandardCharsets.UTF_8),
                                     new String(Base64Utils.decodeFromString(base64ClientId), StandardCharsets.UTF_8));
    }
}
