package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.model.entity.ClientConfiguration;
import it.pagopa.pn.ec.repositorymanager.model.entity.ClientConfigurationInternal;
import it.pagopa.pn.ec.repositorymanager.service.ClientConfigurationService;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.DYNAMO_OPTIMISTIC_LOCKING_RETRY;
import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.getKey;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@Service
@CustomLog
public class ClientConfigurationServiceImpl implements ClientConfigurationService {

    private final DynamoDbAsyncTableDecorator<ClientConfigurationInternal> clientConfigurationDynamoDbTableInternal;
    private final DynamoDbAsyncTableDecorator<ClientConfiguration> clientConfigurationDynamoDbTable;

    public ClientConfigurationServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient,
                                          RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        this.clientConfigurationDynamoDbTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.anagraficaClientName(),
                TableSchema.fromBean(ClientConfiguration.class)));
        this.clientConfigurationDynamoDbTableInternal = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.anagraficaClientName(),
                TableSchema.fromBean(ClientConfigurationInternal.class)));
    }

    @Bean
    @Qualifier("clientConfigurationDynamoDbTable")
    public DynamoDbAsyncTableDecorator<ClientConfiguration> getClientConfigurationDynamoDbTable() {
        return this.clientConfigurationDynamoDbTable;
    }


    @Override
    public Flux<ClientConfiguration> getAllClient() {
        log.debug(INVOKING_OPERATION_LABEL, GET_ALL_CLIENT);
        return Flux.from(clientConfigurationDynamoDbTable.scan().items())
                .doOnComplete(() -> log.info(SUCCESSFUL_OPERATION_NO_RESULT_LABEL, GET_ALL_CLIENT));
    }

    @Override
    public Mono<ClientConfigurationInternal> getClient(String cxId) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, GET_CLIENT, cxId);
        return getClientConfigurationFromDynamoDb(cxId)
                .switchIfEmpty(Mono.error(new RepositoryManagerException.IdClientNotFoundException(cxId)))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, GET_CLIENT, result.getCxId()));
    }

    @Override
    public Mono<ClientConfigurationInternal> insertClient(ClientConfigurationInternal clientConfiguration) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, INSERT_CLIENT, clientConfiguration.getCxId());
        return getClientConfigurationFromDynamoDb(clientConfiguration.getCxId())
                .flatMap(foundedClientConfiguration -> Mono.error(new RepositoryManagerException.IdClientAlreadyPresent(clientConfiguration.getCxId())))
                .switchIfEmpty(Mono.just(clientConfiguration))
                .flatMap(unused -> putClientConfigurationInDynamoDb(clientConfiguration))
                .thenReturn(clientConfiguration)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, INSERT_CLIENT, result.getCxId()));
    }

    @Override
    public Mono<ClientConfigurationInternal> updateClient(String cxId, ClientConfigurationInternal clientConfiguration) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, UPDATE_CLIENT, cxId);
        return getClientConfigurationFromDynamoDb(cxId)
                .switchIfEmpty(Mono.error(new RepositoryManagerException.IdClientNotFoundException(cxId)))
                .flatMap(retrievedClientConfiguration -> {
                    clientConfiguration.setCxId(cxId);
                    return updateClientConfigurationInDynamoDb(retrievedClientConfiguration)
                            .retryWhen(DYNAMO_OPTIMISTIC_LOCKING_RETRY);
                })
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, UPDATE_CLIENT, result.getCxId()));
    }

    @Override
    public Mono<ClientConfigurationInternal> deleteClient(String cxId) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, DELETE_CLIENT, cxId);
        return getClientConfigurationFromDynamoDb(cxId)
                .switchIfEmpty(Mono.error(new RepositoryManagerException.IdClientNotFoundException(cxId)))
                .flatMap(clientToDelete -> deleteClientConfigurationFromDynamoDb(cxId))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, DELETE_CLIENT, result.getCxId()));
    }

    private Mono<ClientConfigurationInternal> getClientConfigurationFromDynamoDb(String cxId) {
        return Mono.fromCompletionStage(() -> clientConfigurationDynamoDbTableInternal.getItem(getKey(cxId)));
    }

    private Mono<Void> putClientConfigurationInDynamoDb(ClientConfigurationInternal clientConfiguration) {
        return Mono.fromCompletionStage(clientConfigurationDynamoDbTableInternal.putItem(builder -> builder.item(clientConfiguration)));
    }

    private Mono<ClientConfigurationInternal> updateClientConfigurationInDynamoDb(ClientConfigurationInternal clientConfiguration) {
        return Mono.fromCompletionStage(clientConfigurationDynamoDbTableInternal.updateItem(clientConfiguration));
    }

    private Mono<ClientConfigurationInternal> deleteClientConfigurationFromDynamoDb(String cxId) {
        return Mono.fromCompletionStage(clientConfigurationDynamoDbTableInternal.deleteItem(getKey(cxId)));
    }

}
