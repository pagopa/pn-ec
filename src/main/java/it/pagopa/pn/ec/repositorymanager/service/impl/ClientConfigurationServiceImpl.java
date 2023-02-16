package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.model.entity.ClientConfiguration;
import it.pagopa.pn.ec.repositorymanager.service.ClientConfigurationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.getKey;

@Service
@Slf4j
public class ClientConfigurationServiceImpl implements ClientConfigurationService {

    private final DynamoDbAsyncTable<ClientConfiguration> clientConfigurationDynamoDbTable;

    public ClientConfigurationServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient,
                                          RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        this.clientConfigurationDynamoDbTable = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.anagraficaClientName(),
                                                                             TableSchema.fromBean(ClientConfiguration.class));
    }

    @Override
    public Mono<ClientConfiguration> getClient(String cxId) {
        return Mono.fromCompletionStage(clientConfigurationDynamoDbTable.getItem(getKey(cxId)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.IdClientNotFoundException(cxId)))
                   .doOnError(RepositoryManagerException.IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()));
    }

    @Override
    public Mono<ClientConfiguration> insertClient(ClientConfiguration clientConfiguration) {
        return Mono.fromCompletionStage(clientConfigurationDynamoDbTable.getItem(getKey(clientConfiguration.getCxId())))
                   .flatMap(foundedClientConfiguration -> Mono.error(new RepositoryManagerException.IdClientAlreadyPresent(
                           clientConfiguration.getCxId())))
                   .doOnError(RepositoryManagerException.IdClientAlreadyPresent.class, throwable -> log.info(throwable.getMessage()))
                   .switchIfEmpty(Mono.just(clientConfiguration))
                   .flatMap(unused -> Mono.fromCompletionStage(clientConfigurationDynamoDbTable.putItem(builder -> builder.item(
                           clientConfiguration))))
                   .thenReturn(clientConfiguration);
    }

    @Override
    public Mono<ClientConfiguration> updateClient(String cxId, ClientConfiguration clientConfiguration) {
        return Mono.fromCompletionStage(clientConfigurationDynamoDbTable.getItem(getKey(cxId)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.IdClientNotFoundException(cxId)))
                   .doOnError(RepositoryManagerException.IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                   .flatMap(retrievedClientConfiguration -> {
                       retrievedClientConfiguration.setCxId(cxId);
                       return Mono.fromCompletionStage(clientConfigurationDynamoDbTable.updateItem(clientConfiguration));
                   });
    }

    @Override
    public Mono<ClientConfiguration> deleteClient(String cxId) {
        return Mono.fromCompletionStage(clientConfigurationDynamoDbTable.getItem(getKey(cxId)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.IdClientNotFoundException(cxId)))
                   .doOnError(RepositoryManagerException.IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                   .flatMap(clientToDelete -> Mono.fromCompletionStage(clientConfigurationDynamoDbTable.deleteItem(getKey(cxId))));
    }
}
