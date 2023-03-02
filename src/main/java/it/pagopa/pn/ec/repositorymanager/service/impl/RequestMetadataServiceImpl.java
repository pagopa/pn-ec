package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.model.entity.Events;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.service.RequestMetadataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.getKey;

@Service
@Slf4j
public class RequestMetadataServiceImpl implements RequestMetadataService {

    private final DynamoDbAsyncTable<RequestMetadata> requestMetadataDynamoDbTable;
    private final CallMacchinaStati callMacchinaStati;

    public RequestMetadataServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient,
                                      RepositoryManagerDynamoTableName repositoryManagerDynamoTableName,
                                      CallMacchinaStati callMacchinaStati) {
        this.callMacchinaStati = callMacchinaStati;
        this.requestMetadataDynamoDbTable = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.richiesteMetadataName(),
                                                                         TableSchema.fromBean(RequestMetadata.class));
    }

    private void checkEventsMetadata(RequestMetadata requestMetadata, Events events) {
        boolean isDigital = requestMetadata.getDigitalRequestMetadata() != null;
        if ((isDigital && events.getPaperProgrStatus() != null) || (!isDigital && events.getDigProgrStatus() != null)) {
            throw new RepositoryManagerException.RequestMalformedException(
                    "Tipo richiesta metadata e tipo evento metadata non " + "compatibili");
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

    @Override
    public Mono<RequestMetadata> updateEventsMetadata(String requestId, Events events) {
        return getRequestMetadata(requestId).doOnSuccess(retrievedRequest -> checkEventsMetadata(retrievedRequest, events))
                                             .doOnError(RepositoryManagerException.RequestMalformedException.class,
                                                        throwable -> log.info(throwable.getMessage()))
                                             .flatMap(retrieveRequestMetadata -> {

                                                 // Id del client
                                                 String clientID = retrieveRequestMetadata.getXPagopaExtchCxId();
                                                 // Stato generale della richiesta
                                                 String generalStatus = null;
                                                 // Stato dell'evento da convertire
                                                 String statusToConvert = null;
                                                 // process ID (es. PEC, SMS ecc.)
                                                 String processID = null;

                                                 List<Events> eventsList = retrieveRequestMetadata.getEventsList();

                                                 // ---> CASO 1: RICHIESTA DIGITALE <---
                                                 if (events.getDigProgrStatus() != null) {

                                                     statusToConvert = events.getDigProgrStatus().getStatus();

                                                     if (eventsList != null) {

                                                         // Controlla se l'evento che stiamo inserendo viene temporalmente prima degli
                                                         // eventi già presenti.
                                                         // In tal caso, non aggiorna lo stato della richiesta.
                                                         for (Events eve : eventsList) {

                                                             if (events.getDigProgrStatus()
                                                                       .getEventTimestamp()
                                                                       .isBefore(eve.getDigProgrStatus().getEventTimestamp()))
                                                                 generalStatus = null;
                                                             else generalStatus = events.getDigProgrStatus().getStatus();
                                                         }
                                                     }
                                                     // Se la lista eventi è nulla, viene automaticamente aggiornato lo stato della
                                                     // richiesta.
                                                     else {
                                                         generalStatus = events.getDigProgrStatus().getStatus();
                                                         retrieveRequestMetadata.setStatusRequest(generalStatus);
                                                     }
                                                     processID = retrieveRequestMetadata.getDigitalRequestMetadata().getChannel();
                                                 }

                                                 // ---> CASO 2: RICHIESTA CARTACEO <---
                                                 else {

                                                     statusToConvert = events.getPaperProgrStatus().getStatusDescription();

                                                     if (eventsList != null) {
                                                         // Controlla se l'evento che stiamo inserendo viene temporalmente prima degli
                                                         // eventi già presenti.
                                                         // In tal caso, non aggiorna lo stato della richiesta.
                                                         for (Events eve : eventsList) {
                                                             if (events.getPaperProgrStatus()
                                                                       .getStatusDateTime()
                                                                       .isBefore(eve.getPaperProgrStatus().getStatusDateTime()))
                                                                 generalStatus = null;
                                                             else generalStatus = events.getPaperProgrStatus().getStatusDescription();
                                                         }
                                                     }
                                                     // Se la lista eventi è nulla, viene automaticamente aggiornato lo stato della
                                                     // richiesta.
                                                     else {
                                                         generalStatus = events.getPaperProgrStatus().getStatusDescription();
                                                         retrieveRequestMetadata.setStatusRequest(generalStatus);
                                                     }

                                                     processID = "PAPER";

                                                 }

                                                 if (generalStatus != null) {
                                                     retrieveRequestMetadata.setStatusRequest(generalStatus);
                                                 }

                                                 // Conversione da stato tecnico a stato logico.
                                                 return callMacchinaStati.statusDecode(processID, statusToConvert, clientID)
                                                                         .map(macchinaStatiDecodeResponseDto -> {

                                                                                  if (events.getDigProgrStatus() != null) {
                                                                                      events.getDigProgrStatus()
                                                                                            .setEventCode(macchinaStatiDecodeResponseDto.getLogicStatus());
                                                                                  } else {
                                                                                      events.getPaperProgrStatus()
                                                                                            .setStatusCode(macchinaStatiDecodeResponseDto.getLogicStatus());
                                                                                  }

                                                                                  List<Events> getEventsList =
                                                                                          retrieveRequestMetadata.getEventsList();
                                                                                  if (getEventsList == null) {
                                                                                      getEventsList = new ArrayList<>();
                                                                                  }

                                                                                  getEventsList.add(events);
                                                                                  retrieveRequestMetadata.setEventsList(getEventsList);
                                                                                  return retrieveRequestMetadata;
                                                                              }

                                                                             );

                                             })
                                             .flatMap(requestMetadataWithEventsUpdated -> Mono.fromCompletionStage(
                                                     requestMetadataDynamoDbTable.updateItem(requestMetadataWithEventsUpdated)));
    }

    @Override
    public Mono<RequestMetadata> deleteRequestMetadata(String requestId) {
        return getRequestMetadata(requestId).flatMap(requestToDelete -> Mono.fromCompletionStage(requestMetadataDynamoDbTable.deleteItem(
                getKey(requestId))));
    }

    @Override
    public Mono<RequestMetadata> getRequestMetadataByMessageId(String messageId) {
        return Flux.from(requestMetadataDynamoDbTable.index("messageId")
                                                     .query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(messageId))))
                   .flatMap(page -> Flux.fromIterable(page.items()))
                   .singleOrEmpty()
                   .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestByMessageIdNotFoundException(messageId)))
                   .doOnError(RepositoryManagerException.RequestByMessageIdNotFoundException.class,
                              throwable -> log.info(throwable.getMessage()));
    }

    @Override
    public Mono<RequestMetadata> updateMessageIdInRequestMetadata(String requestId, String messageId) {
        return getRequestMetadata(requestId).flatMap(retrievedRequestMetadata -> {
            retrievedRequestMetadata.setMessageId(messageId);
            return Mono.fromCompletionStage(requestMetadataDynamoDbTable.updateItem(retrievedRequestMetadata));
        });
    }
}
