package it.pagopa.pn.ec.pec.model.pojo;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PagopaMimeMessage extends MimeMessage {

    final String messageId;

    public PagopaMimeMessage(Session session, String messageId) {
        super(session);
        this.messageId = messageId;
    }

    @Override
    protected void updateMessageID() throws MessagingException {
        setHeader("Message-ID", this.messageId);
    }
}
