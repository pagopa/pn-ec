package it.pagopa.pn.ec.notificationtracker.service.callmachinestati;

import it.pagopa.pn.ec.notificationtracker.model.NotificationResponseModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class CallMachinaStatiImpl implements CallMachinaStati {


    private final WebClient ecWebClient;

    public CallMachinaStatiImpl(WebClient ecWebClient) {
        this.ecWebClient = ecWebClient;
    }


    @Value("${statemachine.url}")
    String statemachineGetClientEndpoint;

    @Override
    public Mono<NotificationResponseModel> getStato(String processId, String currStatus, String xPagopaExtchCxId, String nextStatus) {
        return ecWebClient.get()
                          .uri(uriBuilder -> uriBuilder.path(statemachineGetClientEndpoint + "{processId}/{currStatus}")
                                                       .queryParam("clientId", xPagopaExtchCxId)
                                                       .queryParam("nextStatus", nextStatus)
                                                       .build(processId, currStatus))
                          .retrieve()

                          .bodyToMono(NotificationResponseModel.class);
    }
}
