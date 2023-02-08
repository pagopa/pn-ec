package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.repositorymanager.entity.EventsPersonal;
import it.pagopa.pn.ec.repositorymanager.entity.RequestPersonal;
import it.pagopa.pn.ec.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.service.RequestPersonalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;

import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.getKey;
import static it.pagopa.pn.ec.repositorymanager.constant.GestoreRepositoryDynamoDbTableName.REQUEST_PERSONAL_TABLE_NAME;

@Service
@Slf4j
public class RequestPersonalServiceImpl implements RequestPersonalService {

    private final DynamoDbAsyncTable<RequestPersonal> requestPersonalDynamoDbTable;

    private void checkRequestPersonalToInsert(RequestPersonal requestPersonal) {

        if ((requestPersonal.getDigitalReq() != null && requestPersonal.getPaperReq() != null) ||
                (requestPersonal.getDigitalReq() == null && requestPersonal.getPaperReq() == null)) {
            throw new RepositoryManagerException.RequestMalformedException("Valorizzare solamente un tipologia di richiesta personal");
        }

        List<EventsPersonal> eventsPersonalList = requestPersonal.getEvents();
        if (!eventsPersonalList.isEmpty()) {
            if (eventsPersonalList.size() > 1) {
                throw new RepositoryManagerException.RequestMalformedException("Inserire un solo evento personal");

            }
            checkEventsPersonal(requestPersonal, eventsPersonalList.get(0));
        }
    }

    private void checkEventsPersonal(RequestPersonal requestPersonal, EventsPersonal eventsPersonal) {
        boolean isDigital = requestPersonal.getDigitalReq() != null;
        if ((isDigital && eventsPersonal.getPaperProgrStatus() != null)) {
            throw new RepositoryManagerException.RequestMalformedException("Tipo richiesta personal e tipo evento personal non compatibili");
        }
    }

    public RequestPersonalServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient) {
        this.requestPersonalDynamoDbTable = dynamoDbEnhancedClient.table(REQUEST_PERSONAL_TABLE_NAME, TableSchema.fromBean(RequestPersonal.class));
    }

    @Override
    public Mono<RequestPersonal> getRequestPersonal(String requestIdx) {
        return Mono.fromCompletionStage(requestPersonalDynamoDbTable.getItem(getKey(requestIdx)))
                .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
                .doOnError(RepositoryManagerException.RequestNotFoundException.class, throwable -> log.info(throwable.getMessage()));
    }

    @Override
    public Mono<RequestPersonal> insertRequestPersonal(RequestPersonal requestPersonal) {
        return Mono.fromCompletionStage(requestPersonalDynamoDbTable.getItem(getKey(requestPersonal.getRequestId())))
                .handle((foundedRequest, sink) -> {
                    if (foundedRequest != null) {
                        sink.error(new RepositoryManagerException.IdRequestAlreadyPresent(requestPersonal.getRequestId()));
                    }
                })
                .doOnError(RepositoryManagerException.IdRequestAlreadyPresent.class, throwable -> log.info(throwable.getMessage()))
                .doOnSuccess(unused -> checkRequestPersonalToInsert(requestPersonal))
                .doOnError(RepositoryManagerException.RequestMalformedException.class, throwable -> log.info(throwable.getMessage()))
                .doOnSuccess(unused -> requestPersonalDynamoDbTable.putItem(builder -> builder.item(requestPersonal)))
                .thenReturn(requestPersonal);
    }

    @Override
    public Mono<RequestPersonal> updateEventsPersonal(String requestIdx, EventsPersonal eventsPersonal) {
        return Mono.fromCompletionStage(requestPersonalDynamoDbTable.getItem(getKey(requestIdx)))
                .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
                .doOnError(RepositoryManagerException.RequestNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                .doOnSuccess(retrievedRequest -> checkEventsPersonal(retrievedRequest, eventsPersonal))
                .doOnError(RepositoryManagerException.RequestMalformedException.class, throwable -> log.info(throwable.getMessage()))
                .map(retrieveRequest -> {
                    retrieveRequest.getEvents().add(eventsPersonal);
                    requestPersonalDynamoDbTable.updateItem(retrieveRequest);
                    return retrieveRequest;
                });
    }

    @Override
    public Mono<RequestPersonal> deleteRequestPersonal(String requestIdx) {
        return Mono.fromCompletionStage(requestPersonalDynamoDbTable.getItem(getKey(requestIdx)))
                .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
                .doOnError(RepositoryManagerException.RequestNotFoundException.class, throwable -> log.info(throwable.getMessage()))
                .doOnSuccess(requestToDelete -> requestPersonalDynamoDbTable.deleteItem(getKey(requestIdx)))
                .map(requestPersonal -> requestPersonal);
    }
}
