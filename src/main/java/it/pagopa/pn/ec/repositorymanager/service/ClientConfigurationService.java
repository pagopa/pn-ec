package it.pagopa.pn.ec.repositorymanager.service;

import it.pagopa.pn.ec.repositorymanager.model.entity.ClientConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ClientConfigurationService {

    Flux<ClientConfiguration> getAllClient();
    Mono<ClientConfiguration> getClient(String cxId);
    Mono<ClientConfiguration> insertClient(ClientConfiguration clientConfiguration);
    Mono<ClientConfiguration> updateClient(String cxId, ClientConfiguration clientConfiguration);
    Mono<ClientConfiguration> deleteClient(String cxId);
}
