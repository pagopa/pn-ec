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

import java.time.OffsetDateTime;
import java.util.List;

import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.getKey;
import static it.pagopa.pn.ec.repositorymanager.constant.GestoreRepositoryDynamoDbTableName.REQUEST_TABLE_NAME;

@Service
@Slf4j
public class RequestServiceImpl implements RequestService {

    private final DynamoDbAsyncTable<Request> requestDynamoDbTable;

    private void checkRequestToInsert(Request request) {

        if ((request.getDigitalReq() != null && request.getPaperReq() != null) ||
            (request.getDigitalReq() == null && request.getPaperReq() == null)) {
            throw new RepositoryManagerException.RequestMalformedException("Valorizzare solamente un tipologia di richiesta");
        }

        List<Events> eventsList = request.getEvents();
        if (eventsList.isEmpty()) {
            throw new RepositoryManagerException.RequestMalformedException("Valorizzare una tipologia di evento");
        } else if (eventsList.size() > 1) {
            throw new RepositoryManagerException.RequestMalformedException("Inserire un solo evento");
        } else {
            checkEvents(request, eventsList.get(0));
        }
    }

    private void checkEvents(Request request, Events events) {
        boolean isDigital = request.getDigitalReq() != null;
        if ((isDigital && events.getPaperProgrStatus() != null) || (!isDigital && events.getDigProgrStatus() != null)) {
            throw new RepositoryManagerException.RequestMalformedException("Tipo richiesta e tipo evento non compatibili");
        }
    }

    public RequestServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient) {
        this.requestDynamoDbTable = dynamoDbEnhancedClient.table(REQUEST_TABLE_NAME, TableSchema.fromBean(Request.class));
    }

    @Override
    public Mono<Request> getRequest(String requestIdx) {
        return Mono.fromCompletionStage(requestDynamoDbTable.getItem(getKey(requestIdx)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
                   .doOnError(RepositoryManagerException.RequestNotFoundException.class, throwable -> log.info(throwable.getMessage()));
    }

    @Override
    public Mono<Request> insertRequest(Request request) {
        return Mono.fromCompletionStage(requestDynamoDbTable.getItem(getKey(request.getRequestId())))
                   .handle((foundedRequest, sink) -> {
                       if (foundedRequest != null) {
                           sink.error(new RepositoryManagerException.IdRequestAlreadyPresent(request.getRequestId()));
                       }
                   })
                   .doOnError(RepositoryManagerException.IdRequestAlreadyPresent.class, throwable -> log.info(throwable.getMessage()))
                   .doOnSuccess(unused -> checkRequestToInsert(request))
                   .doOnError(RepositoryManagerException.RequestMalformedException.class, throwable -> log.info(throwable.getMessage()))
                   .doOnSuccess(unused -> {
                       Events firstStatus = request.getEvents().get(0);
                       if (request.getDigitalReq() != null) {
                           request.setStatusRequest(firstStatus.getDigProgrStatus().getStatus());
                           firstStatus.getDigProgrStatus().setEventTimestamp(OffsetDateTime.now());
                       } else {
                           request.setStatusRequest(firstStatus.getPaperProgrStatus().getStatusDescription());
                           firstStatus.getPaperProgrStatus().setStatusDateTime(OffsetDateTime.now());
                       }
                       request.setClientRequestTimeStamp(OffsetDateTime.now());
                       requestDynamoDbTable.putItem(builder -> builder.item(request));
                   })
                   .thenReturn(request);
    }

    @Override
    public Mono<Request> updateEvents(String requestIdx, Events events) {
        return Mono.fromCompletionStage(requestDynamoDbTable.getItem(getKey(requestIdx)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
                   .doOnError(RepositoryManagerException.RequestNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                   .doOnSuccess(retrievedRequest -> checkEvents(retrievedRequest, events))
                   .doOnError(RepositoryManagerException.RequestMalformedException.class, throwable -> log.info(throwable.getMessage()))
                   .map(retrieveRequest -> {
                       if (events.getDigProgrStatus() != null) {
                           retrieveRequest.setStatusRequest(events.getDigProgrStatus().getStatus());
                           events.getDigProgrStatus().setEventTimestamp(OffsetDateTime.now());
                       } else {
                           retrieveRequest.setStatusRequest(events.getPaperProgrStatus().getStatusDescription());
                           events.getPaperProgrStatus().setStatusDateTime(OffsetDateTime.now());
                       }
                       retrieveRequest.getEvents().add(events);
                       requestDynamoDbTable.updateItem(retrieveRequest);
                       return retrieveRequest;
                   });
    }

    @Override
    public Mono<Request> deleteRequest(String requestIdx) {
        return Mono.fromCompletionStage(requestDynamoDbTable.getItem(getKey(requestIdx)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
                   .doOnError(RepositoryManagerException.RequestNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                   .doOnSuccess(requestToDelete -> requestDynamoDbTable.deleteItem(getKey(requestIdx)))
                   .map(request -> request);
    }
}
