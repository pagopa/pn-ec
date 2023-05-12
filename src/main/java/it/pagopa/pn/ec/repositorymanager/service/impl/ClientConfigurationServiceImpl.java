package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.model.entity.ClientConfiguration;
import it.pagopa.pn.ec.repositorymanager.model.entity.ClientConfigurationInternal;
import it.pagopa.pn.ec.repositorymanager.service.ClientConfigurationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.DYNAMO_OPTIMISTIC_LOCKING_RETRY;
import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.getKey;

@Service
@Slf4j
public class ClientConfigurationServiceImpl implements ClientConfigurationService {

    private final DynamoDbAsyncTable<ClientConfigurationInternal> clientConfigurationDynamoDbTableInternal;
    private final DynamoDbAsyncTable<ClientConfiguration> clientConfigurationDynamoDbTable;

    public ClientConfigurationServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient,
                                          RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        this.clientConfigurationDynamoDbTable = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.anagraficaClientName(),
                TableSchema.fromBean(ClientConfiguration.class));
        this.clientConfigurationDynamoDbTableInternal = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.anagraficaClientName(),
                TableSchema.fromBean(ClientConfigurationInternal.class));
    }

    @Bean
    @Qualifier("clientConfigurationDynamoDbTable")
    public DynamoDbAsyncTable<ClientConfiguration> getClientConfigurationDynamoDbTable() {
        return this.clientConfigurationDynamoDbTable;
    }


    @Override
    public Flux<ClientConfiguration> getAllClient() {
        log.info("Try to retrieve all clients");
        return Flux.from(clientConfigurationDynamoDbTable.scan().items());
    }

    @Override
    public Mono<ClientConfigurationInternal> getClient(String cxId) {
        log.info("Try to retrieve client with id -> {}", cxId);
        return Mono.fromCompletionStage(clientConfigurationDynamoDbTableInternal.getItem(getKey(cxId)))
                .switchIfEmpty(Mono.error(new RepositoryManagerException.IdClientNotFoundException(cxId)))
                .doOnError(RepositoryManagerException.IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()));
    }

    @Override
    public Mono<ClientConfigurationInternal> insertClient(ClientConfigurationInternal clientConfiguration) {
        log.info("Try to insert client: {}", clientConfiguration.getCxId());
        return Mono.fromCompletionStage(clientConfigurationDynamoDbTableInternal.getItem(getKey(clientConfiguration.getCxId())))
                .flatMap(foundedClientConfiguration -> Mono.error(new RepositoryManagerException.IdClientAlreadyPresent(
                        clientConfiguration.getCxId())))
                .doOnError(RepositoryManagerException.IdClientAlreadyPresent.class, throwable -> log.info(throwable.getMessage()))
                .switchIfEmpty(Mono.just(clientConfiguration))
                .flatMap(unused -> Mono.fromCompletionStage(clientConfigurationDynamoDbTableInternal.putItem(builder -> builder.item(
                        clientConfiguration))))
                .thenReturn(clientConfiguration);
    }

    @Override
    public Mono<ClientConfigurationInternal> updateClient(String cxId, ClientConfigurationInternal clientConfiguration) {
        log.info("Try to update client: {}", clientConfiguration.getCxId());
        return Mono.fromCompletionStage(clientConfigurationDynamoDbTableInternal.getItem(getKey(cxId)))
                .switchIfEmpty(Mono.error(new RepositoryManagerException.IdClientNotFoundException(cxId)))
                .doOnError(RepositoryManagerException.IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                .flatMap(retrievedClientConfiguration -> {
                    clientConfiguration.setCxId(cxId);
                    return Mono.fromCompletionStage(clientConfigurationDynamoDbTableInternal.updateItem(retrievedClientConfiguration))
                            .retryWhen(DYNAMO_OPTIMISTIC_LOCKING_RETRY);
                });
    }

    @Override
    public Mono<ClientConfigurationInternal> deleteClient(String cxId) {
        log.info("Try to delete client with id -> {}", cxId);
        return Mono.fromCompletionStage(clientConfigurationDynamoDbTableInternal.getItem(getKey(cxId)))
                .switchIfEmpty(Mono.error(new RepositoryManagerException.IdClientNotFoundException(cxId)))
                .doOnError(RepositoryManagerException.IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                .flatMap(clientToDelete -> Mono.fromCompletionStage(clientConfigurationDynamoDbTableInternal.deleteItem(getKey(cxId))));
    }
}
