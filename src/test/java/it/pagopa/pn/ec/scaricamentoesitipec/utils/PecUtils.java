package it.pagopa.pn.ec.scaricamentoesitipec.utils;

import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.utils.EmailUtils;

import javax.mail.MessagingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.encodeMessageId;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;

public class PecUtils {

    public static StringBuffer generateDaticertAccettazione(String from, String receiver, String replyTo, String subject, String gestoreMittente, String data, String orario, String messageId, String tipoDestinatario) {

        //Costruzione del daticert
        StringBuffer stringBufferContent = new StringBuffer();
        stringBufferContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");//popolare con daticert su note
        stringBufferContent.append("<postacert tipo=\"accettazione\" errore=\"nessuno\">");
        stringBufferContent.append("<intestazione>");
        stringBufferContent.append("<mittente>").append(from).append("</mittente>"); //mittente dell'email, sta nella mappa
        stringBufferContent.append("<destinatari tipo=").append("\"").append(tipoDestinatario).append("\"").append(">").append(receiver).append("</destinatari>"); //destinatario dell'email, sta nella mappa
        stringBufferContent.append("<risposte>").append(replyTo).append("</risposte>"); //nel messaggio che uso per popolare la mappa c'è un reply-to
        stringBufferContent.append("<oggetto>").append(subject).append("</oggetto>"); //oggetto dell'email, sta nella mappa
        stringBufferContent.append("</intestazione>");
        stringBufferContent.append("<dati>");
        stringBufferContent.append("<gestore-emittente>").append(gestoreMittente).append("</gestore-emittente>"); //da inventare = "mock-pec" costante
        stringBufferContent.append("<data zona=\"+0200\">"); //lasciare così
        stringBufferContent.append("<giorno>").append(data).append("</giorno>"); //impostare in base all'ora
        stringBufferContent.append("<ora>").append(orario).append("</ora>"); //impostare in base all'ora
        stringBufferContent.append("</data>");
        stringBufferContent.append("<identificativo>").append(generateRandomString(64)).append("</identificativo>"); //stringa random 64 caratteri
        stringBufferContent.append("<msgid>").append(messageId).append("</msgid>"); //msgid della mappa, nella forma url encoded. fare url encode della stringa
        stringBufferContent.append("</dati>");
        stringBufferContent.append("</postacert>");

        return stringBufferContent;
    }

    public static String generateRandomString(int length) {
        Random random = new Random();

        // Use the nextBytes() method to generate a random sequence of bytes.
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);

        // Convert the bytes to a string using the Base64 encoding.
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static ByteArrayOutputStream generatePecAccettazione(String clientId, String requestIdx, String from, String tipoDestinatario, boolean hasDaticert) throws MessagingException, IOException {
        String msgId = "-" + encodeMessageId(clientId, requestIdx) + "-";
        var daticertBytes = generateDaticertAccettazione("from", "receiverAddress@pagopa.it", "replyTo", "subject", "gestoreMittente", "03/11/1999", "00:00:00", msgId, tipoDestinatario).toString().getBytes();
        ByteArrayOutputStream daticertOutput = new ByteArrayOutputStream();
        daticertOutput.write(daticertBytes);

        EmailField emailField = EmailField.builder()
                .msgId("messageId")
                .from(from)
                .to("to")
                .subject("subject")
                .text("text")
                .contentType(PLAIN.getValue())
                .emailAttachments(List.of(EmailAttachment.builder()
                        .nameWithExtension(hasDaticert ? "daticert.xml" : "other.xml")
                        .url("url")
                        .content(daticertOutput).build()))
                .build();


        var emailOutput = new ByteArrayOutputStream();
        EmailUtils.getMimeMessage(emailField).writeTo(emailOutput);

        return emailOutput;
    }

}
