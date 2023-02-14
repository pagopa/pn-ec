package it.pagopa.pn.ec.commons.rest.call.gestorerepository;

import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import reactor.core.publisher.Mono;

@SuppressWarnings("unused")
public interface GestoreRepositoryCall {

//  <-- CLIENT CONFIGURATION -->
    Mono<ClientConfigurationDto> getClientConfiguration(String xPagopaExtchCxId) throws RestCallException.ResourceNotFoundException;
    Mono<ClientConfigurationDto> insertClientConfiguration(ClientConfigurationDto clientConfigurationDto);
    Mono<ClientConfigurationDto> updateClientConfiguration(String xPagopaExtchCxId, ClientConfigurationDto clientConfigurationDto);
    Mono<Void> deleteClientConfiguration(String xPagopaExtchCxId);

//  <-- REQUEST -->
    Mono<RequestDto> getRichiesta(String requestIdx) throws RestCallException.ResourceNotFoundException;
    Mono<RequestDto> insertRichiesta(RequestDto requestDto) throws RestCallException.ResourceNotFoundException;
    Mono<RequestDto> updateRichiesta(String requestIdx, EventsDto eventsDto) throws RestCallException.ResourceNotFoundException;
    Mono<Void> deleteRichiesta(String requestIdx);
}
