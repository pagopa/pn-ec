package it.pagopa.pn.ec.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.Exceptions;

import java.time.Duration;

import static it.pagopa.pn.ec.commons.utils.LogUtils.EXCEPTION_IN_PROCESS;
import static org.junit.jupiter.api.Assertions.*;

class ErrorDetailsUtilsTest {

    private static final String PROCESS = "lavorazioneRichiestaTest()";
    private static final String CAUSED_BY = "; caused by: ";

    private static String build(Throwable exception) {
        return ErrorDetailsUtils.build(EXCEPTION_IN_PROCESS, PROCESS, exception, exception.getMessage());
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int index = text.indexOf(token);
        while (index >= 0) {
            count++;
            index = text.indexOf(token, index + token.length());
        }
        return count;
    }

    @Test
    void build_exceptionInProcessTemplate_equalsFormattedLogText() {
        RuntimeException exception = new RuntimeException("provider failure");

        String result = build(exception);

        assertEquals("Exception in 'lavorazioneRichiestaTest()' - java.lang.RuntimeException: provider failure, provider failure", result);
    }

    @Test
    void build_plainTemplate_isFormattedWithArguments() {
        String result = ErrorDetailsUtils.build("Failure on {} with {}", "queue", 3);

        assertEquals("Failure on queue with 3", result);
    }

    @Test
    void build_exceptionWithoutCause_doesNotAppendCausedBy() {
        String result = build(new RuntimeException("provider failure"));

        assertFalse(result.contains(CAUSED_BY));
    }

    @Test
    void build_exceptionWithCause_doesNotContainStackTrace() {
        String result = build(new RuntimeException("provider failure", new IllegalStateException("root cause")));

        assertFalse(result.contains("\tat "));
        assertFalse(result.contains("Caused by"));
        assertFalse(result.contains("ErrorDetailsUtilsTest"));
    }

    @Test
    void build_exceptionWithCause_appendsCauseWithFullyQualifiedClassName() {
        String result = build(new RuntimeException("outer", new IllegalStateException("inner")));

        assertEquals("Exception in 'lavorazioneRichiestaTest()' - java.lang.RuntimeException: outer, outer"
                     + CAUSED_BY + "java.lang.IllegalStateException: inner", result);
    }

    @Test
    void build_causeWithoutMessage_appendsClassNameOnly() {
        String result = build(new RuntimeException("outer", new IllegalStateException()));

        assertTrue(result.endsWith(CAUSED_BY + "java.lang.IllegalStateException"));
    }

    @Test
    void build_causeChainWithinMaxDepth_appendsEveryCauseInOrder() {
        Throwable chain = new RuntimeException("level0", new IllegalStateException("level1", new IllegalArgumentException("level2", new UnsupportedOperationException("level3"))));

        String result = build(chain);

        assertTrue(result.endsWith(CAUSED_BY + "java.lang.IllegalStateException: level1"
                                   + CAUSED_BY + "java.lang.IllegalArgumentException: level2"
                                   + CAUSED_BY + "java.lang.UnsupportedOperationException: level3"));
    }

    @Test
    void build_causeChainLongerThanMaxDepth_stopsAtThirdCause() {
        Throwable chain = new RuntimeException("level0", new IllegalStateException("level1", new IllegalArgumentException("level2",
                new UnsupportedOperationException("level3", new SecurityException("level4", new NullPointerException("level5"))))));

        String result = build(chain);

        assertEquals(3, countOccurrences(result, CAUSED_BY));
        assertTrue(result.contains("level3"));
        assertFalse(result.contains("level4"));
        assertFalse(result.contains("level5"));
    }

    @Test
    void build_cyclicCauses_stopsWithoutLooping() {
        RuntimeException first = new RuntimeException("first");
        IllegalStateException second = new IllegalStateException("second");
        first.initCause(second);
        second.initCause(first);

        String result = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> build(first));

