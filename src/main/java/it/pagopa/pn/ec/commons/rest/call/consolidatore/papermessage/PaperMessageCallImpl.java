package it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.consolidatore.PaperMessagesEndpointProperties;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperDeliveryProgressesResponse;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperReplicaRequest;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperReplicasProgressesResponse;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
                .exchangeToMono(clientResponse -> { // exchangeToMono is used to handle HTTP errors by clientResponse
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return clientResponse.bodyToMono(OperationResultCodeResponse.class);
                    } else if (clientResponse.statusCode().is4xxClientError()) {
                        // Case HTTP Error (4xx): check JSON Body
                        return clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                JsonNode errorNode = mapper.readTree(errorBody);
                                if (errorNode.has("resultCode")) {
                                    String resultCode = errorNode.get("resultCode").asText();
                                    List<String> nonRetryableErrors = Arrays.asList("400.01", "400.02", "401.00", "409.00");

                                    if (nonRetryableErrors.contains(resultCode)) {
                                        OffsetDateTime dateTime = null;
                                        if (errorNode.has("clientResponseTimeStamp")) {
                                            String timestamp = errorNode.get("clientResponseTimeStamp").asText();
                                            dateTime = OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                                        }

                                        OperationResultCodeResponse response = new OperationResultCodeResponse()
                                                .resultCode(resultCode)
                                                .resultDescription(errorNode.has("resultDescription") ? errorNode.get("resultDescription").asText() : "")
                                                .clientResponseTimeStamp(dateTime);

                                        if (errorNode.has("errorList") && errorNode.get("errorList").isArray()) {
                                            List<String> errors = new ArrayList<>();
                                            errorNode.get("errorList").forEach(error -> errors.add(error.asText()));
                                            response.setErrorList(errors);
                                        }

                                        return Mono.just(response); // return result without exception (no retry)
                                    }
                                }

                                // if not in special cases (400.01, 400.02, 401.00, 409.00) throw exception to retry
                                return Mono.error(new RestCallException("Errore HTTP: " + clientResponse.statusCode().value()));

                            } catch (Exception e) {
                                log.error("Errore nel parsing della risposta di errore: {}", e.getMessage());
                                return Mono.error(new RestCallException("Errore parsing JSON della risposta di errore"));
                            }
                        });
                    } else {
                        // other HTTP errors (5xx, 3xx, etc.) throw generic exception
                        return Mono.error(new RestCallException("Errore HTTP generico: " + clientResponse.statusCode().value()));
                    }
                });
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
