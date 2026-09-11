package it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage;

import io.github.resilience4j.ratelimiter.RateLimiter;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.consolidatore.PaperMessagesEndpointProperties;
import it.pagopa.pn.ec.commons.exception.cartaceo.ConsolidatoreException;
import it.pagopa.pn.ec.commons.exception.consolidatore.RateLimitExceededException;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.utils.JsonUtils;
import it.pagopa.pn.ec.consolidatore.utils.PaperResult;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperDeliveryProgressesResponse;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperReplicaRequest;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperReplicasProgressesResponse;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;


import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static it.pagopa.pn.ec.util.EmfLogUtils.*;


@Component
@CustomLog
public class PaperMessageCallImpl implements PaperMessageCall {

    private final WebClient consolidatoreWebClient;
    private final PaperMessagesEndpointProperties paperMessagesEndpointProperties;
    private final JsonUtils jsonUtils;
    private final Semaphore semaphore;
    private final RateLimiter rateLimiter;
    private final Retry rateLimiterRetryStrategy;

    public PaperMessageCallImpl(@Qualifier("consolidatoreWebClient")WebClient consolidatoreWebClient, PaperMessagesEndpointProperties paperMessagesEndpointProperties, JsonUtils jsonUtils,
                                @Value("${pn.ec.max-concurrent-requests}") int maxConcurrentRequests,
                                @Value("${pn.ec.max-retry-for-rate-limiter}") int maxRetryForRateLimiter,
                                @Value("${pn.ec.max-retry-for-rate-limiter-seconds}") int maxRetryForRateLimiterSeconds,
                                @Autowired(required = false) @Qualifier("rateLimiterConsolidatore") RateLimiter rateLimiter) {
        this.consolidatoreWebClient = consolidatoreWebClient;
        this.paperMessagesEndpointProperties = paperMessagesEndpointProperties;
        this.jsonUtils = jsonUtils;
        this.semaphore = new Semaphore(maxConcurrentRequests);
        this.rateLimiter = rateLimiter;
        this.rateLimiterRetryStrategy = Retry.fixedDelay(maxRetryForRateLimiter, Duration.ofSeconds(maxRetryForRateLimiterSeconds))
                .filter(ex -> ex instanceof RateLimitExceededException)
                .doBeforeRetry(retrySignal -> log.info(
                        "Retry {} per RateLimiter su putRequest, causa: {}",
                        retrySignal.totalRetries(),
                        retrySignal.failure().getMessage()));
        log.info("PaperMessageCallImpl maxConcurrentRequests={} - maxRetryForRateLimiter={}/seconds={} - rateLimiter presente? {}", maxConcurrentRequests, maxRetryForRateLimiter, maxRetryForRateLimiterSeconds, rateLimiter!=null);
    }

    @Override
    public Mono<OperationResultCodeResponse> putRequest(PaperEngageRequest paperEngageRequest) {
        return Mono.defer(() -> executePutRequestWithSemaphore(paperEngageRequest))
                .retryWhen(rateLimiterRetryStrategy)
                //dopo 8 tentativi se ancora c'è errore, non facciamo nulla
                .onErrorResume(RateLimitExceededException.class, ex -> {
                    log.warn("Max retry RateLimiter raggiunti, request ignorata: {}", paperEngageRequest);
                    return Mono.empty();
                });
    }

