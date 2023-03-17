package it.pagopa.pn.ec.commons.rest.call.machinestate;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.statemachine.StateMachineEndpointProperties;
import it.pagopa.pn.ec.commons.exception.InvalidNextStatusException;
import it.pagopa.pn.ec.commons.exception.StatusNotFoundException;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiDecodeResponseDto;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pn.ec.commons.model.pojo.request.RequestStatusChange;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Component
public class CallMacchinaStatiImpl implements CallMacchinaStati {

    private final WebClient stateMachineWebClient;
    private final StateMachineEndpointProperties stateMachineEndpointProperties;

    private static final String CLIENT_ID_QUERY_PARAM = "clientId";

    public CallMacchinaStatiImpl(WebClient stateMachineWebClient, StateMachineEndpointProperties stateMachineEndpointProperties) {
        this.stateMachineWebClient = stateMachineWebClient;
        this.stateMachineEndpointProperties = stateMachineEndpointProperties;
    }

    @Override
    public Mono<MacchinaStatiValidateStatoResponseDto> statusValidation(RequestStatusChange requestStatusChange)
            throws InvalidNextStatusException {
        return stateMachineWebClient.get()
                                    .uri(uriBuilder -> uriBuilder.path(stateMachineEndpointProperties.validate())
                                                                 .queryParam(CLIENT_ID_QUERY_PARAM,
                                                                             requestStatusChange.getXPagopaExtchCxId())
                                                                 .queryParam("nextStatus", requestStatusChange.getNextStatus())
                                                                 .build(requestStatusChange.getProcessId(),
                                                                        requestStatusChange.getCurrentStatus()))
                                    .retrieve()
                                    .bodyToMono(MacchinaStatiValidateStatoResponseDto.class)
                                    .flatMap(macchinaStatiValidateStatoResponseDto -> {
                                        if (!macchinaStatiValidateStatoResponseDto.isAllowed()) {
                                            return Mono.error(new InvalidNextStatusException(requestStatusChange));
                                        } else {
                                            return Mono.just(macchinaStatiValidateStatoResponseDto);
                                        }
                                    });
    }

    @Override
    public Mono<MacchinaStatiDecodeResponseDto> statusDecode(RequestStatusChange requestStatusChange) {
        var currentStatus = requestStatusChange.getCurrentStatus();

        return stateMachineWebClient.get()
                                    .uri(uriBuilder -> uriBuilder.path(stateMachineEndpointProperties.decode())
                                                                 .queryParam(CLIENT_ID_QUERY_PARAM,
                                                                             requestStatusChange.getXPagopaExtchCxId())
                                                                 .build(requestStatusChange.getProcessId(), currentStatus))
                                    .retrieve()
                                    .onStatus(NOT_FOUND::equals, clientResponse -> Mono.error(new StatusNotFoundException(currentStatus)))
                                    .bodyToMono(MacchinaStatiDecodeResponseDto.class);

    }
}
