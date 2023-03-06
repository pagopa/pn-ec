package it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ec.GestoreRepositoryEndpointProperties;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.*;

@Component
public class GestoreRepositoryCallImpl implements GestoreRepositoryCall {

    private final WebClient ecWebClient;
    private final GestoreRepositoryEndpointProperties gestoreRepositoryEndpointProperties;

    public GestoreRepositoryCallImpl(WebClient ecWebClient, GestoreRepositoryEndpointProperties gestoreRepositoryEndpointProperties) {
        this.ecWebClient = ecWebClient;
        this.gestoreRepositoryEndpointProperties = gestoreRepositoryEndpointProperties;
    }

    //  <-- CLIENT CONFIGURATION -->
    @Override
    public Mono<ClientConfigurationDto> getClientConfiguration(String xPagopaExtchCxId) {
        return ecWebClient.get()
                          .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.getClientConfiguration())
                                                       .build(xPagopaExtchCxId))
                          .retrieve()
                          .onStatus(NOT_FOUND::equals, clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                          .bodyToMono(ClientConfigurationDto.class);
    }

    //  <-- REQUEST -->
    @Override
    public Mono<ClientConfigurationDto> insertClientConfiguration(ClientConfigurationDto clientConfigurationDto) {
        return null;
    }

    @Override
    public Mono<ClientConfigurationDto> updateClientConfiguration(String xPagopaExtchCxId, ClientConfigurationDto clientConfigurationDto) {
        return null;
    }

    @Override
    public Mono<Void> deleteClientConfiguration(String xPagopaExtchCxId) {
        return null;
    }

    @Override
    public Mono<RequestDto> getRichiesta(String requestIdx) throws RestCallException.ResourceNotFoundException {
        return ecWebClient.get()
                          .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.getRequest()).build(requestIdx))
                          .retrieve()
                          .onStatus(NOT_FOUND::equals, clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                          .bodyToMono(RequestDto.class);
    }

    @Override
    public Mono<RequestDto> insertRichiesta(RequestDto requestDto) throws RestCallException.ResourceAlreadyExistsException {
        return ecWebClient.post()
                          .uri(gestoreRepositoryEndpointProperties.postRequest())
                          .body(BodyInserters.fromValue(requestDto))
                          .retrieve()
                          .onStatus(CONFLICT::equals, clientResponse -> Mono.error(new RestCallException.ResourceAlreadyExistsException()))
                          .bodyToMono(RequestDto.class);
    }

    @Override
    public Mono<RequestDto> patchRichiestaEvent(String requestIdx, EventsDto eventsDto) throws RestCallException.ResourceNotFoundException {
        return ecWebClient.patch()
                          .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.patchRequest()).build(requestIdx))
                          .bodyValue(eventsDto)
                          .retrieve()
                          .onStatus(NOT_FOUND::equals, clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                          .bodyToMono(RequestDto.class);
    }

    @Override
    public Mono<Void> deleteRichiesta(String requestIdx) {
        return null;
    }

    @Override
    public Mono<RequestDto> getRequestByMessageId(String messageId)
            throws RestCallException.ResourceNotFoundException, RestCallException.BadMessageIdProvidedException {
        return ecWebClient.get()
                          .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.getRequestByMessageId()).build(messageId))
                          .retrieve()
                          .onStatus(NOT_FOUND::equals, clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                          .onStatus(BAD_REQUEST::equals,
                                    clientResponse -> Mono.error(new RestCallException.BadMessageIdProvidedException()))
                          .bodyToMono(RequestDto.class);
    }

    @Override
    public Mono<RequestDto> setMessageIdInRequestMetadata(String requestIdx)
            throws RestCallException.ResourceNotFoundException, RestCallException.ISEForMessageIdCreationException {
        return ecWebClient.get()
                          .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.setMessageIdInRequestMetadata())
                                                       .build(requestIdx))
                          .retrieve()
                          .onStatus(NOT_FOUND::equals, clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                          .onStatus(INTERNAL_SERVER_ERROR::equals,
                                    clientResponse -> Mono.error(new RestCallException.ISEForMessageIdCreationException()))
                          .bodyToMono(RequestDto.class);
    }
}
