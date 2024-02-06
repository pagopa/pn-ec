package it.pagopa.pn.ec.commons.utils;

import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EmailUtilsTest {

    private EmailField emailField;

    @BeforeEach
    void setUp() {
        Header tipoBreveHeader= new Header("X-TipoRicevuta", "breve");

        emailField = new EmailField() {
            {
                setMsgId("msgId");
                setFrom("from");
                setTo("to");
                setSubject("subject");
                setText("text");
                setContentType("contentType");
                setHeadersList(List.of(tipoBreveHeader));
            }
        };
    }

    @Test
    void getMimeMessageWithTipoBreveOk() throws MessagingException {
        MimeMessage result = EmailUtils.getMimeMessage(emailField);
        assertEquals(true, result.getHeader("X-TipoRicevuta")[0].equals("breve"));
    }
}
