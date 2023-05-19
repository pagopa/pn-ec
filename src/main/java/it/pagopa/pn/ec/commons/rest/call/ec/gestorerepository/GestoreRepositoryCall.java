package it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository;

import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.rest.v1.dto.*;
import reactor.core.publisher.Mono;

@SuppressWarnings("unused")
public interface GestoreRepositoryCall {

//  <-- CLIENT CONFIGURATION -->
    Mono<ClientConfigurationInternalDto> getClientConfiguration(String xPagopaExtchCxId) throws RestCallException.ResourceNotFoundException;
    Mono<ClientConfigurationDto> insertClientConfiguration(ClientConfigurationDto clientConfigurationDto);
    Mono<ClientConfigurationDto> updateClientConfiguration(String xPagopaExtchCxId, ClientConfigurationDto clientConfigurationDto);
    Mono<Void> deleteClientConfiguration(String xPagopaExtchCxId);

//  <-- REQUEST -->
    Mono<RequestDto> getRichiesta(String clientId, String requestIdx) throws RestCallException.ResourceNotFoundException;
    Mono<RequestDto> insertRichiesta(RequestDto requestDto) throws RestCallException.ResourceNotFoundException;
    Mono<RequestDto> patchRichiestaEvent(String clientId, String requestIdx, EventsDto eventsDto) throws RestCallException.ResourceNotFoundException;
    Mono<RequestDto> patchRichiestaRetry(String clientId, String requestIdx, RetryDto retryDto) throws RestCallException.ResourceNotFoundException;
    Mono<RequestDto> patchRichiesta(String clientId, String requestIdx, PatchDto patchDto) throws RestCallException.ResourceNotFoundException;
    Mono<Void> deleteRichiesta(String clientId, String requestIdx);
    Mono<RequestDto> getRequestByMessageId(String messageId);
    Mono<RequestDto> setMessageIdInRequestMetadata(String clientId, String requestIdx);
}
