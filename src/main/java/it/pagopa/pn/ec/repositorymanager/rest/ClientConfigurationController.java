package it.pagopa.pn.ec.repositorymanager.rest;

import it.pagopa.pn.ec.commons.utils.RestUtils;
import it.pagopa.pn.ec.repositorymanager.model.entity.ClientConfigurationInternal;
import it.pagopa.pn.ec.repositorymanager.service.ClientConfigurationService;
import it.pagopa.pn.ec.rest.v1.api.ConfigurationsApi;
import it.pagopa.pn.ec.rest.v1.api.ConfigurazioneClientApi;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationInternalDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@Slf4j
public class ClientConfigurationController implements ConfigurationsApi, ConfigurazioneClientApi {

    private final ClientConfigurationService clientConfigurationService;

    private final RestUtils restUtils;

    public ClientConfigurationController(ClientConfigurationService clientConfigurationService, RestUtils restUtils) {
        this.clientConfigurationService = clientConfigurationService;
        this.restUtils = restUtils;
    }

    @Override
    public Mono<ResponseEntity<Flux<ClientConfigurationDto>>> getConfigurations(ServerWebExchange exchange) {

        return clientConfigurationService.getAllClient()
                .map(retrievedClient -> restUtils.entityToDto(retrievedClient, ClientConfigurationDto.class))
                .collectList()
                .doOnNext(configurationDtoList -> log.info("Retrieved all clients ↓\n{}", configurationDtoList))
                .map(configurationDtoList -> ResponseEntity.ok().body(Flux.fromIterable(configurationDtoList)));
    }

    @Override
    public Mono<ResponseEntity<ClientConfigurationInternalDto>> getClient(String xPagopaExtchCxId, ServerWebExchange exchange) {

        return clientConfigurationService.getClient(xPagopaExtchCxId)
                .map(retrievedClient -> restUtils.endReadRequest(retrievedClient, ClientConfigurationInternalDto.class));
    }

    @Override
    public Mono<ResponseEntity<ClientConfigurationInternalDto>> insertClient(Mono<ClientConfigurationDto> clientConfigurationDto,
                                                                             ServerWebExchange exchange) {

        return clientConfigurationDto
                .map(clientDtoToInsert -> {

                    return restUtils.startCreateRequest(clientDtoToInsert, ClientConfigurationInternal.class);
                })
                .flatMap(clientConfigurationService::insertClient)
                .map(insertedClient -> restUtils.endCreateOrUpdateRequest(insertedClient,
                        ClientConfigurationInternalDto.class));
    }

    @Override
    public Mono<ResponseEntity<ClientConfigurationInternalDto>> updateClient(String xPagopaExtchCxId,
                                                                             Mono<ClientConfigurationDto> clientConfigurationPutDto,
                                                                             ServerWebExchange exchange) {
        return clientConfigurationPutDto
                .doOnNext(clientConfigurationDto -> log.info("Try to update client: {}", clientConfigurationDto.getxPagopaExtchCxId()))
                .map(clientDtoToUpdate -> restUtils.startUpdateRequest(clientDtoToUpdate,
                        ClientConfigurationInternal.class))
                .flatMap(clientToUpdate -> clientConfigurationService.updateClient(xPagopaExtchCxId,
                        clientToUpdate))
                .map(updatedClient -> restUtils.endCreateOrUpdateRequest(updatedClient,
                        ClientConfigurationInternalDto.class));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteClient(String xPagopaExtchCxId, ServerWebExchange exchange) {

        return clientConfigurationService.deleteClient(xPagopaExtchCxId)
                .map(retrievedClient -> restUtils.endDeleteRequest(retrievedClient, ClientConfigurationDto.class))
                .thenReturn(new ResponseEntity<>(NO_CONTENT));
    }
}
