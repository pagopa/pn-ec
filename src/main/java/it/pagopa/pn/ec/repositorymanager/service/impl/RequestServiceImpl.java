package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.repositorymanager.entity.Events;
import it.pagopa.pn.ec.repositorymanager.entity.Request;
import it.pagopa.pn.ec.repositorymanager.service.RequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static it.pagopa.pn.ec.repositorymanager.constant.GestoreRepositoryDynamoDbTableName.REQUEST_TABLE_NAME;

@Service
@Slf4j
public class RequestServiceImpl implements RequestService {

    private final DynamoDbAsyncTable<Request> requestDynamoDbTable;

    public RequestServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient) {
        this.requestDynamoDbTable = dynamoDbEnhancedClient.table(REQUEST_TABLE_NAME, TableSchema.fromBean(Request.class));
    }

    @Override
    public Mono<Request> getRequest(String requestId) {
        return null;
    }

    @Override
    public Mono<Request> insertRequest(Request request) {
        return null;
    }

    @Override
    public Mono<Events> updateEvents(String requestId, Events events) {
        return null;
    }

    @Override
    public Mono<Request> deleteRequest(String requestId) {
        return null;
    }
}
