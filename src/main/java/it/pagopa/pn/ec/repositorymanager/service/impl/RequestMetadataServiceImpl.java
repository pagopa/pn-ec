package it.pagopa.pn.ec.repositorymanager.service.impl;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.model.entity.DigitalProgressStatus;
import it.pagopa.pn.ec.repositorymanager.model.entity.Events;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.Retry;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Patch;
import it.pagopa.pn.ec.repositorymanager.service.RequestMetadataService;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.DYNAMO_OPTIMISTIC_LOCKING_RETRY;
import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.getKey;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.decodeMessageId;
import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.encodeMessageId;

@Service
@CustomLog
public class RequestMetadataServiceImpl implements RequestMetadataService {

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    private final DynamoDbAsyncTableDecorator<RequestMetadata> requestMetadataDynamoDbTable;

    public RequestMetadataServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient,
                                      RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        this.requestMetadataDynamoDbTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.richiesteMetadataName(),
                                                                         TableSchema.fromBean(RequestMetadata.class)));
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
    public Mono<RequestMetadata> getRequestMetadata(String concatRequestId) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, GET_REQUEST_METADATA_OP, concatRequestId);
        return Mono.fromCompletionStage(() -> {
                    Key partitionKey = getKey(concatRequestId);
                    return requestMetadataDynamoDbTable.getItem(partitionKey);
                })
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestNotFoundException(concatRequestId)))
                   .doOnError(throwable -> log.debug(EXCEPTION_IN_PROCESS, GET_REQUEST_METADATA_OP, throwable, throwable.getMessage()))
                   .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL,concatRequestId, GET_REQUEST_OP, result));
    }

    @Override
    public Mono<RequestMetadata> insertRequestMetadata(RequestMetadata requestMetadata) {
        String concatRequestId = concatRequestId(requestMetadata.getXPagopaExtchCxId(), requestMetadata.getRequestId());
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, INSERT_REQUEST_METADATA_OP, concatRequestId);
        return Mono.fromCompletionStage(() -> {
                    Key partitionKey = getKey(requestMetadata.getRequestId());
                    return requestMetadataDynamoDbTable.getItem(partitionKey);
                })
                .handle((foundedRequest, sink) -> {
                       var requestId = foundedRequest.getRequestId();
                       var foundedRequestHash = foundedRequest.getRequestHash();
                       if (!requestMetadata.getRequestHash().equals(foundedRequestHash)) {
                           sink.error(new RepositoryManagerException.IdRequestAlreadyPresent(requestId));
                       } else {
                           sink.error(new RepositoryManagerException.RequestWithSameHash(requestId, foundedRequestHash));
                       }
                   })
                   .defaultIfEmpty(requestMetadata)
                   .flatMap(unused -> {
                       if ((requestMetadata.getDigitalRequestMetadata() != null && requestMetadata.getPaperRequestMetadata() != null) ||
                           (requestMetadata.getDigitalRequestMetadata() == null && requestMetadata.getPaperRequestMetadata() == null)) {
                           return Mono.error(new RepositoryManagerException.RequestMalformedException(
                                   "Valorizzare solamente un tipologia di richiesta metadata"));
                       }

                       OffsetDateTime lastUpdateTimestamp = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
                       requestMetadata.setLastUpdateTimestamp(lastUpdateTimestamp.format(dtf));
                       return insertRequestMetadataInDynamoDb(requestMetadata);
                   })
                   .doOnError(RepositoryManagerException.RequestMalformedException.class, throwable -> log.debug(throwable.getMessage()))
                   .thenReturn(requestMetadata)
                   .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, INSERT_REQUEST_METADATA_OP, result));
    }

   private Mono<RequestMetadata> managePatch(String requestId, Patch patch, RequestMetadata retrieveRequestMetadata) {
        var retry = patch.getRetry();

        if (retry != null) {
            // Handle retry patch
            var retrievedRetry = retrieveRequestMetadata.getRetry();
            if (retrievedRetry == null) {
                retrievedRetry = new Retry();
                retrieveRequestMetadata.setRetry(retrievedRetry);
            }
            retrievedRetry.setLastRetryTimestamp(retry.getLastRetryTimestamp());
            retrievedRetry.setRetryStep(retry.getRetryStep());
            retrievedRetry.setRetryPolicy(retry.getRetryPolicy());
        } else {
            // Handle event patch
            var event = patch.getEvent();
            var eventsList = retrieveRequestMetadata.getEventsList();
            eventsCheck(event, eventsList, requestId);
            var newEventsList = new ArrayList<>(eventsList != null ? eventsList : Collections.emptyList());

            OffsetDateTime insertTimestamp = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
            event.setInsertTimestamp(insertTimestamp.format(dtf));

            newEventsList.add(event);
            retrieveRequestMetadata.setEventsList(newEventsList);
            if (event.getDigProgrStatus() != null) {
                // Handle digital request event
                retrieveRequestMetadata.setStatusRequest(event.getDigProgrStatus().getStatus());
            } else {
                // Handle paper request event
                retrieveRequestMetadata.setStatusRequest(event.getPaperProgrStatus().getStatus());
            }
        }
        return Mono.just(retrieveRequestMetadata);
    }

     protected void eventsCheck(Events event, List<Events> eventsList, String requestId) {
        if (eventsList != null && eventsList.contains(event)) {
            // Event already exists
            var status = getStatusFromEvent(event);
            if (status instanceof DigitalProgressStatus digitalProgressStatus) {
                throw new RepositoryManagerException.EventAlreadyExistsException(requestId, digitalProgressStatus);
            }
        }
    }

    private Object getStatusFromEvent(Events event) {
        return event.getDigProgrStatus() != null ? event.getDigProgrStatus() : event.getPaperProgrStatus();
    }

    @Override
    public Mono<RequestMetadata> patchRequestMetadata(String concatRequestId, Patch patch) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PATCH_REQUEST_METADATA_OP, concatRequestId);
        return getRequestMetadata(concatRequestId)
                .doOnSuccess(retrievedRequest -> checkTipoPatchMetadata(retrievedRequest, patch))
                .doOnError(RepositoryManagerException.RequestMalformedException.class,
                        throwable -> log.debug(throwable.getMessage()))
                .flatMap(retrieveRequestMetadata -> managePatch(concatRequestId,
                        patch,
                        retrieveRequestMetadata))
                .map(requestMetadata -> {
                    OffsetDateTime lastUpdateTimestamp = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
                    requestMetadata.setLastUpdateTimestamp(lastUpdateTimestamp.format(dtf));
                    return requestMetadata;
                })
                .flatMap(this::updateRequestMetadataInDynamoDb)
                .retryWhen(DYNAMO_OPTIMISTIC_LOCKING_RETRY)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, PATCH_REQUEST_METADATA_OP, result));
    }

    @Override
    public Mono<RequestMetadata> deleteRequestMetadata(String requestId) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, DELETE_REQUEST_METADATA_OP, requestId);
        return getRequestMetadata(requestId).flatMap(requestToDelete -> deleteRequestMetadataFromDynamoDb(requestId))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, requestId, DELETE_REQUEST_METADATA_OP, result));
    }

    @Override
    public Mono<RequestMetadata> getRequestMetadataByMessageId(String concatRequestId) {
        var presaInCaricoInfo = decodeMessageId(concatRequestId);
        return getRequestMetadata(concatRequestId(presaInCaricoInfo.getXPagopaExtchCxId(), presaInCaricoInfo.getRequestIdx()));
    }

    @Override
    public Mono<RequestMetadata> setMessageIdInRequestMetadata(String concatRequestId) {
        return getRequestMetadata(concatRequestId).flatMap(retrievedRequestMetadata -> {
            retrievedRequestMetadata.setMessageId(encodeMessageId(concatRequestId));
            OffsetDateTime lastUpdateTimestamp = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
            retrievedRequestMetadata.setLastUpdateTimestamp(lastUpdateTimestamp.format(dtf));
            return Mono.fromCompletionStage(requestMetadataDynamoDbTable.updateItem(retrievedRequestMetadata));
        }).retryWhen(DYNAMO_OPTIMISTIC_LOCKING_RETRY);
    }

    private Mono<Void> insertRequestMetadataInDynamoDb(RequestMetadata requestMetadata) {
        return Mono.fromCompletionStage(() -> requestMetadataDynamoDbTable.putItem(builder -> builder.item(requestMetadata)));
    }

    private Mono<RequestMetadata> updateRequestMetadataInDynamoDb(RequestMetadata requestMetadata) {
        return Mono.fromCompletionStage(requestMetadataDynamoDbTable.updateItem(requestMetadata));
    }

    private Mono<RequestMetadata> deleteRequestMetadataFromDynamoDb(String requestId) {
        return Mono.fromCompletionStage(requestMetadataDynamoDbTable.deleteItem(getKey(requestId)));
    }

}
