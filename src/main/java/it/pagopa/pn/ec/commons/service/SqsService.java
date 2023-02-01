package it.pagopa.pn.ec.commons.service;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public interface SqsService {

    <T> Mono<SendMessageResponse> send(final String queueName, final T queuePayload);
}
