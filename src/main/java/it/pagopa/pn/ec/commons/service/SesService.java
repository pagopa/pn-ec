package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.exception.ses.SesSendException;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;

import static it.pagopa.pn.ec.commons.configuration.retry.RetryStrategy.DEFAULT_BACKOFF_RETRY_STRATEGY;

public interface SesService {

    Retry DEFAULT_RETRY_STRATEGY = DEFAULT_BACKOFF_RETRY_STRATEGY.onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
        throw new SesSendException.SesMaxRetriesExceededException();
    });

    Mono<SendRawEmailResponse> send(EmailField field);
}
