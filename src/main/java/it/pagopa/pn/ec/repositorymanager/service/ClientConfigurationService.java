package it.pagopa.pn.ec.repositorymanager.service;

import it.pagopa.pn.ec.repositorymanager.model.entity.ClientConfiguration;
import it.pagopa.pn.ec.repositorymanager.model.entity.ClientConfigurationInternal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ClientConfigurationService {

    Flux<ClientConfiguration> getAllClient();
    Mono<ClientConfigurationInternal> getClient(String cxId);
    Mono<ClientConfigurationInternal> insertClient(ClientConfigurationInternal clientConfiguration);
    Mono<ClientConfigurationInternal> updateClient(String cxId, ClientConfigurationInternal clientConfiguration);
    Mono<ClientConfigurationInternal> deleteClient(String cxId);
}
