package it.pagopa.pn.ec.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class LogSanitizer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?\\d{7,15}");
    public static final String DEFAULT_PHONE = "+39 000 0000000";

    public String sanitize(String message) {
        if (message == null) {
            return null;
        }

        message = EMAIL_PATTERN.matcher(message)
                               .replaceAll(match -> "xxx@" + match.group().split("@")[1]);

        message = PHONE_PATTERN.matcher(message)
                               .replaceAll(DEFAULT_PHONE);

        return message;
    }
}

