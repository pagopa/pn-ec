package it.pagopa.pn.ec.commons.rest.call.machinestate;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.statemachine.StateMachineEndpointProperties;
import it.pagopa.pn.ec.commons.exception.InvalidNextStatusException;
import it.pagopa.pn.ec.commons.exception.StatusNotFoundException;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiDecodeResponseDto;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiValidateStatoResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Component
@Slf4j
public class CallMacchinaStatiImpl implements CallMacchinaStati {

    private final WebClient stateMachineWebClient;
    private final StateMachineEndpointProperties stateMachineEndpointProperties;

    private static final String CLIENT_ID_QUERY_PARAM = "clientId";

    public CallMacchinaStatiImpl(WebClient stateMachineWebClient, StateMachineEndpointProperties stateMachineEndpointProperties) {
        this.stateMachineWebClient = stateMachineWebClient;
        this.stateMachineEndpointProperties = stateMachineEndpointProperties;
    }

    @Override
    public Mono<MacchinaStatiValidateStatoResponseDto> statusValidation(String xPagopaExtchCxId, String processId, String currentStatus,
                                                                        String nextStatus)
            throws InvalidNextStatusException {
        log.info(INVOKING_EXTERNAL_SERVICE, STATE_MACHINE_SERVICE, STATUS_VALIDATION);
        return stateMachineWebClient.get()
                .uri(uriBuilder -> uriBuilder.path(stateMachineEndpointProperties.validate())
                        .queryParam(CLIENT_ID_QUERY_PARAM, xPagopaExtchCxId)
                        .queryParam("nextStatus", nextStatus)
                        .build(processId, currentStatus))
                .retrieve()
                .onStatus(BAD_REQUEST::equals, clientResponse -> Mono.error(new StatusValidationBadRequestException()))
                .bodyToMono(MacchinaStatiValidateStatoResponseDto.class)
                .handle((macchinaStatiValidateStatoResponseDto, sink) -> {
                    if (!macchinaStatiValidateStatoResponseDto.isAllowed()) {
                        sink.error(new InvalidNextStatusException(currentStatus,
                                nextStatus,
                                xPagopaExtchCxId,
                                processId));
                    } else {
                        sink.next(macchinaStatiValidateStatoResponseDto);
                    }
                });
    }

    @Override
    public Mono<MacchinaStatiDecodeResponseDto> statusDecode(String xPagopaExtchCxId, String processId, String statusToDecode) {
        log.info(INVOKING_EXTERNAL_SERVICE, STATE_MACHINE_SERVICE, STATUS_DECODE);
        return stateMachineWebClient.get()
                .uri(uriBuilder -> uriBuilder.path(stateMachineEndpointProperties.decode())
                        .queryParam(CLIENT_ID_QUERY_PARAM, xPagopaExtchCxId)
                        .build(processId, statusToDecode))
                .retrieve()
                .onStatus(NOT_FOUND::equals, clientResponse -> Mono.error(new StatusNotFoundException(statusToDecode)))
                .bodyToMono(MacchinaStatiDecodeResponseDto.class)
                .handle((macchinaStatiDecodeResponseDto, sink) -> {
                    if (macchinaStatiDecodeResponseDto.getExternalStatus() == null) {
                        sink.error(new StatusNotFoundException(statusToDecode));
                    } else {
                        sink.next(macchinaStatiDecodeResponseDto);
                    }
                });
    }
}
