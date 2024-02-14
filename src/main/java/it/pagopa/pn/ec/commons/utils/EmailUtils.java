package it.pagopa.pn.ec.commons.utils;
import it.pagopa.pn.ec.commons.exception.email.*;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.pec.model.pojo.PagopaMimeMessage;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.*;
import java.util.*;

import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@CustomLog
public class EmailUtils {

    public static final String ARUBA_PATTERN_STRING = "@pec.aruba.it";

    private EmailUtils() {
        throw new IllegalStateException("EmailUtils is a utility class");
    }

    public static String getDomainFromAddress(String address) {
        return address.substring(address.indexOf("@"));
    }

    public static MimeMessage getMimeMessage(byte[] bytes) {
        try {
            log.debug("Start getting MimeMessage from byte array with length '{}'", bytes.length);
            return new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(bytes));
        } catch (MessagingException e) {
            throw new ComposeMimeMessageException();
        }
    }

    public static Object getContentFromMimeMessage(MimeMessage mimeMessage) {
        try {
            return mimeMessage.getContent();
        } catch (IOException | MessagingException e) {
            throw new RetrieveContentException();
        }
    }

    public static String getMessageIdFromMimeMessage(MimeMessage mimeMessage) throws RetrieveMessageIdException {
        try {
            return mimeMessage.getMessageID();
        } catch (MessagingException e) {
            throw new RetrieveMessageIdException();
        }
    }

    public static String[] getFromFromMimeMessage(MimeMessage mimeMessage) throws RetrieveMessageIdException {
        try {
            return Arrays.stream(mimeMessage.getFrom()).map(Address::toString).toArray(String[]::new);
        } catch (MessagingException e) {
            throw new RetrieveFromException();
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

            mimeMessage.setFrom(new InternetAddress(emailField.getFrom(), "", UTF_8));
            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(emailField.getTo(), "", UTF_8));
            mimeMessage.setSubject(emailField.getSubject(), UTF_8);

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
        } catch (MessagingException | UnsupportedEncodingException exception) {
            throw new ComposeMimeMessageException();
        }
    }

    public static byte[] getMimeMessageByteArray(EmailField emailField) {
        var output = new ByteArrayOutputStream();
        try {
            getMimeMessage(emailField).writeTo(output);
            return output.toByteArray();
        } catch (IOException | MessagingException exception) {
            throw new ComposeMimeMessageException();
        }
    }

    public static String getMimeMessageInCDATATag(byte[] fileBytes) {
        return String.format("<![CDATA[%s]]>", new String(fileBytes));
    }

    public static byte[] getAttachmentFromMimeMessage(MimeMessage mimeMessage, String attachmentName) {
        try {
            log.debug("Start retrieving attachment with name '{}'", attachmentName);
            Object content = mimeMessage.getContent();
            if (content instanceof String) {
                return new byte[0];
            }

            if (content instanceof Multipart multipart) {

                for (int i = 0; i < multipart.getCount(); i++) {
                    InputStream result = getAttachmentFromBodyPart(multipart.getBodyPart(i), attachmentName);
                    if (!Objects.isNull(result)) {
                        return result.readAllBytes();
                    }
                }
            }
            throw new RetrieveAttachmentException();
        } catch (IOException | MessagingException e) {
            throw new RetrieveAttachmentException();
        }
    }

    public static InputStream getAttachmentFromBodyPart(BodyPart part, String fileName) {
        try {

            Object content = part.getContent();
            if (content instanceof InputStream || content instanceof String) {
                if ((Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || StringUtils.isNotBlank(part.getFileName())) && part.getFileName().equals(fileName)) {
                    return part.getInputStream();
                } else {
                    return null;
                }
            }

            if (content instanceof Multipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    getAttachmentFromBodyPart(bodyPart, fileName);
                }
            }

            return null;

        } catch (IOException | MessagingException exception) {
            throw new RetrieveAttachmentException();
        }
    }

    public static byte[] findAttachmentByName(MimeMessage mimeMessage, String attachmentName) {
        try {
            log.debug("Start retrieving attachment with name '{}'", attachmentName);
            MimeMessageParser mimeMessageParser = new MimeMessageParser(mimeMessage);
            DataSource attachment = mimeMessageParser.parse().findAttachmentByName(attachmentName);
            return attachment == null ? null : attachment.getInputStream().readAllBytes();
        } catch (Exception e) {
            throw new RetrieveAttachmentException();
        }
    }
    //TODO move to arubaserviceimpl
    public static boolean isAruba(String messageID) {
        return messageID.trim().toLowerCase().endsWith(ARUBA_PATTERN_STRING);
    }
}
