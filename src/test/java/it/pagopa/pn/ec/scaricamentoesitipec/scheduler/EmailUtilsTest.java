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

        Path resourceDirectory2 = Paths.get("src","test","resources","prova-test.txt");
        byte[] byteArray2= Files.readAllBytes(resourceDirectory2);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(byteArray.length);
        byteArrayOutputStream.writeBytes(byteArray);

        ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream(byteArray2.length);
        byteArrayOutputStream2.writeBytes(byteArray2);

        var emailAttachment1=EmailAttachment.builder().nameWithExtension("prova1.txt").url("www.prova.it").content(byteArrayOutputStream).build();
        var emailAttachment2=EmailAttachment.builder().nameWithExtension("prova2.txt").url("www.prova.it").content(byteArrayOutputStream).build();
        var emailAttachment3=EmailAttachment.builder().nameWithExtension("prova3.txt").url("www.prova.it").content(byteArrayOutputStream2).build();


        MimeMessage mimeMessage= EmailUtils.getMimeMessage(EmailField.builder().to("tizio").from("caio").msgId("fdadwdawa").text("TESTO DI PROVA.").contentType("multipart/mixed").subject("prova").emailAttachments(List.of(emailAttachment1, emailAttachment2, emailAttachment3)).build());

        var attachBytes= EmailUtils.getAttachmentFromMimeMessage(mimeMessage, "prova3.txt");

        Path savePath = Paths.get("src","test","resources","prova_retrieved.txt");
        assert attachBytes != null;
        Files.write(savePath, attachBytes);

        Assertions.assertNotNull(attachBytes);

    }

}
