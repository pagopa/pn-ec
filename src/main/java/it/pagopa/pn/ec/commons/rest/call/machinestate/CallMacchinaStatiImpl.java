package it.pagopa.pn.ec.commons.rest.call.machinestate;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.statemachine.StateMachineEndpointProperties;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic404ErrorException;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiDecodeResponseDto;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiValidateStatoResponseDto;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class CallMacchinaStatiImpl implements CallMacchinaStati {

	private final WebClient stateMachineWebClient;
	private final StateMachineEndpointProperties stateMachineEndpointProperties;

	public CallMacchinaStatiImpl(WebClient stateMachineWebClient,
			StateMachineEndpointProperties stateMachineEndpointProperties) {
		this.stateMachineWebClient = stateMachineWebClient;
		this.stateMachineEndpointProperties = stateMachineEndpointProperties;
	}

	@Override
	public Mono<MacchinaStatiValidateStatoResponseDto> statusValidation(String processId, String currStatus,
			String xPagopaExtchCxId, String nextStatus) {
		return stateMachineWebClient.get()
				.uri(uriBuilder -> uriBuilder.path(stateMachineEndpointProperties.validate())
						.queryParam("clientId", xPagopaExtchCxId).queryParam("nextStatus", nextStatus)
						.build(processId, currStatus))
				.retrieve().bodyToMono(MacchinaStatiValidateStatoResponseDto.class);
	}

	@Override
	public Mono<MacchinaStatiDecodeResponseDto> statusDecode(String processId, String currStatus, String clientId) {
		return stateMachineWebClient.get()
				.uri(uriBuilder -> uriBuilder.path(stateMachineEndpointProperties.decode())
						.queryParam("clientId", clientId).build(processId, currStatus))
				.retrieve()
				.onStatus(NOT_FOUND::equals,
						clientResponse -> Mono.error(new Generic404ErrorException("Not found", "Status not found")))
				.bodyToMono(MacchinaStatiDecodeResponseDto.class);

	}

}
