package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.model.entity.Events;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.service.RequestMetadataService;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.getKey;

@Service
@Slf4j
public class RequestMetadataServiceImpl implements RequestMetadataService {

    private final DynamoDbAsyncTable<RequestMetadata> requestMetadataDynamoDbTable;

    public RequestMetadataServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient,
                                      RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        this.requestMetadataDynamoDbTable = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.richiesteMetadataName(),
                                                                         TableSchema.fromBean(RequestMetadata.class));
    }

    private void checkRequestMetadataToInsert(RequestMetadata requestMetadata) {

        if ((requestMetadata.getDigitalRequestMetadata() != null && requestMetadata.getPaperRequestMetadata() != null) ||
            (requestMetadata.getDigitalRequestMetadata() == null && requestMetadata.getPaperRequestMetadata() == null)) {
            throw new RepositoryManagerException.RequestMalformedException("Valorizzare solamente un tipologia di richiesta metadata");
        }
    }

    private void checkEventsMetadata(RequestMetadata requestMetadata, Events events) {
        boolean isDigital = requestMetadata.getDigitalRequestMetadata() != null;
        if ((isDigital && events.getPaperProgrStatus() != null) ||
            (!isDigital && events.getDigProgrStatus() != null)) {
            throw new RepositoryManagerException.RequestMalformedException(
                    "Tipo richiesta metadata e tipo evento metadata non " + "compatibili");
        }
    }

    @Override
    public Mono<RequestMetadata> getRequestMetadata(String requestIdx) {
        return Mono.fromCompletionStage(requestMetadataDynamoDbTable.getItem(getKey(requestIdx)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
                   .doOnError(RepositoryManagerException.RequestNotFoundException.class, throwable -> log.info(throwable.getMessage()));
    }

    @Override
    public Mono<RequestMetadata> insertRequestMetadata(RequestMetadata requestMetadata) {
        return Mono.fromCompletionStage(requestMetadataDynamoDbTable.getItem(getKey(requestMetadata.getRequestId())))
                   .handle((foundedRequest, sink) -> {
                       if (foundedRequest != null) {
                           sink.error(new RepositoryManagerException.IdRequestAlreadyPresent(requestMetadata.getRequestId()));
                       }
                   })
                   .doOnError(RepositoryManagerException.IdRequestAlreadyPresent.class, throwable -> log.info(throwable.getMessage()))
                   .doOnSuccess(unused -> checkRequestMetadataToInsert(requestMetadata))
                   .doOnError(RepositoryManagerException.RequestMalformedException.class, throwable -> log.info(throwable.getMessage()))
                   .doOnSuccess(unused -> {
                       if (requestMetadata.getEventsList() != null && !requestMetadata.getEventsList().isEmpty()) {
                           Events firstStatus = requestMetadata.getEventsList().get(0);
                           if (requestMetadata.getDigitalRequestMetadata() != null) {
                               requestMetadata.setStatusRequest(firstStatus.getDigProgrStatus().getStatus().getValue());
                               firstStatus.getDigProgrStatus().setEventTimestamp(OffsetDateTime.now());
                           } else {
                               requestMetadata.setStatusRequest(firstStatus.getPaperProgrStatus().getStatusDescription());
                               firstStatus.getPaperProgrStatus().setStatusDateTime(OffsetDateTime.now());
                           }
                       }
                       requestMetadataDynamoDbTable.putItem(builder -> builder.item(requestMetadata));
                   })
                   .thenReturn(requestMetadata);
    }

    @Override
    public Mono<RequestMetadata> updateEventsMetadata(String requestIdx, Events events) {
        return Mono.fromCompletionStage(requestMetadataDynamoDbTable.getItem(getKey(requestIdx)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
                   .doOnError(RepositoryManagerException.RequestNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                   .doOnSuccess(retrievedRequest -> checkEventsMetadata(retrievedRequest, events))
                   .doOnError(RepositoryManagerException.RequestMalformedException.class, throwable -> log.info(throwable.getMessage()))
                   .map(retrieveRequest -> {
                       if (events.getDigProgrStatus() != null) {
                           retrieveRequest.setStatusRequest(events.getDigProgrStatus().getStatus().getValue());
                           events.getDigProgrStatus().setEventTimestamp(OffsetDateTime.now());
                       } else {
                           retrieveRequest.setStatusRequest(events.getPaperProgrStatus().getStatusDescription());
                           events.getPaperProgrStatus().setStatusDateTime(OffsetDateTime.now());
                       }
                       List<Events> eventsList = retrieveRequest.getEventsList();
                       if(eventsList == null) {
                           eventsList = new ArrayList<>();
                       }
                       eventsList.add(events);
                       requestMetadataDynamoDbTable.updateItem(retrieveRequest);
                       return retrieveRequest;
                   });
    }

    @Override
    public Mono<RequestMetadata> deleteRequestMetadata(String requestIdx) {
        return Mono.fromCompletionStage(requestMetadataDynamoDbTable.getItem(getKey(requestIdx)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
                   .doOnError(RepositoryManagerException.RequestNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                   .doOnSuccess(requestToDelete -> requestMetadataDynamoDbTable.deleteItem(getKey(requestIdx)))
                   .map(requestMetadata -> requestMetadata);
    }
}
