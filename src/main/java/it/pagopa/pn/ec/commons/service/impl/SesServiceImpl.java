package it.pagopa.pn.ec.commons.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.springframework.stereotype.Service;

import it.pagopa.pn.ec.commons.exception.ses.SesSendException;
import it.pagopa.pn.ec.commons.service.SesService;
import it.pagopa.pn.ec.email.model.pojo.EmailField;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;

@Service
@Slf4j
public class SesServiceImpl implements SesService {

	private final SesAsyncClient sesAsyncClient;

	public SesServiceImpl(SesAsyncClient sesAsyncClient) {
		this.sesAsyncClient = sesAsyncClient;
	}

	private SendRawEmailRequest composeSendRawEmailRequest(EmailField field) throws IOException, MessagingException {

		Session session = Session.getInstance(new Properties(System.getProperties()));
		MimeMessage mimeMessage = new MimeMessage(session);
		mimeMessage.setFrom(field.getFrom());
		mimeMessage.setRecipients(RecipientType.TO, field.getTo());
		mimeMessage.setSubject(field.getSubject(), "UTF-8");

		MimeBodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent(field.getHtmlBody(), "text/html; charset=UTF-8");

		MimeMultipart msgBody = new MimeMultipart("alternative");
		msgBody.addBodyPart(htmlPart);

		MimeBodyPart wrap = new MimeBodyPart();
		wrap.setContent(msgBody);

		MimeMultipart msg = new MimeMultipart("mixed");
		msg.addBodyPart(wrap);

		// Add multiple files to attachment
		List<String> files = field.getAttachmentsUrls();
		for (int idx = 1; idx <= files.size(); idx++) {
			MimeBodyPart messageBodyPart = new MimeBodyPart();

			DataSource source = new FileDataSource(files.get(i));
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName("attach-" + idx + ".pdf");

			msg.addBodyPart(messageBodyPart);
		}

		mimeMessage.setContent(msg);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		mimeMessage.writeTo(outputStream);

		SdkBytes data = SdkBytes.fromByteBuffer(ByteBuffer.wrap(outputStream.toByteArray()));//

		RawMessage rawMessage = RawMessage.builder()//
				.data(data)//
				.build();

		SendRawEmailRequest rawEmailRequest = SendRawEmailRequest.builder()//
				.rawMessage(rawMessage)//
				.build();

		return rawEmailRequest;
	}

	@Override
	public Mono<SendRawEmailResponse> send(EmailField field) {

		try {

			// Assemble the email.
			SendRawEmailRequest reqRaw = composeSendRawEmailRequest(field);

			CompletableFuture<SendRawEmailResponse> resRaw = sesAsyncClient.sendRawEmail(reqRaw);

			return Mono.fromFuture(resRaw)//

					.onErrorResume(throwable -> {//
						log.error(throwable.getMessage());
						return Mono.error(new SesSendException());
					})//

					.doOnSuccess(sendMessageResponse -> log.info("Send MAIL '{} 'to '{}' has returned a {} as status"//
							, field.getSubject()//
							, field.getTo()//
							, sendMessageResponse.sdkHttpResponse().statusCode()//
					));

		} catch (IOException | MessagingException e) {
			log.error(e.getMessage());
			return Mono.error(new SesSendException());
		}

	}

}
