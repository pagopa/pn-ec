package it.pagopa.pn.ec.commons.utils;
import it.pagopa.pn.ec.commons.exception.email.*;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.pec.model.pojo.PagopaMimeMessage;
import lombok.CustomLog;
import lombok.SneakyThrows;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.*;
import java.util.*;

import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@CustomLog
public class EmailUtils {

    private EmailUtils() {
        throw new IllegalStateException("EmailUtils is a utility class");
    }
    public static final Integer MB_TO_BYTES = 1000000;
    private static final String C_DATA_TAG = "<![CDATA[%s]]>";
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

    public static byte[] getMimeMessageByteArray(MimeMessage mimeMessage) {
        var output = new ByteArrayOutputStream();
        try {
            mimeMessage.writeTo(output);
            return output.toByteArray();
        } catch (IOException | MessagingException exception) {
            throw new ComposeMimeMessageException();
        }
    }

    @SneakyThrows(MessagingException.class)
    public static void setHeaderInMimeMessage(MimeMessage mimeMessage, Header header) {
        mimeMessage.setHeader(header.getName(), header.getValue());
    }

    @SneakyThrows({MessagingException.class, UnsupportedEncodingException.class})
    public static MimeMessage buildMimeMessage(EmailField emailField) {
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
        mimeMessage.setContent(mimeMultipart);

        return mimeMessage;
    }

    @SneakyThrows(MessagingException.class)
    public static MimeBodyPart buildAttachmentPart(EmailAttachment emailAttachment) {
        var attachmentPart = new MimeBodyPart();
        var byteArrayOutputStream = (ByteArrayOutputStream) emailAttachment.getContent();
        DataSource aAttachment = new ByteArrayDataSource(byteArrayOutputStream.toByteArray(), APPLICATION_OCTET_STREAM_VALUE);
        attachmentPart.setDataHandler(new DataHandler(aAttachment));
        attachmentPart.setFileName(emailAttachment.getNameWithExtension());
        return attachmentPart;
    }

    @SneakyThrows({IOException.class, MessagingException.class})
    public static Multipart getMultipartFromMimeMessage(MimeMessage mimeMessage) {
        return (MimeMultipart) mimeMessage.getContent();
    }

    @SneakyThrows(MessagingException.class)
    public static Integer getMultipartCount(Multipart multipart) {
        return multipart.getCount();
    }

    @SneakyThrows({IOException.class, MessagingException.class})
    public static void removeLastAttachmentFromMimeMessage(MimeMessage mimeMessage) {
        log.debug("Removing last attachment from mimeMessage...");
        var multipart = (MimeMultipart) mimeMessage.getContent();
        multipart.removeBodyPart(multipart.getCount() - 1);
        mimeMessage.saveChanges();
    }

    @SneakyThrows({IOException.class, MessagingException.class})
    public static void removeAllExceptFirstAttachmentFromMimeMessage(MimeMessage mimeMessage) {
        var multipart = (MimeMultipart) mimeMessage.getContent();
        for (int i = multipart.getCount() - 1; i > 1; i--) {
            log.debug("Removing attachment number '{}' from mimeMessage...", i);
            multipart.removeBodyPart(i);
        }
        mimeMessage.saveChanges();
    }

    @SneakyThrows({IOException.class, MessagingException.class})
    public static MimeMessage addAttachmentToMimeMessage(MimeMessage mimeMessage, MimeBodyPart mimeBodyPart) {
        log.debug("Adding attachment '{}' to mimeMessage...", mimeBodyPart.getFileName());
        ((MimeMultipart) mimeMessage.getContent()).addBodyPart(mimeBodyPart);
        mimeMessage.saveChanges();
        return mimeMessage;
    }

    @SneakyThrows({IOException.class, MessagingException.class})
    public static ByteArrayOutputStream getMimeMessageOutputStream(MimeMessage mimeMessage) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        mimeMessage.writeTo(byteArrayOutputStream);
        return byteArrayOutputStream;
    }

    @SneakyThrows({IOException.class, MessagingException.class})
    public static int getMimeMessageSizeInBytes(MimeMessage mimeMessage, CountingOutputStream countingOutputStream) {
        mimeMessage.writeTo(countingOutputStream);
        var mimeMessageSize = countingOutputStream.getCount();
        countingOutputStream.resetCount();
        return mimeMessageSize;
    }

    @SneakyThrows(MessagingException.class)
    public static String[] getHeaderFromMimeMessage(MimeMessage mimeMessage, String headerName) {
        return mimeMessage.getHeader(headerName);
    }

    @SneakyThrows(MessagingException.class)
    public static String getSenderFromMimeMessage(MimeMessage mimeMessage) {
        return mimeMessage.getFrom()[0].toString();
    }

    public static OutputStream getMimeMessageOutputStream(EmailField emailField) {
        var output = new ByteArrayOutputStream();
        try {
            getMimeMessage(emailField).writeTo(output);
            return output;
        } catch (IOException | MessagingException exception) {
            throw new ComposeMimeMessageException();
        }
    }

    public static String getMimeMessageFromCDATATag(String cDataString) {
        return cDataString.substring(cDataString.indexOf("[CDATA[") + "[CDATA[".length(), cDataString.lastIndexOf("]]"));
    }

    public static String getMimeMessageInCDATATag(byte[] fileBytes) {
        return String.format(C_DATA_TAG, new String(fileBytes));
    }

    public static String getMimeMessageInCDATATag(EmailField emailField) {
        return String.format(C_DATA_TAG, getMimeMessageOutputStream(emailField));
    }

    public static String getMimeMessageInCDATATag(MimeMessage mimeMessage) {
        return String.format(C_DATA_TAG, getMimeMessageOutputStream(mimeMessage));
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
            log.error("Attachment with name '{}' not found", attachmentName);
            return new byte[0];
        } catch (IOException | MessagingException e) {
            throw new RetrieveAttachmentException();
        }
    }

    public static InputStream getAttachmentFromBodyPart(BodyPart part, String fileName) {
        try {

            Object content = part.getContent();
            if (content instanceof InputStream || content instanceof String) {
                if (StringUtils.isNotBlank(part.getFileName()) && part.getFileName().equals(fileName)) {
                    return part.getInputStream();
                } else {
                    return null;
                }
            }

            if (content instanceof Multipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    InputStream result = getAttachmentFromBodyPart(bodyPart, fileName);
                    if (result != null)
                        return result;
                }
            }

            return null;

        } catch (IOException | MessagingException exception) {
            throw new RetrieveAttachmentException();
        }
    }

//    public static byte[] findAttachmentByName(MimeMessage mimeMessage, String attachmentName) {
//        try {
//            log.debug("Start retrieving attachment with name '{}'", attachmentName);
//            MimeMessageParser mimeMessageParser = new MimeMessageParser(mimeMessage);
//            DataSource attachment = mimeMessageParser.parse().findAttachmentByName(attachmentName);
//            return attachment == null ? null : attachment.getInputStream().readAllBytes();
//        } catch (Exception e) {
//            throw new RetrieveAttachmentException();
//        }
//    }
}
