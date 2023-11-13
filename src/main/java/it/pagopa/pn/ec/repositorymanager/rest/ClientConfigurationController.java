package it.pagopa.pn.ec.repositorymanager.rest;

import it.pagopa.pn.ec.commons.utils.RestUtils;
import it.pagopa.pn.ec.repositorymanager.model.entity.ClientConfigurationInternal;
import it.pagopa.pn.ec.repositorymanager.service.ClientConfigurationService;
import it.pagopa.pn.ec.rest.v1.api.ConfigurationsApi;
import it.pagopa.pn.ec.rest.v1.api.ConfigurazioneClientApi;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationInternalDto;
import lombok.CustomLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@CustomLog
public class ClientConfigurationController implements ConfigurationsApi, ConfigurazioneClientApi {

    private final ClientConfigurationService clientConfigurationService;

    private final RestUtils restUtils;

    public ClientConfigurationController(ClientConfigurationService clientConfigurationService, RestUtils restUtils) {
        this.clientConfigurationService = clientConfigurationService;
        this.restUtils = restUtils;
    }

    @Override
    public Mono<ResponseEntity<Flux<ClientConfigurationDto>>> getConfigurations(ServerWebExchange exchange) {
        log.logStartingProcess(GET_CONFIGURATIONS);
        return clientConfigurationService.getAllClient()
                .map(retrievedClient -> restUtils.entityToDto(retrievedClient, ClientConfigurationDto.class))
                .collectList()
                .doOnSuccess(configurationDtoList -> log.debug("Retrieved all clients ↓\n{}", configurationDtoList))
                .map(configurationDtoList -> ResponseEntity.ok().body(Flux.fromIterable(configurationDtoList)))
                .doOnSuccess(result -> log.logEndingProcess(GET_CONFIGURATIONS))
                .doOnError(throwable -> log.logEndingProcess(GET_CONFIGURATIONS, false, throwable.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<ClientConfigurationInternalDto>> getClient(String xPagopaExtchCxId, ServerWebExchange exchange) {
        log.logStartingProcess(GET_CLIENT);
        return clientConfigurationService.getClient(xPagopaExtchCxId)
                .map(retrievedClient -> restUtils.endReadRequest(retrievedClient, ClientConfigurationInternalDto.class))
                .doOnSuccess(result -> log.logEndingProcess(GET_CLIENT))
                .doOnError(throwable -> log.logEndingProcess(GET_CLIENT, false, throwable.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<ClientConfigurationInternalDto>> insertClient(Mono<ClientConfigurationDto> clientConfigurationDto,
                                                                             ServerWebExchange exchange) {
        log.logStartingProcess(INSERT_CLIENT);
        return clientConfigurationDto
                .map(clientDtoToInsert -> restUtils.startCreateRequest(clientDtoToInsert, ClientConfigurationInternal.class))
                .flatMap(clientConfigurationService::insertClient)
                .map(insertedClient -> restUtils.endCreateOrUpdateRequest(insertedClient,
                        ClientConfigurationInternalDto.class))
                .doOnSuccess(result -> log.logEndingProcess(INSERT_CLIENT))
                .doOnError(throwable -> log.logEndingProcess(INSERT_CLIENT, false, throwable.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<ClientConfigurationInternalDto>> updateClient(String xPagopaExtchCxId,
                                                                             Mono<ClientConfigurationDto> clientConfigurationPutDto,
                                                                             ServerWebExchange exchange) {
        log.logStartingProcess(UPDATE_CLIENT);
        return clientConfigurationPutDto
                .doOnNext(clientConfigurationDto -> log.info("Try to update client: {}", clientConfigurationDto.getxPagopaExtchCxId()))
                .map(clientDtoToUpdate -> restUtils.startUpdateRequest(clientDtoToUpdate,
                        ClientConfigurationInternal.class))
                .flatMap(clientToUpdate -> clientConfigurationService.updateClient(xPagopaExtchCxId,
                        clientToUpdate))
                .map(updatedClient -> restUtils.endCreateOrUpdateRequest(updatedClient,
                        ClientConfigurationInternalDto.class))
                .doOnSuccess(result -> log.logEndingProcess(UPDATE_CLIENT))
                .doOnError(throwable -> log.logEndingProcess(UPDATE_CLIENT, false, throwable.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteClient(String xPagopaExtchCxId, ServerWebExchange exchange) {
        log.logStartingProcess(DELETE_CLIENT);
        return clientConfigurationService.deleteClient(xPagopaExtchCxId)
                .map(retrievedClient -> restUtils.endDeleteRequest(retrievedClient, ClientConfigurationDto.class))
                .doOnSuccess(result -> log.logEndingProcess(DELETE_CLIENT))
                .doOnError(throwable -> log.logEndingProcess(DELETE_CLIENT, false, throwable.getMessage()))
                .thenReturn(new ResponseEntity<>(NO_CONTENT));
    }
}
