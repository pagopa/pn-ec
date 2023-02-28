package it.pagopa.pn.ec.commons.rest.call.machinestate;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.statemachine.StateMachineEndpointProperties;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiValidateStatoResponseDto;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class CallMachinaStatiImpl implements CallMachinaStati {


    private final WebClient stateMachineWebClient;
    private final StateMachineEndpointProperties stateMachineEndpointProperties;

    public CallMachinaStatiImpl(WebClient stateMachineWebClient, StateMachineEndpointProperties stateMachineEndpointProperties) {
        this.stateMachineWebClient = stateMachineWebClient;
        this.stateMachineEndpointProperties = stateMachineEndpointProperties;
    }

    @Override
    public Mono<MacchinaStatiValidateStatoResponseDto> statusValidation(String processId, String currStatus, String xPagopaExtchCxId,
                                                                        String nextStatus) {
        return stateMachineWebClient.get()
                                    .uri(uriBuilder -> uriBuilder.path(stateMachineEndpointProperties.validate())
                                                                 .queryParam("clientId", xPagopaExtchCxId)
                                                                 .queryParam("nextStatus", nextStatus)
                                                                 .build(processId, currStatus))
                                    .retrieve()
                                    .bodyToMono(MacchinaStatiValidateStatoResponseDto.class);
    }
}
