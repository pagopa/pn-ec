package it.pagopa.pn.ec.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogSanitizerTest {

    private LogSanitizer logSanitizer;

    @BeforeEach
    void setUp() {
        logSanitizer = new LogSanitizer();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "Invio email a test.user@example.com; Invio email a xxx@example.com",
            "Contatti: user1@example.com, user.two@domain.it; Contatti: xxx@example.com, xxx@domain.it",
            "Contatto email: first.last+category@gmail.com; Contatto email: xxx@gmail.com"
    }, delimiter = ';')
    void testSanitizeEmails(String input, String expected) {
        String result = logSanitizer.sanitize(input);
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testSanitizePhone() {
        String input = "Chiamata a +391231234567";
        String result = logSanitizer.sanitize(input);
        Assertions.assertEquals("Chiamata a " + LogSanitizer.DEFAULT_PHONE, result);
    }

    @Test
    void testSanitizeMultiplePhones() {
        String input = "Numeri utili: +393331234567 e +441234567890";
        String result = logSanitizer.sanitize(input);
        Assertions.assertEquals("Numeri utili: " + LogSanitizer.DEFAULT_PHONE + " e " + LogSanitizer.DEFAULT_PHONE, result);
    }

    @Test
    void testSanitizeNull() {
        String result = logSanitizer.sanitize(null);
        Assertions.assertNull(result);
    }

    @Test
    void testSanitizeEmpty() {
        String result = logSanitizer.sanitize("");
        Assertions.assertEquals("", result);
    }

    @Test
    void testSanitizeWithoutSensitiveData() {
        String input = "Messaggio generico senza informazioni sensibili.";
        String result = logSanitizer.sanitize(input);
        Assertions.assertEquals(input, result);
    }

    @Test
    void testSanitizeWithSpecialCharacters() {
        String input = "Email:[test@example.com]; Tel:(+391231234567)";
        String result = logSanitizer.sanitize(input);
        Assertions.assertEquals("Email:[xxx@example.com]; Tel:(" + LogSanitizer.DEFAULT_PHONE + ")", result);
    }

}

