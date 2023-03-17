package it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage;

import java.time.Duration;

import org.openapitools.client.model.OperationResultCodeResponse;
import org.openapitools.client.model.PaperDeliveryProgressesResponse;
import org.openapitools.client.model.PaperEngageRequest;
import org.openapitools.client.model.PaperReplicaRequest;
import org.openapitools.client.model.PaperReplicasProgressesResponse;

import it.pagopa.pn.ec.commons.exception.cartaceo.CartaceoSendException;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public interface PaperMessageCall {

	//  Spiegazione per jitter https://www.baeldung.com/resilience4j-backoff-jitter#jitter
	Retry DEFAULT_RETRY_STRATEGY = Retry.backoff(3, Duration.ofSeconds(2))
			//                                        .jitter(0.75)
			.onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
				throw new CartaceoSendException.CartaceoMaxRetriesExceededException();
			});

	//  <-- ENGAGEMENT -->
	Mono<OperationResultCodeResponse> putRequest(PaperEngageRequest paperEngageRequest) throws RestCallException.ResourceAlreadyInProgressException;

	Mono<OperationResultCodeResponse> putDuplicateRequest(PaperReplicaRequest paperReplicaRequest) throws RestCallException.ResourceAlreadyInProgressException;

	//  <-- PROGRESS -->
	Mono<PaperDeliveryProgressesResponse> getProgress(String requestId) throws RestCallException.ResourceNotFoundException;

	Mono<PaperReplicasProgressesResponse> getDuplicateProgress(String requestId) throws RestCallException.ResourceNotFoundException;

}
