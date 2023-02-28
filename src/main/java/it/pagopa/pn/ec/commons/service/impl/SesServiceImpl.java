package it.pagopa.pn.ec.commons.service.impl;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import it.pagopa.pn.ec.commons.exception.ses.SesSendException;
import it.pagopa.pn.ec.commons.service.SesService;
import it.pagopa.pn.ec.email.model.pojo.EmailField;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

@Service
@Slf4j
public class SesServiceImpl implements SesService {

	private final SesAsyncClient sesAsyncClient;

	public SesServiceImpl(SesAsyncClient sesAsyncClient) {
		this.sesAsyncClient = sesAsyncClient;
	}

	@Override
	public Mono<SendEmailResponse> send(EmailField field) {

		Destination destination = Destination.builder()//
				.toAddresses(new String[] { field.getTo() }).build();

		// Create the subject and body of the message.
		Content subject = Content.builder()//
				.data(field.getSubject())//
				.build();
		Content textBody = Content.builder()//
				.data(field.getTextBody())//
				.build();
		Content htmlBody = Content.builder()//
				.data(field.getHtmlBody())//
				.build();
		Body body = Body.builder()//
				.text(textBody)//
				.html(htmlBody)//
				.build();

		// Create a message with the specified subject and body.
		Message message = Message.builder()//
				.subject(subject)//
				.body(body)//
				.build();

		// Assemble the email.
		SendEmailRequest req = SendEmailRequest.builder()//
				.source(field.getFrom())//
				.destination(destination)//
				.message(message)//
				.build();

		CompletableFuture<SendEmailResponse> res = sesAsyncClient.sendEmail(req);

		return Mono.fromFuture(res)//

				.onErrorResume(throwable -> {//
					log.error(throwable.getMessage());
					return Mono.error(new SesSendException());
				})//

				.doOnSuccess(sendMessageResponse -> log.info("Send MAIL '{} 'to '{}' has returned a {} as status"//
						, field//
						, sendMessageResponse.sdkHttpResponse().statusCode()//
				));

	}

}
