package it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint .internal.consolidatore.PaperMessagesEndpointProperties;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperDeliveryProgressesResponse;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperReplicaRequest;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperReplicasProgressesResponse;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest;
import it.pagopa.pn.ec.rest.v1.dto.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Component
public class PaperMessageCallImpl implements PaperMessageCall {

    private final WebClient consolidatoreWebClient;
    private final PaperMessagesEndpointProperties paperMessagesEndpointProperties;

    public PaperMessageCallImpl(WebClient consolidatoreWebClient, PaperMessagesEndpointProperties paperMessagesEndpointProperties) {
        this.consolidatoreWebClient = consolidatoreWebClient;
        this.paperMessagesEndpointProperties = paperMessagesEndpointProperties;
    }

    @Override
    public Mono<OperationResultCodeResponse> putRequest(PaperEngageRequest paperEngageRequest)
            throws RestCallException.ResourceAlreadyInProgressException {
        return consolidatoreWebClient.post()
                                     .uri(paperMessagesEndpointProperties.putRequest())
                                     .bodyValue(paperEngageRequest)
                                     .retrieve()
                                     .bodyToMono(OperationResultCodeResponse.class);
    }

    @Override
    public Mono<OperationResultCodeResponse> putDuplicateRequest(PaperReplicaRequest paperReplicaRequest)
            throws RestCallException.ResourceAlreadyInProgressException {
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
        return consolidatoreWebClient.get()
                                     .uri(uriBuilder -> uriBuilder.path(paperMessagesEndpointProperties.getRequest()).build(requestId))
                                     .retrieve()
                                     .onStatus(NOT_FOUND::equals,
                                               clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                                     .bodyToMono(PaperDeliveryProgressesResponse.class);
    }

    @Override
    public Mono<PaperReplicasProgressesResponse> getDuplicateProgress(String requestId) throws RestCallException.ResourceNotFoundException {
        return consolidatoreWebClient.get()
                                     .uri(uriBuilder -> uriBuilder.path(paperMessagesEndpointProperties.getDuplicateRequest()).build(requestId))
                                     .retrieve()
                                     .onStatus(NOT_FOUND::equals,
                                               clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                                     .bodyToMono(PaperReplicasProgressesResponse.class);
    }
}
