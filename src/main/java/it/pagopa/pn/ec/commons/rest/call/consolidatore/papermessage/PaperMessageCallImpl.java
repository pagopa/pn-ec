package it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.consolidatore.PaperMessagesEndpointProperties;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.consolidatore.utils.PaperResult;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperDeliveryProgressesResponse;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperReplicaRequest;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperReplicasProgressesResponse;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Component
@CustomLog
public class PaperMessageCallImpl implements PaperMessageCall {

    private final WebClient consolidatoreWebClient;
    private final PaperMessagesEndpointProperties paperMessagesEndpointProperties;
    private static final List<String> NON_RETRYABLE_ERRORS = Arrays.asList(
            PaperResult.SYNTAX_ERROR_CODE,
            PaperResult.SEMANTIC_ERROR_CODE,
            PaperResult.AUTHENTICATION_ERROR_CODE,
            PaperResult.DUPLICATED_REQUEST_CODE);

    public PaperMessageCallImpl(WebClient consolidatoreWebClient, PaperMessagesEndpointProperties paperMessagesEndpointProperties) {
        this.consolidatoreWebClient = consolidatoreWebClient;
        this.paperMessagesEndpointProperties = paperMessagesEndpointProperties;
    }

    @Override
    public Mono<OperationResultCodeResponse> putRequest(PaperEngageRequest paperEngageRequest) {
        return consolidatoreWebClient
                .post()
                .uri(paperMessagesEndpointProperties.putRequest())
                .bodyValue(paperEngageRequest)
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return clientResponse.bodyToMono(OperationResultCodeResponse.class);
                    } else if (clientResponse.statusCode().is4xxClientError()) {
                        return handleClientError(clientResponse);
                    } else {
                        return Mono.error(new RestCallException("HTTP generic error: " + clientResponse.statusCode().value()));
                    }
                }).onErrorResume(e -> {
                    if (e instanceof RestCallException) {
                        log.error(e.getMessage());
                        return Mono.error(e);
                    } else {
                        log.error("Error in parsing response: {}", e.getMessage());
                        return Mono.error(new RestCallException("Error in parsing response"));
                    }
                });
    }

    private Mono<OperationResultCodeResponse> handleClientError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(OperationResultCodeResponse.class).flatMap(operationResultCodeResponse -> {
            if (isNonRetryableError(operationResultCodeResponse.getResultCode())) {
                return Mono.just(operationResultCodeResponse);
            }

            return Mono.error(new RestCallException("Errore HTTP: " + clientResponse.statusCode().value()));
        });
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
