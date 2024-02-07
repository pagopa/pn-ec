package it.pagopa.pn.ec.commons.utils;
import it.pagopa.pn.ec.commons.exception.email.*;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.pec.model.pojo.PagopaMimeMessage;
import lombok.CustomLog;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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

    private EmailUtils() {
        throw new IllegalStateException("EmailUtils is a utility class");
    }
    public static final Integer MB_TO_KB = 1000000;
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

    public static Mono<MimeMessage> getMonoMimeMessage(EmailField emailField, String mimeMessageRule, Integer maxMessageSizeKb, boolean canInsertXTipoRicevutaHeader) {
        return Mono.fromSupplier(() -> buildMimeMessage(emailField))
                .flatMap(mimeMessage ->  setAttachmentsInMimeMessage(mimeMessage, emailField, maxMessageSizeKb, mimeMessageRule))
                .flatMap(mimeMessage -> setHeadersInMimeMessage(mimeMessage, emailField.getHeadersList(), canInsertXTipoRicevutaHeader));
    }

    private static Mono<MimeMessage> setAttachmentsInMimeMessage(MimeMessage mimeMessage, EmailField emailField, Integer maxMessageSizeKb, String mimeMessageRule) {
        return Flux.fromIterable(emailField.getEmailAttachments())
                .map(EmailUtils::buildAttachmentPart)
                .map(mimeBodyPart -> addAttachmentToMimeMessage(mimeMessage, mimeBodyPart))
                .takeWhile(mime -> {
                    ByteArrayOutputStream outputStream = getMimeMessageOutputStream(mimeMessage);
                    return outputStream.toByteArray().length <= maxMessageSizeKb;
                })
                .then()
                .thenReturn(mimeMessage)
                .filter(mime -> getMimeMessageOutputStream(mime).toByteArray().length > maxMessageSizeKb)
                .handle((mime, sink) -> {
                    Multipart multipart = getMultipartFromMimeMessage(mime);
                    if (getMultipartCount(multipart) <= 2)
                        sink.error(new RuntimeException());
                    else sink.next(mime);
                })
                .cast(MimeMessage.class)
                .flatMap(mime -> {
                    if (mimeMessageRule.equals("LIMIT"))
                        removeLastAttachmentFromMimeMessage(mime);
                    else if (mimeMessageRule.equals("FIRST"))
                        removeAllExceptFirstAttachmentFromMimeMessage(mime);
                    return Mono.just(mime);
                })
                .defaultIfEmpty(mimeMessage);
    }
    private static Mono<MimeMessage> setHeadersInMimeMessage(MimeMessage mimeMessage, List<Header> headers, boolean canInsertXTipoRicevutaHeader) {
        return Flux.fromIterable(headers)
                .filter(header -> !header.getName().equals("X-TipoRicevuta"))
                .doOnNext(header -> setHeaderInMimeMessage(mimeMessage, header))
                .doOnDiscard(Header.class, header -> {
                    if (canInsertXTipoRicevutaHeader) {
                        setHeaderInMimeMessage(mimeMessage, header);
                    }
                })
                .then()
                .thenReturn(mimeMessage);
    }
    private static void setHeaderInMimeMessage(MimeMessage mimeMessage, Header header) {
        try {
            mimeMessage.setHeader(header.getName(), header.getValue());
        } catch (MessagingException e) {
            throw new RuntimeException(e);
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

    @SneakyThrows
    private static MimeMessage buildMimeMessage(EmailField emailField) {
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

    @SneakyThrows
    private static MimeBodyPart buildAttachmentPart(EmailAttachment emailAttachment) {
        var attachmentPart = new MimeBodyPart();
        var byteArrayOutputStream = (ByteArrayOutputStream) emailAttachment.getContent();
        DataSource aAttachment = new ByteArrayDataSource(byteArrayOutputStream.toByteArray(), APPLICATION_OCTET_STREAM_VALUE);
        attachmentPart.setDataHandler(new DataHandler(aAttachment));
        attachmentPart.setFileName(emailAttachment.getNameWithExtension());
        return attachmentPart;
    }

    @SneakyThrows
    public static Multipart getMultipartFromMimeMessage(MimeMessage mimeMessage) {
        return (MimeMultipart) mimeMessage.getContent();
    }

    @SneakyThrows
    public static Integer getMultipartCount(Multipart multipart) {
        return multipart.getCount();
    }
    @SneakyThrows
    private static void removeLastAttachmentFromMimeMessage(MimeMessage mimeMessage) {
        var multipart = (MimeMultipart) mimeMessage.getContent();
        multipart.removeBodyPart(multipart.getCount() - 1);
        mimeMessage.setContent(multipart);
    }

    @SneakyThrows
    private static void removeAllExceptFirstAttachmentFromMimeMessage(MimeMessage mimeMessage) {
        var multipart = (MimeMultipart) mimeMessage.getContent();
        var emailBody = multipart.getBodyPart(0);
        var firstAttachment = multipart.getBodyPart(1);
        MimeMultipart newMultipart = new MimeMultipart();
        newMultipart.addBodyPart(emailBody);
        newMultipart.addBodyPart(firstAttachment);
        mimeMessage.setContent(newMultipart);
    }

    @SneakyThrows
    private static MimeMessage addAttachmentToMimeMessage(MimeMessage mimeMessage, MimeBodyPart mimeBodyPart) {
        var multipart = (MimeMultipart) mimeMessage.getContent();
        multipart.addBodyPart(mimeBodyPart);
        mimeMessage.setContent(multipart);
        return mimeMessage;
    }

    @SneakyThrows
    public static ByteArrayOutputStream getMimeMessageOutputStream(MimeMessage mimeMessage) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        mimeMessage.writeTo(byteArrayOutputStream);
        return byteArrayOutputStream;
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

    public static String getMimeMessageInCDATATag(EmailField emailField) {
        return String.format("<![CDATA[%s]]>", getMimeMessageOutputStream(emailField));
    }

    public static String getMimeMessageInCDATATag(MimeMessage mimeMessage) {
        return String.format("<![CDATA[%s]]>", getMimeMessageOutputStream(mimeMessage));
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

    public static String[] getHeaderFromMimeMessage(MimeMessage mimeMessage, String headerName) {
        try {
            return mimeMessage.getHeader(headerName);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
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

}