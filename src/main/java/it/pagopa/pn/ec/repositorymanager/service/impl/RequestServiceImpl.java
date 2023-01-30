package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.repositorymanager.entity.Events;
import it.pagopa.pn.ec.repositorymanager.entity.Request;
import it.pagopa.pn.ec.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.service.RequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.getKey;
import static it.pagopa.pn.ec.repositorymanager.constant.GestoreRepositoryDynamoDbTableName.REQUEST_TABLE_NAME;

@Service
@Slf4j
public class RequestServiceImpl implements RequestService {

    private final DynamoDbAsyncTable<Request> requestDynamoDbTable;

    private void checkRequestType(Request request){
        if(request.getDigitalReq() != null && request.getPaperReq() != null){

        }
    }

    public RequestServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient) {
        this.requestDynamoDbTable = dynamoDbEnhancedClient.table(REQUEST_TABLE_NAME, TableSchema.fromBean(Request.class));
    }

    @Override
    public Mono<Request> getRequest(String requestIdx) {
        return Mono.fromCompletionStage(requestDynamoDbTable.getItem(getKey(requestIdx)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.IdClientNotFoundException(requestIdx)))
                   .doOnError(RepositoryManagerException.IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()));
    }

    @Override
    public Mono<Request> insertRequest(Request request) {
        return Mono.fromCompletionStage(requestDynamoDbTable.getItem(getKey(request.getRequestId())))
                   .handle((foundedRequest, sink) -> {
                       if (foundedRequest != null) {
                           sink.error(new RepositoryManagerException.IdClientAlreadyPresent(request.getRequestId()));
                       }
                   })
                   .doOnError(RepositoryManagerException.IdClientAlreadyPresent.class, throwable -> log.info(throwable.getMessage()))
                   .doOnSuccess(unused -> {
                       Events firstStatus = request.getEvents().get(0);
                       if (request.getDigitalReq() != null) {
                           request.setStatusRequest(firstStatus.getDigProgrStatus().getStatus());
                       } else {
                           request.setStatusRequest(firstStatus.getPaperProgrStatus().getStatusDescription());
                       }
                       requestDynamoDbTable.putItem(builder -> builder.item(request));
                   })
                   .thenReturn(request);
    }

    @Override
    public Mono<Request> updateEvents(String requestIdx, Events events) {
        return Mono.fromCompletionStage(requestDynamoDbTable.getItem(getKey(requestIdx)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.IdClientNotFoundException(requestIdx)))
                   .doOnError(RepositoryManagerException.IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                   .map(retrieveRequest -> {
                       if (events.getDigProgrStatus() != null) {
                           retrieveRequest.setStatusRequest(events.getDigProgrStatus().getStatus());
                       } else {
                           retrieveRequest.setStatusRequest(events.getPaperProgrStatus().getStatusDescription());
                       }
                       retrieveRequest.getEvents().add(events);
                       requestDynamoDbTable.updateItem(retrieveRequest);
                       return retrieveRequest;
                   });
    }

    @Override
    public Mono<Request> deleteRequest(String requestIdx) {
        return Mono.fromCompletionStage(requestDynamoDbTable.getItem(getKey(requestIdx)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.IdClientNotFoundException(requestIdx)))
                   .doOnError(RepositoryManagerException.IdClientNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                   .doOnSuccess(requestToDelete -> requestDynamoDbTable.deleteItem(getKey(requestIdx)))
                   .map(request -> request);
    }

}
