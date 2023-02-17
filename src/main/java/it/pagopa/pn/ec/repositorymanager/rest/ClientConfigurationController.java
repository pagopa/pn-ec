package it.pagopa.pn.ec.repositorymanager.rest;

import it.pagopa.pn.ec.commons.utils.RestUtils;
import it.pagopa.pn.ec.repositorymanager.model.entity.ClientConfiguration;
import it.pagopa.pn.ec.repositorymanager.service.ClientConfigurationService;
import it.pagopa.pn.ec.rest.v1.api.ConfigurationsApi;
import it.pagopa.pn.ec.rest.v1.api.ConfigurazioneClientApi;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.OK;

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
        log.info("Try to retrieve all clients");
        return clientConfigurationService.getAllClient()
                                         .map(retrievedClient -> restUtils.dtoToEntity(retrievedClient, ClientConfigurationDto.class))
                                         .collectList()
                                         .doOnNext(configurationDtoList -> log.info("Retrieved all clients ↓\n{}", configurationDtoList))
                                         .map(configurationDtoList -> ResponseEntity.ok().body(Flux.fromIterable(configurationDtoList)));
    }

    @Override
    public Mono<ResponseEntity<ClientConfigurationDto>> getClient(String xPagopaExtchCxId, ServerWebExchange exchange) {
        log.info("Try to retrieve client with id -> {}", xPagopaExtchCxId);
        return clientConfigurationService.getClient(xPagopaExtchCxId)
                                         .map(retrievedClient -> restUtils.endReadRequest(retrievedClient, ClientConfigurationDto.class));
    }

    @Override
    public Mono<ResponseEntity<ClientConfigurationDto>> insertClient(Mono<ClientConfigurationDto> clientConfigurationDto,
                                                                     ServerWebExchange exchange) {
        return clientConfigurationDto.map(clientDtoToInsert -> restUtils.startCreateRequest(clientDtoToInsert, ClientConfiguration.class))
                                     .flatMap(clientConfigurationService::insertClient)
                                     .map(insertedClient -> restUtils.endCreateOrUpdateRequest(insertedClient,
                                                                                               ClientConfigurationDto.class));
    }

    @Override
    public Mono<ResponseEntity<ClientConfigurationDto>> updateClient(String xPagopaExtchCxId,
                                                                     Mono<ClientConfigurationDto> clientConfigurationPutDto,
                                                                     ServerWebExchange exchange) {
        return clientConfigurationPutDto.map(clientDtoToUpdate -> restUtils.startUpdateRequest(clientDtoToUpdate,
                                                                                               ClientConfiguration.class))
                                        .flatMap(clientToUpdate -> clientConfigurationService.updateClient(xPagopaExtchCxId,
                                                                                                           clientToUpdate))
                                        .map(updatedClient -> restUtils.endCreateOrUpdateRequest(updatedClient,
                                                                                                 ClientConfigurationDto.class));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteClient(String xPagopaExtchCxId, ServerWebExchange exchange) {
        log.info("Try to delete client with id -> {}", xPagopaExtchCxId);
        return clientConfigurationService.deleteClient(xPagopaExtchCxId)
                                         .map(retrievedClient -> restUtils.endDeleteRequest(retrievedClient, ClientConfigurationDto.class))
                                         .thenReturn(new ResponseEntity<>(OK));
    }
}
