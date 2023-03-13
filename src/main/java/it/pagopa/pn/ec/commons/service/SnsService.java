package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static it.pagopa.pn.ec.commons.configuration.retry.RetryStrategy.DEFAULT_BACKOFF_RETRY_STRATEGY;

public interface SnsService {

    Retry DEFAULT_RETRY_STRATEGY = DEFAULT_BACKOFF_RETRY_STRATEGY.onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
        throw new SnsSendException.SnsMaxRetriesExceededException();
    });

    Mono<PublishResponse> send(String phoneNumber, String message);
}
