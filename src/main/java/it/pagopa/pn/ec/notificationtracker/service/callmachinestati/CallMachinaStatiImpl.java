package it.pagopa.pn.ec.notificationtracker.service.callmachinestati;

import it.pagopa.pn.ec.notificationtracker.model.NotificationResponseModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class CallMachinaStatiImpl implements CallMachinaStati {


    private final WebClient stateMachineWebClient;

    public CallMachinaStatiImpl(WebClient stateMachineWebClient) {
        this.stateMachineWebClient = stateMachineWebClient;
    }

    @Override
    public Mono<NotificationResponseModel> getStato(String processId, String currStatus, String xPagopaExtchCxId, String nextStatus) {
        return stateMachineWebClient.get()
                                    // TODO: DEFINE ENDPOINT IN PROPERTIES
                                    .uri(uriBuilder -> uriBuilder.path("/statemachinemanager/validate/{processId}/{currStatus}")
                                                                 .queryParam("clientId", xPagopaExtchCxId)
                                                                 .queryParam("nextStatus", nextStatus)
                                                                 .build(processId, currStatus)).retrieve()

                                    .bodyToMono(NotificationResponseModel.class);
    }
}
