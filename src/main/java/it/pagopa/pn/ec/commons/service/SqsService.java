package it.pagopa.pn.ec.commons.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import reactor.core.publisher.Mono;

public interface SqsService {

    int MAXIMUM_LISTENING_TIME = 25;

    <T> Mono<Void> send(final String queueName, final T queuePayload);

    <T> Mono<Void> incomingMessageFlow(final T queuePayload, final Acknowledgment acknowledgment);
}
