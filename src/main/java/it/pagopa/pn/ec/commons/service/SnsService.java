package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.time.Duration;

public interface SnsService {

    //  Spiegazione per jitter https://www.baeldung.com/resilience4j-backoff-jitter#jitter
    Retry DEFAULT_RETRY_STRATEGY = Retry.backoff(3, Duration.ofSeconds(2))
//                                        .jitter(0.75)
                                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                            throw new SnsSendException.SnsMaxRetriesExceededException();
                                        });

    Mono<PublishResponse> send(String phoneNumber, String message);
}
