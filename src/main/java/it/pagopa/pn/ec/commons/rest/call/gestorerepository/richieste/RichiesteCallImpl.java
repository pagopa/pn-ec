package it.pagopa.pn.ec.commons.rest.call.gestorerepository.richieste;

import it.pagopa.pn.ec.commons.constant.status.CommonStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class RichiesteCallImpl implements RichiesteCall {

    private final WebClient ecInternalWebClient;

    @Value("${gestore.repository.richieste.get}")
    String gestoreRepositoryGetRichiestaEndpoint;

    public RichiesteCallImpl(WebClient ecInternalWebClient) {
        this.ecInternalWebClient = ecInternalWebClient;
    }

    @Override
    public Mono<CommonStatus> getRichiesta(String idRequest) {
        return ecInternalWebClient.get().uri(String.format(gestoreRepositoryGetRichiestaEndpoint, idRequest)).retrieve().bodyToMono(
                CommonStatus.class);
    }
}