    private Mono<OperationResultCodeResponse> executePutRequestWithSemaphore(PaperEngageRequest paperEngageRequest) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Thread interrupted while acquiring semaphore", e);
        }
            if (rateLimiter != null && !rateLimiter.acquirePermission()) {
                semaphore.release();
                throw new RateLimitExceededException("Max requests per minute exceeded");
            }
            long startTimeCalling = System.currentTimeMillis();
            return consolidatoreWebClient
                    .post()
                    .uri(paperMessagesEndpointProperties.putRequest())
                    .bodyValue(paperEngageRequest)
                    .exchangeToMono(clientResponse -> {
                        long elapsedTime = System.currentTimeMillis() - startTimeCalling;
                        trackMetricsConsolidatore(elapsedTime);
                        if (clientResponse.statusCode().is2xxSuccessful()) {
                            return clientResponse.bodyToMono(OperationResultCodeResponse.class);
                        } else if (clientResponse.statusCode().is4xxClientError()) {
                            return handleClientError(clientResponse);
                        } else {
                            return handleServerError(clientResponse);
                        }
                    })
                    .doFinally(signalType -> semaphore.release());
    }

    @Override
    public Mono<OperationResultCodeResponse> putDuplicateRequest(PaperReplicaRequest paperReplicaRequest)
            throws RestCallException.ResourceAlreadyInProgressException {
        log.logInvokingExternalService(CONSOLIDATORE_SERVICE, SEND_PAPER_REPLICAS_ENGAGEMENT_REQUEST);
        return consolidatoreWebClient.put()
                                     .uri(paperMessagesEndpointProperties.putDuplicateRequest())
                                     .bodyValue(paperReplicaRequest)
                                     .retrieve()
                                     .onStatus(FORBIDDEN::equals,
                                               clientResponse -> Mono.error(new RestCallException.ResourceAlreadyInProgressException()))
                                     .bodyToMono(OperationResultCodeResponse.class);
    }

    @Override
    public Mono<PaperDeliveryProgressesResponse> getProgress(String requestId) {
        log.logInvokingExternalService(CONSOLIDATORE_SERVICE, GET_PAPER_ENGAGE_PROGRESSES);
        return consolidatoreWebClient.get()
                .uri(UriComponentsBuilder.fromUriString(paperMessagesEndpointProperties.getRequestProgress()).build(requestId).toString())
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return clientResponse.bodyToMono(PaperDeliveryProgressesResponse.class);
                    } else if (clientResponse.statusCode().is4xxClientError()) {
                        return handleClientError(clientResponse)
                                .flatMap(operationResult -> Mono.error(new ConsolidatoreException.PermanentException(operationResult,
                                                                                                                       clientResponse.statusCode().value())));
                    } else {
                        return handleServerError(clientResponse)
                                .flatMap(operationResult -> Mono.error(new ConsolidatoreException.TemporaryException(operationResult,
                                                                                                                       clientResponse.statusCode().value())));
                    }
                });
    }

    @Override
    public Mono<PaperReplicasProgressesResponse> getDuplicateProgress(String requestId) throws RestCallException.ResourceNotFoundException {
        log.logInvokingExternalService(CONSOLIDATORE_SERVICE, GET_PAPER_REPLICAS_PROGRESSES_REQUEST);
        return consolidatoreWebClient.get()
                                     .uri( UriComponentsBuilder.fromPath(paperMessagesEndpointProperties.getDuplicateRequestProgress()).build(requestId).toString())
                                     .retrieve()
                                     .onStatus(NOT_FOUND::equals,
                                               clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                                     .bodyToMono(PaperReplicasProgressesResponse.class);
    }

    private Mono<OperationResultCodeResponse> handleClientError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(String.class).flatMap(response -> {
            OperationResultCodeResponse operationResultCodeResponse = jsonUtils.convertJsonStringToObject(response, OperationResultCodeResponse.class);
            String resultCode = operationResultCodeResponse.getResultCode();
            // La response non è conforme al formato che ci aspettiamo.
            if (StringUtils.isBlank(resultCode)) {
                String errStr = String.format("Missing result code or non conforming response: %s", response);
                log.warn(errStr);
                return clientResponse.createException()
                                     .flatMap(e -> Mono.error(new ConsolidatoreException.PermanentException(errStr, clientResponse.statusCode().value())));
            }
            return Mono.just(operationResultCodeResponse);
        });
    }

    private Mono<OperationResultCodeResponse> handleServerError(ClientResponse clientResponse) {
        return clientResponse
                .createException()
                .flatMap(e -> Mono.error(new ConsolidatoreException.TemporaryException(e.getMessage(), clientResponse.statusCode().value())));
    }

}
