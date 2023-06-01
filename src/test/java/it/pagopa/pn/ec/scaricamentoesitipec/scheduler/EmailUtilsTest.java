package it.pagopa.pn.ec.scaricamentoesitipec.scheduler;

import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.utils.EmailUtils;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@SpringBootTestWebEnv
public class EmailUtilsTest {

    private final Integer maxMessages = 11;


    @Test
    void testFluxRepeat() throws IOException {
        Path resourceDirectory = Paths.get("src","test","resources","prova.txt");
        byte[] byteArray= Files.readAllBytes(resourceDirectory);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(byteArray.length);
        byteArrayOutputStream.writeBytes(byteArray);

        var emailAttachment=EmailAttachment.builder().nameWithExtension("prova.txt").url("www.prova.it").content(byteArrayOutputStream).build();


        MimeMessage mimeMessage= EmailUtils.getMimeMessage(EmailField.builder().to("tizio").from("caio").msgId("fdadwdawa").contentType("text/plain").text("").subject("prova").emailAttachments(List.of(emailAttachment)).build());
        var attachBytes= EmailUtils.findAttachmentByName(mimeMessage, "prova.txt");

        Path savePath = Paths.get("src","test","resources","prova_retrieved.txt");
        assert attachBytes != null;
        Files.write(savePath, attachBytes);

        Assertions.assertNotNull(attachBytes);

    }

}
