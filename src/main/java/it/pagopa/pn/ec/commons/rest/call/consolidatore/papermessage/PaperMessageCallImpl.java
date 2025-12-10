package it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.consolidatore.PaperMessagesEndpointProperties;
import it.pagopa.pn.ec.commons.exception.cartaceo.ConsolidatoreException;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.utils.JsonUtils;
import it.pagopa.pn.ec.consolidatore.utils.PaperResult;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperDeliveryProgressesResponse;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperReplicaRequest;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperReplicasProgressesResponse;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.ec.util.EmfLogUtils;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

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
    private static final List<String> NON_RETRYABLE_ERRORS = Arrays.asList(
            PaperResult.SYNTAX_ERROR_CODE,
            PaperResult.SEMANTIC_ERROR_CODE,
            PaperResult.AUTHENTICATION_ERROR_CODE,
            PaperResult.DUPLICATED_REQUEST_CODE);

    public PaperMessageCallImpl(WebClient consolidatoreWebClient, PaperMessagesEndpointProperties paperMessagesEndpointProperties, JsonUtils jsonUtils) {
        this.consolidatoreWebClient = consolidatoreWebClient;
        this.paperMessagesEndpointProperties = paperMessagesEndpointProperties;
        this.jsonUtils = jsonUtils;
    }

    @Override
    public Mono<OperationResultCodeResponse> putRequest(PaperEngageRequest paperEngageRequest) {
        long startTimeCalling = System.currentTimeMillis();
        return consolidatoreWebClient
                .post()
                .uri(paperMessagesEndpointProperties.putRequest())
                .bodyValue(paperEngageRequest)
                .exchangeToMono(clientResponse -> {
                    long elapsedTime = System.currentTimeMillis() - startTimeCalling;
                    trackMetricsConsolidatore(clientResponse.statusCode().value(), elapsedTime);
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return clientResponse.bodyToMono(OperationResultCodeResponse.class);
                    } else if (clientResponse.statusCode().is4xxClientError()) {
                        return handleClientError(clientResponse);
                    } else {
                        return handleServerError(clientResponse);
                    }
                }).onErrorResume(TimeoutException.class, ex -> {
                    long elapsedTime = System.currentTimeMillis() - startTimeCalling;

                    // timeout -> statusCode = 999
                    trackMetricsConsolidatore(999, elapsedTime);

                    return Mono.error(new ConsolidatoreException.TemporaryException("Timeout calling consolidatore"));
                });
    }

    private Mono<OperationResultCodeResponse> handleClientError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(String.class).flatMap(response -> {
            OperationResultCodeResponse operationResultCodeResponse = jsonUtils.convertJsonStringToObject(response, OperationResultCodeResponse.class);
            String resultCode = operationResultCodeResponse.getResultCode();
            // La response non è conforme al formato che ci aspettiamo.
            if (StringUtils.isBlank(resultCode)) {
                String errStr = String.format("Missing result code or non conforming response: %s", response);
                log.warn(errStr);
                return clientResponse.createException().flatMap(e -> Mono.error(new ConsolidatoreException.PermanentException(errStr)));
            }
            return Mono.just(operationResultCodeResponse);
        });
    }

    private Mono<OperationResultCodeResponse> handleServerError(ClientResponse clientResponse) {
        return clientResponse
                .createException()
                .flatMap(e -> Mono.error(new ConsolidatoreException.TemporaryException(e.getMessage())));
    }

    private boolean isNonRetryableError(String resultCode) {
        return resultCode != null && NON_RETRYABLE_ERRORS.contains(resultCode);
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
    public Mono<PaperDeliveryProgressesResponse> getProgress(String requestId) throws RestCallException.ResourceNotFoundException {
        log.logInvokingExternalService(CONSOLIDATORE_SERVICE, GET_PAPER_ENGAGE_PROGRESSES);
        return consolidatoreWebClient.get()
                                     .uri(uriBuilder -> uriBuilder.path(paperMessagesEndpointProperties.getRequest()).build(requestId))
                                     .retrieve()
                                     .onStatus(NOT_FOUND::equals,
                                               clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                                     .bodyToMono(PaperDeliveryProgressesResponse.class);
    }

    @Override
    public Mono<PaperReplicasProgressesResponse> getDuplicateProgress(String requestId) throws RestCallException.ResourceNotFoundException {
        log.logInvokingExternalService(CONSOLIDATORE_SERVICE, GET_PAPER_REPLICAS_PROGRESSES_REQUEST);
        return consolidatoreWebClient.get()
                                     .uri(uriBuilder -> uriBuilder.path(paperMessagesEndpointProperties.getDuplicateRequest()).build(requestId))
                                     .retrieve()
                                     .onStatus(NOT_FOUND::equals,
                                               clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                                     .bodyToMono(PaperReplicasProgressesResponse.class);
    }


}
