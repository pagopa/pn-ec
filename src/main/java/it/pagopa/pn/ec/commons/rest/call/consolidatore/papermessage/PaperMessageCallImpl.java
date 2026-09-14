package it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage;

import io.github.resilience4j.ratelimiter.RateLimiter;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.consolidatore.PaperMessagesEndpointProperties;
import it.pagopa.pn.ec.commons.exception.JsonStringToObjectException;
import it.pagopa.pn.ec.commons.exception.cartaceo.ConsolidatoreException;
import it.pagopa.pn.ec.commons.exception.consolidatore.RateLimitExceededException;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.utils.JsonUtils;
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
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;


import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
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
    private final Duration progressesTimeout;
    private final Duration progressesRetryAfter;

    public PaperMessageCallImpl(@Qualifier("consolidatoreWebClient")WebClient consolidatoreWebClient, PaperMessagesEndpointProperties paperMessagesEndpointProperties, JsonUtils jsonUtils,
                                @Value("${pn.ec.max-concurrent-requests}") int maxConcurrentRequests,
                                @Value("${pn.ec.max-retry-for-rate-limiter}") int maxRetryForRateLimiter,
                                @Value("${pn.ec.max-retry-for-rate-limiter-seconds}") int maxRetryForRateLimiterSeconds,
                                @Value("${pn.ec.consolidatore.progresses-timeout-seconds}") int progressesTimeoutSeconds,
                                @Value("${pn.ec.consolidatore.progresses-retry-after-seconds}") int progressesRetryAfterSeconds,
                                @Autowired(required = false) @Qualifier("rateLimiterConsolidatore") RateLimiter rateLimiter) {
        this.consolidatoreWebClient = consolidatoreWebClient;
        this.paperMessagesEndpointProperties = paperMessagesEndpointProperties;
        this.jsonUtils = jsonUtils;
        this.semaphore = new Semaphore(maxConcurrentRequests);
        this.rateLimiter = rateLimiter;
        this.progressesTimeout = Duration.ofSeconds(progressesTimeoutSeconds);
        this.progressesRetryAfter = Duration.ofSeconds(progressesRetryAfterSeconds);
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
                .exchangeToMono(clientResponse -> clientResponse.statusCode().is2xxSuccessful() ?
                        clientResponse.bodyToMono(PaperDeliveryProgressesResponse.class) :
                        handleProgressError(clientResponse))
                .timeout(progressesTimeout)
                .onErrorMap(TimeoutException.class, e -> new ConsolidatoreException.CallTimeoutException(progressesTimeout.toString()))
                .onErrorMap(WebClientRequestException.class, e -> new ConsolidatoreException.ConnectionFailedException(e.getMessage()))
                .onErrorMap(DecodingException.class, e -> new ConsolidatoreException.PermanentException(String.format("Non conforming response: %s", e.getMessage())));
    }

    private Mono<PaperDeliveryProgressesResponse> handleProgressError(ClientResponse clientResponse) {
        HttpStatusCode statusCode = clientResponse.statusCode();
        Duration retryAfter = readRetryAfter(clientResponse);
        return clientResponse.bodyToMono(String.class)
                             .defaultIfEmpty(StringUtils.EMPTY)
                             .map(this::readOperationResult)
                             .flatMap(operationResult -> Mono.error(buildProgressException(statusCode, operationResult, retryAfter)));
    }

    private ConsolidatoreException buildProgressException(HttpStatusCode statusCode, OperationResultCodeResponse operationResult, Duration retryAfter) {
        if (statusCode.isSameCodeAs(NOT_FOUND)) {
            return new ConsolidatoreException.RequestIdNotFoundException(operationResult);
        }
        if (statusCode.isSameCodeAs(TOO_MANY_REQUESTS)) {
            return new ConsolidatoreException.RateLimitedException(operationResult, retryAfter);
        }
        return statusCode.is4xxClientError() ? new ConsolidatoreException.PermanentException(operationResult, statusCode.value()) :
                new ConsolidatoreException.TemporaryException(operationResult, statusCode.value());
    }

    private OperationResultCodeResponse readOperationResult(String body) {
        try {
            return jsonUtils.convertJsonStringToObject(body, OperationResultCodeResponse.class);
        } catch (JsonStringToObjectException e) {
            log.warn("Non conforming error response from consolidatore: {}", body);
            return new OperationResultCodeResponse().resultDescription(body);
        }
    }

    private Duration readRetryAfter(ClientResponse clientResponse) {
        return clientResponse.headers()
                             .header(RETRY_AFTER)
                             .stream()
                             .filter(StringUtils::isNumeric)
                             .findFirst()
                             .map(seconds -> Duration.ofSeconds(Long.parseLong(seconds)))
                             .orElse(progressesRetryAfter);
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
