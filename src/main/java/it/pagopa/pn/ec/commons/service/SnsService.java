package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.time.Duration;

public interface SnsService {

    Mono<PublishResponse> send(String phoneNumber, String message);

}
