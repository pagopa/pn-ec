package it.pagopa.pn.ec.util;

import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static it.pagopa.pn.ec.commons.utils.LogUtils.EXCEPTION_IN_PROCESS;

public final class ErrorDetailsUtils {

    public static final int MAX_LENGTH = 1000;

    private static final int MAX_CAUSE_DEPTH = 3;
    private static final String CAUSED_BY = "; caused by: ";
    private static final Pattern URL_QUERY_STRING_PATTERN = Pattern.compile("(?i)(https?://[^\\s?]*)\\?[^\\s,;)\\]]*");
    private static final LogSanitizer LOG_SANITIZER = new LogSanitizer();

    private ErrorDetailsUtils() {
    }

    public static String exceptionInProcess(String process, Throwable throwable) {
        return build(EXCEPTION_IN_PROCESS, process, throwable, throwable == null ? null : throwable.getMessage());
    }

    public static String build(String template, Object... args) {
        if (template == null) {
            return null;
        }
        try {
            StringBuilder text = new StringBuilder(MessageFormatter.arrayFormat(template, args).getMessage());
            appendCauses(text, args);
            return truncate(LOG_SANITIZER.sanitize(removeUrlQueryStrings(text.toString())));
        } catch (Exception e) {
            return fallback(template);
        }
    }

    private static void appendCauses(StringBuilder text, Object[] args) {
        if (args == null) {
            return;
        }
        for (Object arg : args) {
            if (arg instanceof Throwable throwable) {
                try {
                    appendCauseChain(text, throwable);
                } catch (Exception ignored) {
                    return;
                }
            }
        }
    }

    private static void appendCauseChain(StringBuilder text, Throwable throwable) {
        Map<Throwable, Boolean> visited = new IdentityHashMap<>();
        visited.put(throwable, Boolean.TRUE);
        List<Throwable> causes = new ArrayList<>();
        Throwable current = throwable.getCause();
        while (current != null && causes.size() < MAX_CAUSE_DEPTH && visited.put(current, Boolean.TRUE) == null) {
            causes.add(current);
            current = current.getCause();
        }
        for (Throwable cause : causes) {
            text.append(CAUSED_BY).append(cause.getClass().getName());
            String message = cause.getMessage();
            if (message != null) {
                text.append(": ").append(message);
            }
        }
    }

    private static String removeUrlQueryStrings(String text) {
        return URL_QUERY_STRING_PATTERN.matcher(text).replaceAll("$1");
    }

    private static String truncate(String text) {
        if (text == null || text.length() <= MAX_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_LENGTH);
    }

    private static String fallback(String template) {
        try {
            return truncate(LOG_SANITIZER.sanitize(removeUrlQueryStrings(template)));
        } catch (Exception e) {
            return null;
        }
    }

}
