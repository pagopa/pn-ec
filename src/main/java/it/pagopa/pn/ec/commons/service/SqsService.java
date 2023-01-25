package it.pagopa.pn.ec.commons.service;

import reactor.core.publisher.Mono;

public interface SqsService {

    <T> Mono<Void> send(final String queueName, final T queuePayload);
}
