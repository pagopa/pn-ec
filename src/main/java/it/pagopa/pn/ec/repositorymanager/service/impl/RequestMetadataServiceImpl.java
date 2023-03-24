package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.model.entity.Events;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.Retry;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Patch;
import it.pagopa.pn.ec.repositorymanager.service.RequestMetadataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.DYNAMO_OPTIMISTIC_LOCKING_RETRY;
import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.getKey;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.decodeMessageId;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.encodeMessageId;

@Service
@Slf4j
public class RequestMetadataServiceImpl implements RequestMetadataService {

    private final DynamoDbAsyncTable<RequestMetadata> requestMetadataDynamoDbTable;

    public RequestMetadataServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient
            , RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        this.requestMetadataDynamoDbTable = dynamoDbEnhancedClient
                .table(repositoryManagerDynamoTableName.richiesteMetadataName()
                        , TableSchema.fromBean(RequestMetadata.class));
    }

    private void checkTipoPatchMetadata(RequestMetadata requestMetadata, Patch patch) {

        boolean isRetry = patch.getRetry() != null;

        if ((patch.getEvent() != null && isRetry) || (patch.getEvent() == null && !isRetry)) {
            throw new RepositoryManagerException.RequestMalformedException(
                    "Valorizzare una e una sola tipologia di aggiornamento " + "richiesta");
        }

        if (!isRetry) {
            boolean isDigital = requestMetadata.getDigitalRequestMetadata() != null;
            if ((isDigital && patch.getEvent().getPaperProgrStatus() != null) ||
                (!isDigital && patch.getEvent().getDigProgrStatus() != null)) {
                throw new RepositoryManagerException.RequestMalformedException(
                        "Tipo richiesta metadata e tipo evento metadata non " + "compatibili");
            }
        }
    }

    @Override
    public Mono<RequestMetadata> getRequestMetadata(String requestId) {
        return Mono.fromCompletionStage(requestMetadataDynamoDbTable.getItem(getKey(requestId)))
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestNotFoundException(requestId)))
                   .doOnError(RepositoryManagerException.RequestNotFoundException.class, throwable -> log.info(throwable.getMessage()));
    }

    @Override
    public Mono<RequestMetadata> insertRequestMetadata(RequestMetadata requestMetadata) {
        return Mono.fromCompletionStage(requestMetadataDynamoDbTable.getItem(getKey(requestMetadata.getRequestId())))
                   .flatMap(foundedRequest -> Mono.error(new RepositoryManagerException.IdRequestAlreadyPresent(requestMetadata.getRequestId())))
                   .doOnError(RepositoryManagerException.IdRequestAlreadyPresent.class, throwable -> log.info(throwable.getMessage()))
                   .switchIfEmpty(Mono.just(requestMetadata))
                   .flatMap(unused -> {
                       if ((requestMetadata.getDigitalRequestMetadata() != null && requestMetadata.getPaperRequestMetadata() != null) ||
                           (requestMetadata.getDigitalRequestMetadata() == null && requestMetadata.getPaperRequestMetadata() == null)) {
                           return Mono.error(new RepositoryManagerException.RequestMalformedException(
                                   "Valorizzare solamente un tipologia di richiesta metadata"));
                       }
                       return Mono.fromCompletionStage(requestMetadataDynamoDbTable.putItem(builder -> builder.item(requestMetadata)));
                   })
                   .doOnError(RepositoryManagerException.RequestMalformedException.class, throwable -> log.error(throwable.getMessage()))
                   .thenReturn(requestMetadata);
    }

    private Mono<RequestMetadata> managePatch(String requestId, Patch patch, RequestMetadata retrieveRequestMetadata) {
        var retry = patch.getRetry();

        if (retry != null) {
            // ---> CASO: RETRY <---
            var retrievedRetry = retrieveRequestMetadata.getRetry();

            if (retrievedRetry == null) {
                retrievedRetry = new Retry();
            }

            retrievedRetry.setLastRetryTimestamp(retry.getLastRetryTimestamp());
            retrievedRetry.setRetryStep(retry.getRetryStep());
            retrievedRetry.setRetryPolicy(retry.getRetryPolicy());

            retrieveRequestMetadata.setRetry(retrievedRetry);
            return Mono.just(retrieveRequestMetadata);

        } else {

            var event = patch.getEvent();
            var eventsList = retrieveRequestMetadata.getEventsList(); // lista eventi metadata

            if (event.getDigProgrStatus() != null) {
                // ---> CASO 1: RICHIESTA DIGITALE <---
                if (eventsList != null) {
                    // Controlla se l'evento che stiamo inserendo sia già presente
                    for (Events currentCycledEvent : eventsList) {
                        if (currentCycledEvent.equals(event)) {
                            return Mono.error(new RepositoryManagerException.EventAlreadyExistsException(requestId, event.getDigProgrStatus()));
                        }
                    }
                }
                retrieveRequestMetadata.setStatusRequest(event.getDigProgrStatus().getStatus());
            } else {
                // ---> CASO 2: RICHIESTA CARTACEO <---
                if (eventsList != null) {
                    // Controlla se l'evento che stiamo inserendo sia già presente
                    for (Events currentCycledEvent : eventsList) {
                        if (currentCycledEvent.equals(event)) {
                            return Mono.error(new RepositoryManagerException.EventAlreadyExistsException(requestId, event.getPaperProgrStatus()));
                        }
                    }
                }
                retrieveRequestMetadata.setStatusRequest(event.getPaperProgrStatus().getStatusDescription());
            }

            List<Events> getEventsList = retrieveRequestMetadata.getEventsList();
            if (getEventsList == null) {
                getEventsList = new ArrayList<>();
            }

            getEventsList.add(event);
            retrieveRequestMetadata.setEventsList(getEventsList);
            return Mono.just(retrieveRequestMetadata);
        }
    }
    
    @Override
    public Mono<RequestMetadata> patchRequestMetadata(String requestId, Patch patch) {
        return getRequestMetadata(requestId)//
                .doOnSuccess(retrievedRequest -> checkTipoPatchMetadata(retrievedRequest, patch))
                .doOnError(RepositoryManagerException.RequestMalformedException.class, throwable -> log.info(throwable.getMessage()))
                .flatMap(retrieveRequestMetadata -> managePatch(requestId, patch, retrieveRequestMetadata))
                .flatMap(requestMetadataWithPatchUpdated -> Mono.fromCompletionStage(requestMetadataDynamoDbTable.updateItem(requestMetadataWithPatchUpdated)))
                .retryWhen(DYNAMO_OPTIMISTIC_LOCKING_RETRY);
    }

    @Override
    public Mono<RequestMetadata> deleteRequestMetadata(String requestId) {
        return getRequestMetadata(requestId).flatMap(requestToDelete -> Mono.fromCompletionStage(requestMetadataDynamoDbTable.deleteItem(
                getKey(requestId))));
    }

    @Override
    public Mono<RequestMetadata> getRequestMetadataByMessageId(String messageId) {
        return getRequestMetadata(decodeMessageId(messageId).getRequestIdx());
    }

    @Override
    public Mono<RequestMetadata> setMessageIdInRequestMetadata(String requestId) {
        return getRequestMetadata(requestId).flatMap(retrievedRequestMetadata -> {
            retrievedRequestMetadata.setMessageId(encodeMessageId(requestId, retrievedRequestMetadata.getXPagopaExtchCxId()));
            return Mono.fromCompletionStage(requestMetadataDynamoDbTable.updateItem(retrievedRequestMetadata));
        }).retryWhen(DYNAMO_OPTIMISTIC_LOCKING_RETRY);
    }
}