        assertEquals(1, countOccurrences(result, CAUSED_BY));
        assertTrue(result.endsWith(CAUSED_BY + "java.lang.IllegalStateException: second"));
    }

    @Test
    void build_causeCoincidingWithException_doesNotAppendIt() {
        RuntimeException selfReferencing = new RuntimeException("self") {
            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        };

        String result = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> build(selfReferencing));

        assertFalse(result.contains(CAUSED_BY));
    }

    @Test
    void build_retryExhaustedException_appendsCauseNotReportedByItsMessage() {
        Throwable retryExhausted = Exceptions.retryExhausted("Retries exhausted: 3/3", new IllegalStateException("attachment not available"));

        String result = build(retryExhausted);

        assertTrue(result.contains("Retries exhausted: 3/3"));
        assertTrue(result.endsWith(CAUSED_BY + "java.lang.IllegalStateException: attachment not available"));
        assertFalse(result.contains("\tat "));
    }

    @Test
    void build_retryExhaustedExceptionWithoutCause_doesNotThrow() {
        Throwable retryExhausted = Exceptions.retryExhausted("Retries exhausted: 3/3", null);

        String result = assertDoesNotThrow(() -> build(retryExhausted));

        assertTrue(result.contains("Retries exhausted: 3/3"));
        assertFalse(result.contains(CAUSED_BY));
    }

    @Test
    void build_emailInMessage_isSanitized() {
        String result = build(new RuntimeException("rejected mario.rossi@example.com"));

        assertTrue(result.contains("xxx@example.com"));
        assertFalse(result.contains("mario.rossi"));
    }

    @Test
    void build_phoneNumberInMessage_isSanitized() {
        String result = build(new RuntimeException("invalid number +393331234567"));

        assertTrue(result.contains(LogSanitizer.DEFAULT_PHONE));
        assertFalse(result.contains("3331234567"));
    }

    @Test
    void build_emailInCauseMessage_isSanitized() {
        String result = build(new RuntimeException("outer", new IllegalStateException("bounce for mario.rossi@example.com")));

        assertTrue(result.contains("bounce for xxx@example.com"));
        assertFalse(result.contains("mario.rossi"));
    }

    @Test
    void build_phoneNumberInRetryExhaustedCause_isSanitized() {
        Throwable retryExhausted = Exceptions.retryExhausted("Retries exhausted: 3/3", new IllegalStateException("invalid number +393331234567"));

        String result = build(retryExhausted);

        assertTrue(result.contains(LogSanitizer.DEFAULT_PHONE));
        assertFalse(result.contains("3331234567"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "123456789012345"})
    void build_digitSequenceOfSevenToFifteenDigits_isReplaced(String digits) {
        String result = ErrorDetailsUtils.build("request {} failed", digits);

        assertEquals("request " + LogSanitizer.DEFAULT_PHONE + " failed", result);
    }

    @Test
    void build_digitSequenceOfSixDigits_isKept() {
        String result = ErrorDetailsUtils.build("code {} failed", "123456");

        assertEquals("code 123456 failed", result);
    }

    @Test
    void build_urlWithQueryString_queryStringIsRemoved() {
        String url = "https://bucket.s3.eu-south-1.amazonaws.com/file.pdf?X-Amz-Signature=abcdef&X-Amz-Credential=ASIAXYZ%2F20260101&X-Amz-Security-Token=token";

        String result = build(new RuntimeException("download failed for " + url + " with status 403"));

        assertTrue(result.contains("https://bucket.s3.eu-south-1.amazonaws.com/file.pdf"));
        assertFalse(result.contains("?"));
        assertFalse(result.contains("X-Amz"));
        assertFalse(result.contains("abcdef"));
        assertFalse(result.contains("ASIAXYZ"));
        assertTrue(result.contains(" with status 403"));
    }

    @Test
    void build_urlWithQueryStringInCauseMessage_queryStringIsRemoved() {
        String url = "https://bucket.s3.eu-south-1.amazonaws.com/file.pdf?X-Amz-Signature=abcdef";

        String result = build(new RuntimeException("outer", new IllegalStateException("403 Forbidden from GET " + url)));

        assertTrue(result.contains(CAUSED_BY + "java.lang.IllegalStateException: 403 Forbidden from GET https://bucket.s3.eu-south-1.amazonaws.com/file.pdf"));
        assertFalse(result.contains("X-Amz-Signature"));
        assertFalse(result.contains("abcdef"));
    }

    @Test
    void build_urlWithoutQueryString_isKept() {
        String result = ErrorDetailsUtils.build("failed {}", "https://host.example/path/file.pdf");

        assertEquals("failed https://host.example/path/file.pdf", result);
    }

    @Test
    void build_urlWithQueryStringAtEndOfText_queryStringIsRemoved() {
        String result = ErrorDetailsUtils.build("failed {}", "http://host.example/file?token=secret");

        assertEquals("failed http://host.example/file", result);
    }

    @Test
    void build_urlWithQueryStringFollowedByComma_commaIsPreserved() {
        String result = ErrorDetailsUtils.build("failed {}, retry later", "http://host.example/file?token=secret");

        assertEquals("failed http://host.example/file, retry later", result);
    }

    @Test
    void build_urlWithQueryStringInParentheses_closingParenthesisIsPreserved() {
        String result = ErrorDetailsUtils.build("failed ({})", "http://host.example/file?token=secret");

        assertEquals("failed (http://host.example/file)", result);
    }

    @Test
    void build_urlWithCommaInsideQueryValue_wholeQueryIsRemoved() {
        String result = ErrorDetailsUtils.build("failed {} with status 403", "https://host.example/file?context=a,b&token=secret");

        assertEquals("failed https://host.example/file with status 403", result);
    }

    @Test
    void build_urlWithSemicolonInsideQueryValue_wholeQueryIsRemoved() {
        String result = ErrorDetailsUtils.build("failed {}", "https://host.example/file?context=a;b&token=secret");

        assertEquals("failed https://host.example/file", result);
    }

    @Test
    void build_urlWithParenthesisInsideQueryValue_wholeQueryIsRemoved() {
        String result = ErrorDetailsUtils.build("failed {} with status 403", "https://host.example/file?filter=(a)]&token=secret");

        assertEquals("failed https://host.example/file with status 403", result);
    }

    @Test
    void build_urlWithQueryStringInQuotesFollowedByPeriod_punctuationIsPreserved() {
        String result = ErrorDetailsUtils.build("GET '{}'. Retry later", "https://host.example/file?x=1,y=2&sig=secret");

        assertEquals("GET 'https://host.example/file'. Retry later", result);
    }

    @Test
    void build_uppercaseHttpsUrlWithQueryString_queryStringIsRemoved() {
        String result = ErrorDetailsUtils.build("failed {}", "HTTPS://HOST.EXAMPLE/file?token=secret");

        assertEquals("failed HTTPS://HOST.EXAMPLE/file", result);
    }

    @Test
    void build_longMessage_isTruncatedToMaxLength() {
        String longMessage = "x".repeat(5000);

        String result = build(new RuntimeException(longMessage));

        assertEquals(ErrorDetailsUtils.MAX_LENGTH, result.length());
        assertEquals(1000, ErrorDetailsUtils.MAX_LENGTH);
    }

    @Test
    void build_longMessageWithCause_isTruncatedAfterAppendingCause() {
        String longMessage = "x".repeat(5000);

        String result = build(new RuntimeException(longMessage, new IllegalStateException("inner")));

        assertEquals(1000, result.length());
        assertFalse(result.contains(CAUSED_BY));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 999, 1000})
    void build_messageNotExceedingMaxLength_isNotTruncated(int length) {
        String message = "y".repeat(length);

        String result = ErrorDetailsUtils.build("{}", message);

        assertEquals(message, result);
    }

    @Test
    void build_messageOneCharOverMaxLength_isTruncated() {
        String result = ErrorDetailsUtils.build("{}", "z".repeat(1001));

        assertEquals(1000, result.length());
    }

    @Test
    void build_phoneNumberAcrossTruncationBoundary_isSanitizedBeforeTruncation() {
        String result = ErrorDetailsUtils.build("{}", "a".repeat(995) + "+393331234567");

        assertEquals(1000, result.length());
        assertFalse(result.contains("33312"));
        assertTrue(result.contains(LogSanitizer.DEFAULT_PHONE.substring(0, 4)));
    }

    @Test
    void build_nullArguments_doesNotThrow() {
        String result = assertDoesNotThrow(() -> ErrorDetailsUtils.build(EXCEPTION_IN_PROCESS, null, null, null));

        assertNotNull(result);
        assertTrue(result.startsWith("Exception in "));
    }

    @Test
    void build_nullArgumentsArray_returnsTemplate() {
        String result = assertDoesNotThrow(() -> ErrorDetailsUtils.build("plain text", (Object[]) null));

        assertEquals("plain text", result);
    }

    @Test
    void build_nullTemplate_returnsNullWithoutThrowing() {
        String result = assertDoesNotThrow(() -> ErrorDetailsUtils.build(null, "arg"));

        assertNull(result);
    }

    @Test
    void build_argumentWithFailingToString_doesNotThrow() {
        Object failing = new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("toString failure");
            }
        };

        String result = assertDoesNotThrow(() -> ErrorDetailsUtils.build(EXCEPTION_IN_PROCESS, PROCESS, failing, "message"));

        assertNotNull(result);
    }

    @Test
    void build_exceptionWithFailingGetCause_doesNotThrow() {
        RuntimeException failingCause = new RuntimeException("outer") {
            @Override
            public synchronized Throwable getCause() {
                throw new IllegalStateException("getCause failure");
            }
        };

        String result = assertDoesNotThrow(() -> build(failingCause));

        assertNotNull(result);
        assertTrue(result.startsWith("Exception in "));
    }

    @Test
    void build_exceptionWithNullMessage_doesNotThrow() {
        String result = assertDoesNotThrow(() -> build(new RuntimeException()));

        assertTrue(result.contains("java.lang.RuntimeException"));
    }

    @Test
    void exceptionInProcess_exception_equalsBuildOfExceptionInProcessTemplate() {
        RuntimeException exception = new RuntimeException("outer", new IllegalStateException("inner"));

        String result = ErrorDetailsUtils.exceptionInProcess(PROCESS, exception);

        assertEquals(build(exception), result);
        assertEquals("Exception in 'lavorazioneRichiestaTest()' - java.lang.RuntimeException: outer, outer"
                     + CAUSED_BY + "java.lang.IllegalStateException: inner", result);
    }

    @Test
    void exceptionInProcess_nullThrowable_doesNotThrow() {
        String result = assertDoesNotThrow(() -> ErrorDetailsUtils.exceptionInProcess(PROCESS, null));

        assertEquals("Exception in 'lavorazioneRichiestaTest()' - null, null", result);
    }

    @Test
    void exceptionInProcess_exceptionWithNullMessage_doesNotThrow() {
        String result = assertDoesNotThrow(() -> ErrorDetailsUtils.exceptionInProcess(PROCESS, new RuntimeException()));

        assertTrue(result.contains("java.lang.RuntimeException"));
    }

    @Test
    void exceptionInProcess_phoneNumberInMessage_isSanitizedOnlyOnce() {
        String result = ErrorDetailsUtils.exceptionInProcess(PROCESS, new RuntimeException("invalid number +393331234567"));

        assertEquals(2, countOccurrences(result, LogSanitizer.DEFAULT_PHONE));
        assertFalse(result.contains("+39 000 +39"));
        assertFalse(result.contains("3331234567"));
    }

    @Test
    void exceptionInProcess_emailInMessage_isSanitized() {
        String result = ErrorDetailsUtils.exceptionInProcess(PROCESS, new RuntimeException("rejected mario.rossi@example.com"));

        assertTrue(result.contains("xxx@example.com"));
        assertFalse(result.contains("mario.rossi"));
    }

}
