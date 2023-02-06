package it.pagopa.pn.ec.commons.rest.call.gestorerepository;

import it.pagopa.pn.ec.commons.model.configurationproperties.endpoint.GestoreRepositoryEndpoint;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
@Slf4j
public class GestoreRepositoryCallImpl implements GestoreRepositoryCall {

    private final WebClient ecInternalWebClient;
    private final GestoreRepositoryEndpoint gestoreRepositoryEndpoint;

    public GestoreRepositoryCallImpl(WebClient ecInternalWebClient, GestoreRepositoryEndpoint gestoreRepositoryEndpoint) {
        this.ecInternalWebClient = ecInternalWebClient;
        this.gestoreRepositoryEndpoint = gestoreRepositoryEndpoint;
    }

    //  <-- CLIENT CONFIGURATION -->
    @Override
    public Mono<ClientConfigurationDto> getClientConfiguration(String xPagopaExtchCxId) {
        return ecInternalWebClient.get()
                                  .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpoint.getGetClientConfiguration())
                                                               .build(xPagopaExtchCxId))
                                  .retrieve()
                                  .onStatus(BAD_REQUEST::equals,
                                            clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException(
                                                    "Client not " + "found")))
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
        return ecInternalWebClient.get()
                                  .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpoint.getGetRequest()).build(requestIdx))
                                  .retrieve()
                                  .onStatus(BAD_REQUEST::equals,
                                            clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException("Request not " + "found"
                                            )))
                                  .bodyToMono(RequestDto.class);
    }

    @Override
    public Mono<RequestDto> insertRichiesta(RequestDto requestDto) {
        return ecInternalWebClient.post()
                                  .uri(gestoreRepositoryEndpoint.getPostRequest())
                                  .body(BodyInserters.fromValue(requestDto))
                                  .retrieve()
                                  .onStatus(FORBIDDEN::equals,
                                            clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException(
                                                    "Request already exists")))
                                  .bodyToMono(RequestDto.class);
    }

    @Override
    public Mono<RequestDto> updateRichiesta(String requestIdx, EventsDto eventsDto) {
        return ecInternalWebClient.put()
                .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpoint.getPatchRequest()).build(requestIdx)).bodyValue(eventsDto)
                .retrieve()
                .onStatus(BAD_REQUEST::equals,
                        clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException(
                                "Request requestIdx not  " + "found")))
                .bodyToMono(RequestDto.class);
    }

    @Override
    public Mono<Void> deleteRichiesta(String requestIdx) {
        return null;
    }
}
