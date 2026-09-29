package it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository;

import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.*;

@Component
@CustomLog
public class GestoreRepositoryCallImpl implements GestoreRepositoryCall {

    private final WebClient ecWebClient;
    private final PnEcConfig.Commons.Endpoint.GestoreRepository gestoreRepositoryEndpointProperties;
    private static final String CLIENT_HEADER_NAME = "x-pagopa-extch-cx-id";

    public GestoreRepositoryCallImpl(@Qualifier("ecWebClient")WebClient ecWebClient, PnEcConfig pnEcConfig) {
        this.ecWebClient = ecWebClient;
        this.gestoreRepositoryEndpointProperties = pnEcConfig.getCommons().getEndpoint().getGestoreRepository();
    }

    //  <-- CLIENT CONFIGURATION -->
    @Override
    public Mono<ClientConfigurationInternalDto> getClientConfiguration(String xPagopaExtchCxId) {
        return ecWebClient.get()
                          .uri(UriComponentsBuilder.fromPath(gestoreRepositoryEndpointProperties.getGetClientConfiguration())
                                                       .build(xPagopaExtchCxId).toString())
                          .retrieve()
                          .onStatus(NOT_FOUND::equals, clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                          .bodyToMono(ClientConfigurationInternalDto.class);
    }

    //  <-- REQUEST -->
    @Override
    public Mono<ClientConfigurationDto> insertClientConfiguration(ClientConfigurationDto clientConfigurationDto) {
        return null;
    }

    @Override
    public Mono<ClientConfigurationDto> updateClientConfiguration(String xPagopaExtchCxId, ClientConfigurationDto clientConfigurationDto) {
        return null;
    }

    @Override
    public Mono<Void> deleteClientConfiguration(String xPagopaExtchCxId) {
        return null;
    }

    @Override
    public Mono<RequestDto> getRichiesta(String clientId, String requestIdx) throws RestCallException.ResourceNotFoundException {
        return ecWebClient.get()
                          .uri(UriComponentsBuilder.fromPath(gestoreRepositoryEndpointProperties.getGetRequest()).build(requestIdx).toString())
                          .header(CLIENT_HEADER_NAME, clientId)
                          .retrieve()
                          .onStatus(NOT_FOUND::equals, clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                          .bodyToMono(RequestDto.class);
    }

    @Override
    public Mono<RequestDto> insertRichiesta(RequestDto requestDto) throws RestCallException.ResourceAlreadyExistsException {
        return ecWebClient.post()
                          .uri(gestoreRepositoryEndpointProperties.getPostRequest())
                          .bodyValue(requestDto)
                          .retrieve()
                          .onStatus(BAD_REQUEST::equals,
                                    clientResponse -> Mono.error(new RepositoryManagerException.RequestMalformedException()))
                          .onStatus(CONFLICT::equals,
                                    clientResponse -> clientResponse.bodyToMono(Problem.class)
                                                                    .flatMap(problem -> Mono.error(new RestCallException.ResourceAlreadyExistsException(
                                                                            problem.getDetail()))))
                          .onStatus(NO_CONTENT::equals, clientResponse -> Mono.error(new RestCallException.ResourceAlreadySameHashException()))
                          .bodyToMono(RequestDto.class);
    }

    @Override
    public Mono<RequestDto> patchRichiestaEvent(String clientId, String requestIdx, EventsDto eventsDto)
            throws RestCallException.ResourceNotFoundException {
        return patchRichiesta(clientId, requestIdx, new PatchDto().event(eventsDto));
    }

    @Override
    public Mono<RequestDto> patchRichiestaRetry(String clientId, String requestIdx, RetryDto retryDto)
            throws RestCallException.ResourceNotFoundException {
        return patchRichiesta(clientId, requestIdx, new PatchDto().retry(retryDto));
    }

    @Override
    public Mono<RequestDto> patchRichiesta(String clientId, String requestIdx, PatchDto patchDto)
            throws RestCallException.ResourceNotFoundException {
        return ecWebClient.patch()
                          .uri(UriComponentsBuilder.fromPath(gestoreRepositoryEndpointProperties.getPatchRequest()).build(requestIdx).toString())
                          .header(CLIENT_HEADER_NAME, clientId)
                          .bodyValue(patchDto)
                          .retrieve()
                          .onStatus(BAD_REQUEST::equals,
                                    clientResponse -> Mono.error(new RepositoryManagerException.RequestMalformedException()))
                          .onStatus(NOT_FOUND::equals, clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                          .bodyToMono(RequestDto.class);
    }

    @Override
    public Mono<Void> deleteRichiesta(String clientId, String requestIdx) {
        return null;
    }

    @Override
    public Mono<RequestDto> getRequestByMessageId(String messageId)
            throws RestCallException.ResourceNotFoundException, BadMessageIdProvidedException {
        return ecWebClient.get()
                          .uri(UriComponentsBuilder.fromPath(gestoreRepositoryEndpointProperties.getGetRequestByMessageId()).build(messageId).toString())
                          .retrieve()
                          .onStatus(NOT_FOUND::equals, clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                          .onStatus(BAD_REQUEST::equals, clientResponse -> Mono.error(new BadMessageIdProvidedException()))
                          .bodyToMono(RequestDto.class);
    }

    private static class BadMessageIdProvidedException extends RestCallException {

        public BadMessageIdProvidedException() {
            super("Bad messageId provided");
        }
    }

    @Override
    public Mono<RequestDto> setMessageIdInRequestMetadata(String clientId, String requestIdx)
            throws RestCallException.ResourceNotFoundException, ISEForMessageIdCreationException {
        return ecWebClient.post()
                          .uri(UriComponentsBuilder.fromPath(gestoreRepositoryEndpointProperties.getSetMessageIdInRequestMetadata())
                                                       .build(requestIdx).toString())
                          .header(CLIENT_HEADER_NAME, clientId)
                          .retrieve()
                          .onStatus(NOT_FOUND::equals, clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                          .onStatus(INTERNAL_SERVER_ERROR::equals, clientResponse -> Mono.error(new ISEForMessageIdCreationException()))
                          .bodyToMono(RequestDto.class);
    }

    @Override
    public Mono<RequestDto> setRequestMetadataMessageId(String clientId, String requestIdx, MessageIdRequestMetadataDto messageIdRequestMetadataDto) {
        return ecWebClient.patch()
                .uri(UriComponentsBuilder
                        .fromPath(gestoreRepositoryEndpointProperties.getSetRequestMetadataMessageId())
                        .build(requestIdx)
                        .toString())
                .header(CLIENT_HEADER_NAME, clientId)
                .bodyValue(messageIdRequestMetadataDto)
                .retrieve()
                .onStatus(NOT_FOUND::equals,
                        clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                .onStatus(BAD_REQUEST::equals,
                        clientResponse -> Mono.error(new RepositoryManagerException.RequestMalformedException()))
                .bodyToMono(RequestDto.class);
    }

    @Override
    public Mono<RequestDto> getRequestMetadataByMessageId(String messageId) {
        return ecWebClient.get()
                .uri(UriComponentsBuilder
                        .fromPath(gestoreRepositoryEndpointProperties.getGetRequestMetadataByMessageId())
                        .build(messageId)
                        .toString())
                .retrieve()
                .onStatus(NOT_FOUND::equals,
                        clientResponse -> Mono.error(new RestCallException.ResourceNotFoundException()))
                .onStatus(BAD_REQUEST::equals,
                        clientResponse -> Mono.error(new BadMessageIdProvidedException()))
                .bodyToMono(RequestDto.class);    }

    @Override
    public Flux<DiscardedEventDto> insertDiscardedEvents(Flux<DiscardedEventDto> discardedEventsDto) {
        return ecWebClient.post()
                .uri(gestoreRepositoryEndpointProperties.getPostDiscardedEvents())
                .body(discardedEventsDto, DiscardedEventDto.class)
                .retrieve()
                .onStatus(BAD_REQUEST::equals,
                        clientResponse -> Mono.error(new RepositoryManagerException.RequestMalformedException()))
                .bodyToFlux(DiscardedEventDto.class);

    }

    private static class ISEForMessageIdCreationException extends RestCallException {

        public ISEForMessageIdCreationException() {
            super("Internal server error for messageId creation");
        }
    }

}
