package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.ses.SesSendException;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailAttachment;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.service.SesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.URLDataSource;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Properties;

@Service
@Slf4j
public class SesServiceImpl implements SesService {

    private final SesAsyncClient sesAsyncClient;

    public SesServiceImpl(SesAsyncClient sesAsyncClient) {
        this.sesAsyncClient = sesAsyncClient;
    }

    @Override
    public Mono<SendRawEmailResponse> send(EmailField field) {

        return Mono.fromCallable(() -> composeSendRawEmailRequest(field))
                   .flatMap(sendRawEmailRequest -> Mono.fromCompletionStage(sesAsyncClient.sendRawEmail(sendRawEmailRequest)))
                   .onErrorResume(throwable -> {
                       log.error(throwable.getMessage());
                       return Mono.error(new SesSendException());
                   })
                   .doOnSuccess(sendMessageResponse -> log.info("Send MAIL '{} 'to '{}' has returned a {} as status",
                                                                field.getSubject(),
                                                                field.getTo(),
                                                                sendMessageResponse.sdkHttpResponse().statusCode()));
    }

    private SendRawEmailRequest composeSendRawEmailRequest(EmailField field) throws IOException, MessagingException {

        Session session = Session.getInstance(new Properties(System.getProperties()));
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setFrom(field.getFrom());
        mimeMessage.setRecipients(RecipientType.TO, field.getTo());
        mimeMessage.setSubject(field.getSubject(), "UTF-8");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(field.getText(), field.getContentType());

        MimeMultipart msgBody = new MimeMultipart("alternative");
        msgBody.addBodyPart(htmlPart);

        MimeBodyPart wrap = new MimeBodyPart();
        wrap.setContent(msgBody);

        MimeMultipart msg = new MimeMultipart("mixed");
        msg.addBodyPart(wrap);

//		Add multiple files to attachment
        List<EmailAttachment> files = field.getEmailAttachments();
        for (EmailAttachment file : files) {
            MimeBodyPart messageBodyPart = new MimeBodyPart();

            DataSource source = new URLDataSource(new URL(file.getUrl()));
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
