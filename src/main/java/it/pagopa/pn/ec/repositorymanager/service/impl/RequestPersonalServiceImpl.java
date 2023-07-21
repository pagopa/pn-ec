package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.model.entity.ClientConfigurationInternal;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestPersonal;
import it.pagopa.pn.ec.repositorymanager.service.RequestPersonalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.getKey;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.LogUtils.DELETED_DATA_FROM_DYNAMODB_TABLE;
import static it.pagopa.pn.ec.repositorymanager.utils.RequestMapper.concatRequestId;

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
        log.debug(INVOKING_OPERATION_LABEL, GET_REQUEST_PERSONAL_OP, concatRequestId);
        return Mono.fromCompletionStage(requestPersonalDynamoDbTable.getItem(getKey(concatRequestId))).defaultIfEmpty(new RequestPersonal())
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, GET_REQUEST_PERSONAL_OP, result));
    }

    @Override
    public Mono<RequestPersonal> insertRequestPersonal(RequestPersonal requestPersonal) {
        String concatRequestId = concatRequestId(requestPersonal.getXPagopaExtchCxId(), requestPersonal.getRequestId());
        log.debug(INVOKING_OPERATION_LABEL, INSERT_REQUEST_PERSONAL_OP, concatRequestId);
        return Mono.fromCompletionStage(requestPersonalDynamoDbTable.getItem(getKey(requestPersonal.getRequestId())))
                   .flatMap(foundedRequest -> Mono.error(new RepositoryManagerException.IdRequestAlreadyPresent(requestPersonal.getRequestId())))
                   .defaultIfEmpty(requestPersonal)
                   .flatMap(unused -> {
                       if ((requestPersonal.getDigitalRequestPersonal() != null && requestPersonal.getPaperRequestPersonal() != null) ||
                           (requestPersonal.getDigitalRequestPersonal() == null && requestPersonal.getPaperRequestPersonal() == null)) {
                           return Mono.error(new RepositoryManagerException.RequestMalformedException(
                                   "Valorizzare solamente un tipologia di richiesta personal"));
                       }
                       return putRequestPersonalInDynamoDb(requestPersonal);
                   })
                   .doOnError(RepositoryManagerException.IdRequestAlreadyPresent.class, throwable -> log.info(throwable.getMessage()))
                   .doOnError(RepositoryManagerException.RequestMalformedException.class, throwable -> log.info(throwable.getMessage()))
                   .thenReturn(requestPersonal)
                   .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, INSERT_REQUEST_PERSONAL_OP, result));
    }

    @Override
    public Mono<RequestPersonal> deleteRequestPersonal(String concatRequestId) {
        log.debug(INVOKING_OPERATION_LABEL, DELETE_REQUEST_PERSONAL_OP, concatRequestId);
        return Mono.fromCompletionStage(requestPersonalDynamoDbTable.getItem(getKey(concatRequestId)))
                   .defaultIfEmpty(new RequestPersonal())
                   .flatMap(requestToDelete -> deleteRequestPersonalFromDynamoDb(concatRequestId))
                   .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, DELETE_REQUEST_PERSONAL_OP, result));
    }

    private Mono<Void> putRequestPersonalInDynamoDb(RequestPersonal requestPersonal) {
        log.debug(INSERTING_DATA_IN_DYNAMODB_TABLE, requestPersonal, requestPersonalDynamoDbTable.tableName());
        return Mono.fromCompletionStage(requestPersonalDynamoDbTable.putItem(builder -> builder.item(requestPersonal)))
                .doOnSuccess(result -> log.info(INSERTED_DATA_IN_DYNAMODB_TABLE, requestPersonalDynamoDbTable.tableName()));
    }

    private Mono<RequestPersonal> deleteRequestPersonalFromDynamoDb(String concatRequestId) {
        log.debug(DELETING_DATA_FROM_DYNAMODB_TABLE, concatRequestId, requestPersonalDynamoDbTable.tableName());
        return Mono.fromCompletionStage(requestPersonalDynamoDbTable.deleteItem(getKey(concatRequestId)))
                .doOnNext(result -> log.info(DELETED_DATA_FROM_DYNAMODB_TABLE, requestPersonalDynamoDbTable.tableName()));
    }

}
