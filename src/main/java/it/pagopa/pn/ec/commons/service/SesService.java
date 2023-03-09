package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.exception.ses.SesSendException;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;

import java.time.Duration;

public interface SesService {

	//  Spiegazione per jitter https://www.baeldung.com/resilience4j-backoff-jitter#jitter
	Retry DEFAULT_RETRY_STRATEGY = Retry.backoff(3, Duration.ofSeconds(2))//
										// .jitter(0.75)
										.onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
											throw new SesSendException.SesMaxRetriesExceededException();
										});

	Mono<SendRawEmailResponse> send(EmailField field);

}
