package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public interface SqsService {

    <T> Mono<SendMessageResponse> send(final String queueName, final T queuePayload) throws SqsPublishException;

    Flux<Message> getAllQueueMessage(final String queueName, int maxNumberOfMessages);

    Mono<DeleteMessageResponse> deleteMessageFromQueue(final Message message, final String queueName);

    Mono<String> getQueueUrlFromName(final String queueName);
}
