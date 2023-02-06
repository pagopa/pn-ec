package it.pagopa.pn.ec.notificationtracker.service.callmachinestati;

import it.pagopa.pn.ec.notificationtracker.model.NotificationResponseModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
@Service
@Slf4j
public class CallMachinaStatiImpl implements CallMachinaStati{


    private final WebClient ecInternalWebClient;

    public CallMachinaStatiImpl(WebClient ecInternalWebClient) {
        this.ecInternalWebClient = ecInternalWebClient;
    }


    @Value("${statemachine.url}")
    String statemachineGetClientEndpoint;

    @Override
    public Mono<NotificationResponseModel> getStato(String prossesId, String currStatus, String xPagopaExtchCxId, String nextStatus) {
        return ecInternalWebClient.get()
                .uri(uriBuilder -> uriBuilder.path(statemachineGetClientEndpoint + "{processId}/{currStatus}" )
                        .queryParam("clientId",  xPagopaExtchCxId)
                        .queryParam("nextStatus" ,nextStatus)
                        .build(prossesId,currStatus))
                .retrieve()

                .bodyToMono(NotificationResponseModel.class);
    }
}
