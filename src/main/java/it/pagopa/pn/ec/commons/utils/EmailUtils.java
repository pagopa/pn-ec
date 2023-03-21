package it.pagopa.pn.ec.commons.utils;

import it.pagopa.pn.ec.commons.exception.email.ComposeMimeMessageException;
import it.pagopa.pn.ec.commons.exception.email.RetrieveMessageIdException;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.pec.model.pojo.PagopaMimeMessage;
import lombok.extern.slf4j.Slf4j;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@Slf4j
public class EmailUtils {

    private EmailUtils() {
        throw new IllegalStateException("EmailUtils is a utility class");
    }

    public static String getDomainFromAddress(String address) {
        return address.substring(address.indexOf("@"));
    }

    public static MimeMessage getMimeMessage(byte[] bytes) {
        try {
            return new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(bytes));
        } catch (MessagingException e) {
            throw new ComposeMimeMessageException();
        }
    }

    public static String getMessageIdFromMimeMessage(MimeMessage mimeMessage) throws RetrieveMessageIdException {
        try {
            return mimeMessage.getMessageID();
        } catch (MessagingException e) {
            throw new RetrieveMessageIdException();
        }
    }

    public static MimeMessage getMimeMessage(EmailField emailField) {
        try {
            var session = Session.getInstance(new Properties());
            MimeMessage mimeMessage;
            if (emailField.getMsgId() == null) {
                mimeMessage = new MimeMessage(session);
            } else {
                mimeMessage = new PagopaMimeMessage(session, emailField.getMsgId());
            }

            mimeMessage.setFrom(emailField.getFrom());
            mimeMessage.setRecipients(Message.RecipientType.TO, emailField.getTo());
            mimeMessage.setSubject(emailField.getSubject());

            var htmlOrPlainTextPart = new MimeBodyPart();
            htmlOrPlainTextPart.setContent(emailField.getText(), emailField.getContentType());

            var mimeMultipart = new MimeMultipart();

            mimeMultipart.addBodyPart(htmlOrPlainTextPart);

            var emailAttachments = emailField.getEmailAttachments();
            if (emailAttachments != null) {
                emailAttachments.forEach(attachment -> {
                    var attachmentPart = new MimeBodyPart();
                    var byteArrayOutputStream = (ByteArrayOutputStream) attachment.getContent();
                    DataSource aAttachment = new ByteArrayDataSource(byteArrayOutputStream.toByteArray(), APPLICATION_OCTET_STREAM_VALUE);
                    try {
                        attachmentPart.setDataHandler(new DataHandler(aAttachment));
                        attachmentPart.setFileName(attachment.getNameWithExtension());
                        mimeMultipart.addBodyPart(attachmentPart);
                    } catch (MessagingException exception) {
                        log.error(exception.getMessage());
                        throw new ComposeMimeMessageException();
                    }
                });
            }

            mimeMessage.setContent(mimeMultipart);

            return mimeMessage;
        } catch (MessagingException exception) {
            log.error(exception.getMessage());
            throw new ComposeMimeMessageException();
        }
    }

    public static OutputStream getMimeMessageOutputStream(EmailField emailField) {
        var output = new ByteArrayOutputStream();
        try {
            getMimeMessage(emailField).writeTo(output);
            return output;
        } catch (IOException | MessagingException exception) {
            log.error(exception.getMessage());
            throw new ComposeMimeMessageException();
        }
    }

    public static String getMimeMessageInCDATATag(EmailField emailField) {
        return String.format("<![CDATA[%s]]>", getMimeMessageOutputStream(emailField));
    }
}
