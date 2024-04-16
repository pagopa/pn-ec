package it.pagopa.pn.ec.scaricamentoesitipec.utils;

import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.utils.EmailUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.encodeMessageId;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;

public class PecUtils {

    public static StringBuffer generateDaticertAccettazione(String tipo, String from, String receiver, String replyTo, String subject, String gestoreMittente, String data, String orario, String messageId, String tipoDestinatario) {

        //Costruzione del daticert
        StringBuffer stringBufferContent = new StringBuffer();
        stringBufferContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");//popolare con daticert su note
        stringBufferContent.append("<postacert tipo=").append("\"").append(tipo).append("\"").append(" ").append("errore=\"nessuno\">");
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
        stringBufferContent.append("<errore-esteso>").append("5.4.1 - Aruba Pec S.p.A. - il messaggio non è stato consegnato nelle prime ventiquattro ore dal suo invio").append("</errore-esteso>");
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

}
