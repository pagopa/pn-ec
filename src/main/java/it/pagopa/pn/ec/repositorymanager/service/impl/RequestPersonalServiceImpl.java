package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestPersonal;
import it.pagopa.pn.ec.repositorymanager.service.RequestPersonalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.getKey;

@Service
@Slf4j
public class RequestPersonalServiceImpl implements RequestPersonalService {

    private final DynamoDbAsyncTable<RequestPersonal> requestPersonalDynamoDbTable;

    public RequestPersonalServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient,
                                      RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        this.requestPersonalDynamoDbTable = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.richiestePersonalName(),
                                                                         TableSchema.fromBean(RequestPersonal.class));
    }

    @Override
    public Mono<RequestPersonal> getRequestPersonal(String concatRequestId) {

        return Mono.fromCompletionStage(requestPersonalDynamoDbTable.getItem(getKey(concatRequestId))).defaultIfEmpty(new RequestPersonal());
    }

    @Override
    public Mono<RequestPersonal> insertRequestPersonal(RequestPersonal requestPersonal) {
        return Mono.fromCompletionStage(requestPersonalDynamoDbTable.getItem(getKey(requestPersonal.getRequestId())))
                   .flatMap(foundedRequest -> Mono.error(new RepositoryManagerException.IdRequestAlreadyPresent(requestPersonal.getRequestId())))
                   .defaultIfEmpty(requestPersonal)
                   .flatMap(unused -> {
                       if ((requestPersonal.getDigitalRequestPersonal() != null && requestPersonal.getPaperRequestPersonal() != null) ||
                           (requestPersonal.getDigitalRequestPersonal() == null && requestPersonal.getPaperRequestPersonal() == null)) {
                           return Mono.error(new RepositoryManagerException.RequestMalformedException(
                                   "Valorizzare solamente un tipologia di richiesta personal"));
                       }
                       return Mono.fromCompletionStage(requestPersonalDynamoDbTable.putItem(builder -> builder.item(requestPersonal)));
                   })
                   .doOnError(RepositoryManagerException.IdRequestAlreadyPresent.class, throwable -> log.info(throwable.getMessage()))
                   .doOnError(RepositoryManagerException.RequestMalformedException.class, throwable -> log.info(throwable.getMessage()))
                   .thenReturn(requestPersonal);
    }

    @Override
    public Mono<RequestPersonal> deleteRequestPersonal(String concatRequestId) {
        return Mono.fromCompletionStage(requestPersonalDynamoDbTable.getItem(getKey(concatRequestId)))
                   .defaultIfEmpty(new RequestPersonal())
                   .flatMap(requestToDelete -> Mono.fromCompletionStage(requestPersonalDynamoDbTable.deleteItem(getKey(concatRequestId))));
    }
}
