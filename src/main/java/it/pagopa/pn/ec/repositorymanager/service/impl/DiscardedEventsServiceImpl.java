package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.model.entity.DiscardedEvent;
import it.pagopa.pn.ec.repositorymanager.service.DiscardedEventsService;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.LogUtils.GET_REQUEST_OP;

@Service
@CustomLog
public class DiscardedEventsServiceImpl implements DiscardedEventsService {


    private final DynamoDbAsyncTableDecorator<DiscardedEvent> discardedEventDynamoDbAsyncTable;


    public DiscardedEventsServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        this.discardedEventDynamoDbAsyncTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.scartiConsolidatoreName(), TableSchema.fromBean(DiscardedEvent.class)));
    }

    @Override
    public Mono<DiscardedEvent> insertDiscardedEvent(DiscardedEvent discardedEvent) {
        return Mono.fromCompletionStage(() -> discardedEventDynamoDbAsyncTable.putItem(builder -> builder.item(discardedEvent)))
                .thenReturn(discardedEvent)
                .doOnError(throwable -> log.debug(EXCEPTION_IN_PROCESS, GET_REQUEST_METADATA_OP, throwable, throwable.getMessage()))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, discardedEvent.getRequestId(), GET_REQUEST_OP, result));
    }
}
