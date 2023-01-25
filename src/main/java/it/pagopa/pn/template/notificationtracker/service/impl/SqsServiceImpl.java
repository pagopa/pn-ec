package it.pagopa.pn.template.notificationtracker.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.template.notificationtracker.exception.SqsConvertToJsonPayloadException;
import it.pagopa.pn.template.notificationtracker.exception.SqsPublishException;
import it.pagopa.pn.template.notificationtracker.service.SqsService;
import it.pagopa.pn.template.notificationtracker.exception.SqsCharacterInPayloadNotAllowedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.InvalidMessageContentsException;
import software.amazon.awssdk.services.sqs.model.SqsException;


@Service
@Slf4j
public class SqsServiceImpl implements SqsService {

	private final SqsAsyncClient sqsAsyncClient;
	private final ObjectMapper objectMapper;

	public SqsServiceImpl(SqsAsyncClient sqsAsyncClient, ObjectMapper objectMapper) {
		this.sqsAsyncClient = sqsAsyncClient;
		this.objectMapper = objectMapper;
	}

		@Override
		public <T> Mono<Void> send(String queueName, T queuePayload) {

			String queuePayloadString;
			try {
				queuePayloadString = objectMapper.writeValueAsString(queuePayload);
			} catch (JsonProcessingException e) {
				throw new SqsConvertToJsonPayloadException(queuePayload);
			}

			log.info("Send to {} queue with payload ↓\n{}", queueName, queuePayloadString);

			GetQueueUrlRequest getQueueUrlRequest = GetQueueUrlRequest.builder().queueName(queueName).build();

			return Mono.fromFuture(sqsAsyncClient.sendMessage(builder -> {
						try {
							builder.queueUrl(getQueueUrlRequest.queueName()).messageBody(queuePayloadString);
						} catch (InvalidMessageContentsException e) {
							throw new SqsCharacterInPayloadNotAllowedException(queuePayloadString);
						} catch (SqsException e) {
							throw new SqsPublishException(queueName);
						}
					}))
					.doOnNext(sendMessageResponse -> log.info("Publishing on {} has returned a {} as status",
							queueName,
							sendMessageResponse.sdkHttpResponse().statusCode()))
					.doOnError(throwable -> log.info(throwable.getMessage(), throwable))
					.then();
		}
}
