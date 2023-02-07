package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
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

@Service
@Slf4j
public class RequestServiceImpl implements RequestService {

    private final DynamoDbAsyncTable<Request> requestDynamoDbAsyncTable;

    public RequestServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                              RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        this.requestDynamoDbAsyncTable =
                dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.richiesteName(), TableSchema.fromBean(Request.class));
    }

    private void checkRequestToInsert(Request request) {

        if ((request.getDigitalReq() != null && request.getPaperReq() != null) ||
            (request.getDigitalReq() == null && request.getPaperReq() == null)) {
            throw new RepositoryManagerException.RequestMalformedException("Valorizzare solamente un tipologia di richiesta");
        }

        List<Events> eventsList = request.getEvents();
        if (!eventsList.isEmpty()) {
            if (eventsList.size() > 1) {
                throw new RepositoryManagerException.RequestMalformedException("Inserire un solo evento");

            }
            checkEvents(request, eventsList.get(0));
        }
    }

    private void checkEvents(Request request, Events events) {
        boolean isDigital = request.getDigitalReq() != null;
        if ((isDigital && events.getPaperProgrStatus() != null) || (!isDigital && events.getDigProgrStatus() != null)) {
            throw new RepositoryManagerException.RequestMalformedException("Tipo richiesta e tipo evento non compatibili");
        }
    }

    @Override
    public Mono<Request> getRequest(String requestIdx) {
        return Mono.fromCompletionStage(requestDynamoDbAsyncTable.getItem(getKey(requestIdx)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
                   .doOnError(RepositoryManagerException.RequestNotFoundException.class, throwable -> log.info(throwable.getMessage()));
    }

    @Override
    public Mono<Request> insertRequest(Request request) {
        return Mono.fromCompletionStage(requestDynamoDbAsyncTable.getItem(getKey(request.getRequestId())))
                   .handle((foundedRequest, sink) -> {
                       if (foundedRequest != null) {
                           sink.error(new RepositoryManagerException.IdRequestAlreadyPresent(request.getRequestId()));
                       }
                   })
                   .doOnError(RepositoryManagerException.IdRequestAlreadyPresent.class, throwable -> log.info(throwable.getMessage()))
                   .doOnSuccess(unused -> checkRequestToInsert(request))
                   .doOnError(RepositoryManagerException.RequestMalformedException.class, throwable -> log.info(throwable.getMessage()))
                   .doOnSuccess(unused -> {
                       if (request.getEvents() != null && !request.getEvents().isEmpty()) {
                           Events firstStatus = request.getEvents().get(0);
                           if (request.getDigitalReq() != null) {
                               request.setStatusRequest(firstStatus.getDigProgrStatus().getStatus().getValue());
                               firstStatus.getDigProgrStatus().setEventTimestamp(OffsetDateTime.now());
                           } else {
                               request.setStatusRequest(firstStatus.getPaperProgrStatus().getStatusDescription());
                               firstStatus.getPaperProgrStatus().setStatusDateTime(OffsetDateTime.now());
                           }
                       }
                       requestDynamoDbAsyncTable.putItem(builder -> builder.item(request));
                   })
                   .thenReturn(request);
    }

    @Override
    public Mono<Request> updateEvents(String requestIdx, Events events) {
        return Mono.fromCompletionStage(requestDynamoDbAsyncTable.getItem(getKey(requestIdx)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
                   .doOnError(RepositoryManagerException.RequestNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                   .doOnSuccess(retrievedRequest -> checkEvents(retrievedRequest, events))
                   .doOnError(RepositoryManagerException.RequestMalformedException.class, throwable -> log.info(throwable.getMessage()))
                   .map(retrieveRequest -> {
                       if (events.getDigProgrStatus() != null) {
                           retrieveRequest.setStatusRequest(events.getDigProgrStatus().getStatus().getValue());
                           events.getDigProgrStatus().setEventTimestamp(OffsetDateTime.now());
                       } else {
                           retrieveRequest.setStatusRequest(events.getPaperProgrStatus().getStatusDescription());
                           events.getPaperProgrStatus().setStatusDateTime(OffsetDateTime.now());
                       }
                       retrieveRequest.getEvents().add(events);
                       requestDynamoDbAsyncTable.updateItem(retrieveRequest);
                       return retrieveRequest;
                   });
    }

    @Override
    public Mono<Request> deleteRequest(String requestIdx) {
        return Mono.fromCompletionStage(requestDynamoDbAsyncTable.getItem(getKey(requestIdx)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
                   .doOnError(RepositoryManagerException.RequestNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                   .doOnSuccess(requestToDelete -> requestDynamoDbAsyncTable.deleteItem(getKey(requestIdx)))
                   .map(request -> request);
    }
}
