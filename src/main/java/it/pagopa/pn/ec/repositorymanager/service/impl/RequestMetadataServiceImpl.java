package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.model.entity.EventsMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.service.RequestMetadataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.OffsetDateTime;

import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.getKey;
import static it.pagopa.pn.ec.repositorymanager.constant.GestoreRepositoryDynamoDbTableName.REQUEST_METADATA_TABLE_NAME;

@Service
@Slf4j
public class RequestMetadataServiceImpl implements RequestMetadataService {

    private final DynamoDbAsyncTable<RequestMetadata> requestMetadataDynamoDbTable;

    public RequestMetadataServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient) {
        this.requestMetadataDynamoDbTable =
                dynamoDbEnhancedClient.table(REQUEST_METADATA_TABLE_NAME, TableSchema.fromBean(RequestMetadata.class));
    }

    private void checkRequestMetadataToInsert(RequestMetadata requestMetadata) {

        if ((requestMetadata.getDigitalRequestMetadata() != null && requestMetadata.getPaperRequestMetadata() != null) ||
            (requestMetadata.getDigitalRequestMetadata() == null && requestMetadata.getPaperRequestMetadata() == null)) {
            throw new RepositoryManagerException.RequestMalformedException("Valorizzare solamente un tipologia di richiesta metadata");
        }
    }

    private void checkEventsMetadata(RequestMetadata requestMetadata, EventsMetadata eventsMetadata) {
        boolean isDigital = requestMetadata.getDigitalRequestMetadata() != null;
        if ((isDigital && eventsMetadata.getPaperProgrStatusMetadata() != null) ||
            (!isDigital && eventsMetadata.getDigProgrStatusMetadata() != null)) {
            throw new RepositoryManagerException.RequestMalformedException("Tipo richiesta metadata e tipo evento metadata non " +
                                                                           "compatibili");
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
                       if (requestMetadata.getEventsMetadataList() != null && !requestMetadata.getEventsMetadataList().isEmpty()) {
                           EventsMetadata firstStatus = requestMetadata.getEventsMetadataList().get(0);
                           if (requestMetadata.getDigitalRequestMetadata() != null) {
                               requestMetadata.setStatusRequest(firstStatus.getDigProgrStatusMetadata().getStatus().getValue());
                               firstStatus.getDigProgrStatusMetadata().setEventTimestamp(OffsetDateTime.now());
                           } else {
                               requestMetadata.setStatusRequest(firstStatus.getPaperProgrStatusMetadata().getStatusDescription());
                               firstStatus.getPaperProgrStatusMetadata().setStatusDateTime(OffsetDateTime.now());
                           }
                       }
                       requestMetadataDynamoDbTable.putItem(builder -> builder.item(requestMetadata));
                   })
                   .thenReturn(requestMetadata);
    }

    @Override
    public Mono<RequestMetadata> updateEventsMetadata(String requestIdx, EventsMetadata eventsMetadata) {
        return Mono.fromCompletionStage(requestMetadataDynamoDbTable.getItem(getKey(requestIdx)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
                   .doOnError(RepositoryManagerException.RequestNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                   .doOnSuccess(retrievedRequest -> checkEventsMetadata(retrievedRequest, eventsMetadata))
                   .doOnError(RepositoryManagerException.RequestMalformedException.class, throwable -> log.info(throwable.getMessage()))
                   .map(retrieveRequest -> {
                       if (eventsMetadata.getDigProgrStatusMetadata() != null) {
                           retrieveRequest.setStatusRequest(eventsMetadata.getDigProgrStatusMetadata().getStatus().getValue());
                           eventsMetadata.getDigProgrStatusMetadata().setEventTimestamp(OffsetDateTime.now());
                       } else {
                           retrieveRequest.setStatusRequest(eventsMetadata.getPaperProgrStatusMetadata().getStatusDescription());
                           eventsMetadata.getPaperProgrStatusMetadata().setStatusDateTime(OffsetDateTime.now());
                       }
                       retrieveRequest.getEventsMetadataList().add(eventsMetadata);
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
