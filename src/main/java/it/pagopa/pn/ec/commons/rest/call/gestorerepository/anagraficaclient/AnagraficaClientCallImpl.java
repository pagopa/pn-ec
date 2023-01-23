package it.pagopa.pn.ec.commons.rest.call.gestorerepository.anagraficaclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AnagraficaClientCallImpl implements AnagraficaClientCall {

    private final WebClient ecInternalWebClient;

    @Value("${gestore.repository.anagrafica.client.get}")
    String anagraficaClientGetClientEndpoint;

    public AnagraficaClientCallImpl(WebClient ecInternalWebClient) {
        this.ecInternalWebClient = ecInternalWebClient;
    }

    @Override
    public Mono<String> getClient(String idClient) {
        return ecInternalWebClient.get()
                                  .uri(String.format(anagraficaClientGetClientEndpoint, idClient))
                                  .retrieve()
                                  .bodyToMono(String.class);
    }
}
