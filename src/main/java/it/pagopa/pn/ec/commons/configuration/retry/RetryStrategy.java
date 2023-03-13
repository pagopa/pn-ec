package it.pagopa.pn.ec.commons.configuration.retry;

import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;

public final class RetryStrategy {

    private RetryStrategy() {
        throw new IllegalStateException("RetryStrategy is a constant class");
    }

    public static final RetryBackoffSpec DEFAULT_BACKOFF_RETRY_STRATEGY = Retry.backoff(3, Duration.ofSeconds(2));
}
