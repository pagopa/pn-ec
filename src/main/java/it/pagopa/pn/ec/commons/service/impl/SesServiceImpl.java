package it.pagopa.pn.ec.commons.service.impl;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Properties;

import it.pagopa.pn.ec.util.LogSanitizer;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.springframework.stereotype.Service;
import it.pagopa.pn.ec.commons.exception.ses.SesSendException;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.service.SesService;
import lombok.CustomLog;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;

import static org.apache.commons.codec.CharEncoding.UTF_8;

@Service
@CustomLog
public class SesServiceImpl implements SesService {

    private final SesAsyncClient sesAsyncClient;
    private final LogSanitizer logSanitizer;

    public SesServiceImpl(SesAsyncClient sesAsyncClient, LogSanitizer logSanitizer) {
        this.sesAsyncClient = sesAsyncClient;
        this.logSanitizer = logSanitizer;
    }

    @Override
    public Mono<SendRawEmailResponse> send(EmailField field) {
        log.info(CLIENT_METHOD_INVOCATION_WITH_ARGS, SES_SEND_MAIL, field);
        return Mono.fromCallable(() -> composeSendRawEmailRequest(field))
                   .flatMap(sendRawEmailRequest -> Mono.fromCompletionStage(sesAsyncClient.sendRawEmail(sendRawEmailRequest)))
                   .onErrorResume(throwable -> {
                       log.error(logSanitizer.sanitize(throwable.getMessage()));
                       return Mono.error(new SesSendException());
                   })
                   .doOnSuccess(sendMessageResponse -> log.debug(CLIENT_METHOD_RETURN, SES_SEND_MAIL, sendMessageResponse));
    }

    private SendRawEmailRequest composeSendRawEmailRequest(EmailField field) throws IOException, MessagingException {

        Session session = Session.getInstance(new Properties(System.getProperties()));
        MimeMessage mimeMessage = new MimeMessage(session);

        mimeMessage.setFrom(new InternetAddress(field.getFrom(), "", UTF_8));
        mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(field.getTo(), "", UTF_8));
        mimeMessage.setSubject(field.getSubject(), UTF_8);

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(field.getText(), field.getContentType());

        MimeMultipart msgBody = new MimeMultipart("alternative");
        msgBody.addBodyPart(htmlPart);

        MimeBodyPart wrap = new MimeBodyPart();
        wrap.setContent(msgBody);

        MimeMultipart msg = new MimeMultipart("mixed");
        msg.addBodyPart(wrap);

        // Add multiple files to attachment
        List<EmailAttachment> files = field.getEmailAttachments();
        for (EmailAttachment file : files) {
            MimeBodyPart messageBodyPart = new MimeBodyPart();

            var byteArrayOutputStream = (ByteArrayOutputStream) file.getContent();
            DataSource source = new ByteArrayDataSource(byteArrayOutputStream.toByteArray(), APPLICATION_OCTET_STREAM_VALUE);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(file.getNameWithExtension());

            msg.addBodyPart(messageBodyPart);
        }

        mimeMessage.setContent(msg);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mimeMessage.writeTo(outputStream);

        SdkBytes data = SdkBytes.fromByteBuffer(ByteBuffer.wrap(outputStream.toByteArray()));

        return SendRawEmailRequest.builder().rawMessage(builder -> builder.data(data)).build();
    }

}
