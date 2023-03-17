package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.commons.model.pojo.request.RequestStatusChange;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
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
    private final CallMacchinaStati callMacchinaStati;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;

    public RequestMetadataServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient,
                                      RepositoryManagerDynamoTableName repositoryManagerDynamoTableName,
                                      CallMacchinaStati callMacchinaStati,
                                      TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
        this.callMacchinaStati = callMacchinaStati;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
        this.requestMetadataDynamoDbTable = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.richiesteMetadataName(),
                TableSchema.fromBean(RequestMetadata.class));
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

    @Override
    public Mono<RequestMetadata> patchRequestMetadata(String requestId, Patch patch) {
        return getRequestMetadata(requestId).doOnSuccess(retrievedRequest -> checkTipoPatchMetadata(retrievedRequest, patch))
                .doOnError(RepositoryManagerException.RequestMalformedException.class,
                        throwable -> log.info(throwable.getMessage()))
                .flatMap(retrieveRequestMetadata -> {

                    // Id del client
                    String clientID = retrieveRequestMetadata.getXPagopaExtchCxId();
                    // Stato generale della richiesta
                    String generalStatus = null;
                    // Stato dell'evento da convertire
                    String statusToConvert;
                    // process ID (es. PEC, SMS ecc.)
                    String processID;

                    // lista eventi metadata
                    List<Events> eventsList = retrieveRequestMetadata.getEventsList();

                    // retry metadata
                    Retry retry = retrieveRequestMetadata.getRetry();

                    // ---> CASO: RETRY <---
                    if (patch.getRetry() != null) {

                        if (retry == null) {
                            retry = new Retry();
                        }

                        retry.setLastRetryTimestamp(patch.getRetry().getLastRetryTimestamp());
                        retry.setRetryStep(patch.getRetry().getRetryStep());
                        retry.setRetryPolicy(patch.getRetry().getRetryPolicy());

                        retrieveRequestMetadata.setRetry(retry);
                        return Mono.just(retrieveRequestMetadata);

                    } else {

                        // ---> CASO 1: RICHIESTA DIGITALE <---
                        if (patch.getEvent().getDigProgrStatus() != null) {

                            statusToConvert = patch.getEvent().getDigProgrStatus().getStatus();

                            if (eventsList != null) {

                                // Controlla se l'evento che stiamo inserendo viene temporalmente prima degli
                                // eventi già presenti.
                                // In tal caso, non aggiorna lo stato della richiesta.
                                for (Events eve : eventsList) {

                                    if (eve.getDigProgrStatus().getEventTimestamp().isAfter(patch.getEvent()
                                            .getDigProgrStatus()
                                            .getEventTimestamp())) {
                                        generalStatus = null;
                                        break;
                                    } else if (eve.getDigProgrStatus().getEventTimestamp().isBefore(patch.getEvent()
                                            .getDigProgrStatus()
                                            .getEventTimestamp()))
                                        generalStatus = patch.getEvent().getDigProgrStatus().getStatus();
                                }
                            }
                            // Se la lista eventi è nulla, viene automaticamente aggiornato lo stato della
                            // richiesta.
                            else {
                                generalStatus = patch.getEvent().getDigProgrStatus().getStatus();
                                retrieveRequestMetadata.setStatusRequest(generalStatus);
                            }
                            processID = retrieveRequestMetadata.getDigitalRequestMetadata().getChannel();
                        }

                        // ---> CASO 2: RICHIESTA CARTACEO <---
                        else {

                            statusToConvert = patch.getEvent().getPaperProgrStatus().getStatusDescription();

                            if (eventsList != null) {
                                // Controlla se l'evento che stiamo inserendo viene temporalmente prima degli
                                // eventi già presenti.
                                // In tal caso, non aggiorna lo stato della richiesta.
                                for (Events eve : eventsList) {

                                    if (eve.getPaperProgrStatus().getStatusDateTime().isAfter(patch.getEvent()
                                            .getPaperProgrStatus()
                                            .getStatusDateTime())) {
                                        generalStatus = null;
                                        break;
                                    } else if (eve.getPaperProgrStatus().getStatusDateTime().isBefore(patch.getEvent()
                                            .getPaperProgrStatus()
                                            .getStatusDateTime()))
                                        generalStatus = patch.getEvent().getPaperProgrStatus().getStatusDescription();
                                }
                            }
                            // Se la lista eventi è nulla, viene automaticamente aggiornato lo stato della
                            // richiesta.
                            else {
                                generalStatus = patch.getEvent().getPaperProgrStatus().getStatusDescription();
                                retrieveRequestMetadata.setStatusRequest(generalStatus);
                            }

                            processID = transactionProcessConfigurationProperties.paper();

                        }

                        if (generalStatus != null) {
                            retrieveRequestMetadata.setStatusRequest(generalStatus);
                        }

                        // Conversione da stato tecnico a stato logico.
                        return callMacchinaStati.statusDecode(RequestStatusChange.builder()
                                        .processId(processID)
                                        .currentStatus(statusToConvert)
                                        .xPagopaExtchCxId(clientID)
                                        .build())
                                .map(macchinaStatiDecodeResponseDto -> {

                                            if (patch.getEvent().getDigProgrStatus() != null) {
                                                patch.getEvent()
                                                        .getDigProgrStatus()
                                                        .setEventCode(macchinaStatiDecodeResponseDto.getLogicStatus());
                                            } else {
                                                patch.getEvent()
                                                        .getPaperProgrStatus()
                                                        .setStatusCode(macchinaStatiDecodeResponseDto.getLogicStatus());
                                            }

                                            List<Events> getEventsList =
                                                    retrieveRequestMetadata.getEventsList();
                                            if (getEventsList == null) {
                                                getEventsList = new ArrayList<>();
                                            }

                                            getEventsList.add(patch.getEvent());
                                            retrieveRequestMetadata.setEventsList(getEventsList);
                                            return retrieveRequestMetadata;
                                        }

                                );
                    }
                })
                .flatMap(requestMetadataWithPatchUpdated -> Mono.fromCompletionStage(
                        requestMetadataDynamoDbTable.updateItem(requestMetadataWithPatchUpdated)))
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
        });
    }
}
