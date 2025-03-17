package it.pagopa.pn.ec.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;

@ExtendWith(MockitoExtension.class)
class LogSanitizerTest {

    private LogSanitizer logSanitizer;

    @BeforeEach
    void setUp() {
        logSanitizer = new LogSanitizer();
    }

    @Test
    void testSanitizeEmail() {
        String input = "Invio email a test.user@example.com";
        String result = logSanitizer.sanitize(input);
        assertEquals("Invio email a xxx@example.com", result);
    }

    @Test
    void testSanitizeMultipleEmails() {
        String input = "Contatti: user1@example.com, user.two@domain.it";
        String result = logSanitizer.sanitize(input);
        assertEquals("Contatti: xxx@example.com, xxx@domain.it", result);
    }

    @Test
    void testSanitizePhone() {
        String input = "Chiamata a +391231234567";
        String result = logSanitizer.sanitize(input);
        assertEquals("Chiamata a " + LogSanitizer.DEFAULT_PHONE, result);
    }

    @Test
    void testSanitizeMultiplePhones() {
        String input = "Numeri utili: +393331234567 e +441234567890";
        String result = logSanitizer.sanitize(input);
        assertEquals("Numeri utili: " + LogSanitizer.DEFAULT_PHONE + " e " + LogSanitizer.DEFAULT_PHONE, result);
    }

    @Test
    void testSanitizeNull() {
        String result = logSanitizer.sanitize(null);
        assertEquals(null, result);
    }

    @Test
    void testSanitizeEmpty() {
        String result = logSanitizer.sanitize("");
        assertEquals("", result);
    }

    @Test
    void testSanitizeWithoutSensitiveData() {
        String input = "Messaggio generico senza informazioni sensibili.";
        String result = logSanitizer.sanitize(input);
        assertEquals(input, result);
    }

    @Test
    void testSanitizeWithSpecialCharacters() {
        String input = "Email:[test@example.com]; Tel:(+391231234567)";
        String result = logSanitizer.sanitize(input);
        assertEquals("Email:[xxx@example.com]; Tel:(" + LogSanitizer.DEFAULT_PHONE + ")", result);
    }

    @Test
    void testSanitizeComplexEmails() {
        String input = "Contatto email: first.last+category@gmail.com";
        String result = logSanitizer.sanitize(input);
        assertEquals("Contatto email: xxx@gmail.com", result);
    }
}

