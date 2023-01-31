package it.pagopa.pn.ec.commons.rest.call.gestorerepository;

import it.pagopa.pn.ec.commons.model.configurationproperties.endpoint.GestoreRepositoryEndpoint;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
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
                                  .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpoint.getClientConfiguration)
                                                               .build(xPagopaExtchCxId))
                                  .retrieve()
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
    public Mono<RequestDto> getRichiesta(String requestIdx) {
        return ecInternalWebClient.get()
                                  .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpoint.getRequest).build(requestIdx))
                                  .retrieve()
                                  .bodyToMono(RequestDto.class);
    }

    @Override
    public Mono<RequestDto> insertRichiesta(RequestDto requestDto) {
        return null;
    }

    @Override
    public Mono<RequestDto> updateRichiesta(String requestIdx, RequestDto requestDto) {
        return null;
    }

    @Override
    public Mono<Void> deleteRichiesta(String requestIdx) {
        return null;
    }
}
