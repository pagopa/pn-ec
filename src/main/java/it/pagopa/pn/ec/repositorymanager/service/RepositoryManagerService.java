package it.pagopa.pn.ec.repositorymanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.repositorymanager.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.repositorymanager.dto.RequestDto;
import it.pagopa.pn.ec.repositorymanager.dto.UpdatedEventDto;
import it.pagopa.pn.ec.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.model.ClientConfiguration;
import it.pagopa.pn.ec.repositorymanager.model.Events;
import it.pagopa.pn.ec.repositorymanager.model.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import static it.pagopa.pn.ec.repositorymanager.constant.DynamoTableNameConstant.ANAGRAFICA_TABLE_NAME;
import static it.pagopa.pn.ec.repositorymanager.constant.DynamoTableNameConstant.REQUEST_TABLE_NAME;

@Service
@Slf4j
public class RepositoryManagerService {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final ObjectMapper objectMapper;

    public RepositoryManagerService(DynamoDbEnhancedClient dynamoDbEnhancedClient, ObjectMapper objectMapper) {
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.objectMapper = objectMapper;
    }

    public ClientConfigurationDto insertClient(ClientConfigurationDto clientConfigurationDto) {
        try {
            DynamoDbTable<ClientConfiguration> clientConfigurationTable = dynamoDbEnhancedClient.table(ANAGRAFICA_TABLE_NAME,
                                                                                                       TableSchema.fromBean(
                                                                                                               ClientConfiguration.class));

            ClientConfiguration clientConfiguration = objectMapper.convertValue(clientConfigurationDto, ClientConfiguration.class);

            if (clientConfigurationTable.getItem(clientConfiguration) == null) {

                log.info("New client added to the table -> {}", clientConfiguration);
                clientConfigurationTable.putItem(clientConfiguration);

                return objectMapper.convertValue(clientConfiguration, ClientConfigurationDto.class);
            } else {
                log.info("Client id already exists");
                throw new RepositoryManagerException.IdClientAlreadyPresent(clientConfigurationDto.getCxId());
            }
        } catch (DynamoDbException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryManagerException.DynamoDbException();
        }
    }

    public ClientConfigurationDto getClient(String idClient) {
        try {

            DynamoDbTable<ClientConfiguration> clientConfigurationTable = dynamoDbEnhancedClient.table(ANAGRAFICA_TABLE_NAME,
                                                                                                       TableSchema.fromBean(
                                                                                                               ClientConfiguration.class));

            ClientConfiguration clientConfiguration = clientConfigurationTable.getItem(Key.builder().partitionValue(idClient).build());

            if (clientConfiguration == null) {
                log.info("Client id doesn't exists");
                throw new RepositoryManagerException.IdClientNotFoundException(idClient);
            }

            log.info("Client record -> {}", clientConfiguration);
            return objectMapper.convertValue(clientConfiguration, ClientConfigurationDto.class);
        } catch (DynamoDbException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryManagerException.DynamoDbException();
        }
    }

    public ClientConfigurationDto updateClient(String idClient, ClientConfigurationDto clientConfigurationDto) {

        try {

            DynamoDbTable<ClientConfiguration> clientConfigurationTable = dynamoDbEnhancedClient.table(ANAGRAFICA_TABLE_NAME,
                                                                                                        TableSchema.fromBean(
                                                                                                                ClientConfiguration.class));
            ClientConfiguration clientConfiguration = clientConfigurationTable.getItem(Key.builder().partitionValue(idClient).build());

            if (clientConfiguration == null) {
                log.info("Client id doesn't exists");
                throw new RepositoryManagerException.IdClientNotFoundException(idClient);
            }

            log.info("Client deleted from the table -> {}", clientConfiguration);
            clientConfigurationTable.deleteItem(clientConfiguration);

            ClientConfiguration clientConfigurationNew = objectMapper.convertValue(clientConfigurationDto, ClientConfiguration.class);
            clientConfigurationNew.setCxId(idClient);

            if(clientConfigurationTable.getItem(clientConfigurationNew) == null) {

                log.info("Client updated -> {}", clientConfigurationNew);
                clientConfigurationTable.putItem(clientConfigurationNew);

                return objectMapper.convertValue(clientConfigurationNew, ClientConfigurationDto.class);
            } else {
                log.info("Client id already exists");
                throw new RepositoryManagerException.IdClientAlreadyPresent(idClient);
            }

        } catch (DynamoDbException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryManagerException.DynamoDbException();
        }

    }

    public void deleteClient(String idClient) {

        try {

            DynamoDbTable<ClientConfiguration> clientConfigurationTable = dynamoDbEnhancedClient.table(ANAGRAFICA_TABLE_NAME,
                                                                                               TableSchema.fromBean(ClientConfiguration.class));

            ClientConfiguration clientConfiguration = clientConfigurationTable.getItem(Key.builder().partitionValue(idClient).build());

            if (clientConfiguration == null) {
                log.info("Client id can't be deleted because it doesn't exists");
                throw new RepositoryManagerException.IdClientNotFoundException(idClient);
            }

            log.info("Client deleted from the table -> {}", clientConfiguration);
            clientConfigurationTable.deleteItem(clientConfiguration);

        } catch (DynamoDbException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryManagerException.DynamoDbException();
        }
    }

    public RequestDto insertRequest(RequestDto requestDto) {

        if(requestDto.getEvents().get(0).getDigProgrStatus().getCode() == null || requestDto.getEvents().get(0).getDigProgrStatus().getCode().equals("")) {
            requestDto.setStatusRequest(requestDto.getEvents().get(0).getPaperProgrStatus().getStatusCode());
        } else {
            requestDto.setStatusRequest(requestDto.getEvents().get(0).getDigProgrStatus().getCode());
        }

        try {
            DynamoDbTable<Request> requestTable = dynamoDbEnhancedClient.table(REQUEST_TABLE_NAME,
                    TableSchema.fromBean(
                            Request.class));

            Request request = objectMapper.convertValue(requestDto, Request.class);

            if (requestTable.getItem(request) == null) {

                log.info("New request added to the table -> {}", request);
                requestTable.putItem(request);

                return objectMapper.convertValue(request, RequestDto.class);
            } else {
                log.info("Request id already exists");
                throw new RepositoryManagerException.IdClientAlreadyPresent(requestDto.getRequestId());
            }
        } catch (DynamoDbException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryManagerException.DynamoDbException();
        }
    }

    public RequestDto getRequest(String requestId) {
        try {

            DynamoDbTable<Request> requestTable = dynamoDbEnhancedClient.table(REQUEST_TABLE_NAME,
                    TableSchema.fromBean(
                            Request.class));

            Request request = requestTable.getItem(Key.builder().partitionValue(requestId).build());

            if (request == null) {
                log.info("Request id doesn't exists");
                throw new RepositoryManagerException.IdClientNotFoundException(requestId);
            }

            log.info("Request -> {}", request);
            return objectMapper.convertValue(request, RequestDto.class);
        } catch (DynamoDbException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryManagerException.DynamoDbException();
        }
    }

    public RequestDto updateRequest(String requestId, UpdatedEventDto updateEventDto) {

        try {

            DynamoDbTable<Request> requestTable = dynamoDbEnhancedClient.table(REQUEST_TABLE_NAME,
                    TableSchema.fromBean(
                            Request.class));
            Request request = requestTable.getItem(Key.builder().partitionValue(requestId).build());

            Events event = objectMapper.convertValue(updateEventDto, Events.class);

            if (request == null) {
                log.info("Request id doesn't exists");
                throw new RepositoryManagerException.IdClientNotFoundException(requestId);
            }

            if(updateEventDto.getDigProgrStatus().getStatus() == null || updateEventDto.getDigProgrStatus().getStatus().equals("")) {
                request.setStatusRequest(updateEventDto.getPaperProgrStatus().getStatusCode());
            } else {
                request.setStatusRequest(updateEventDto.getDigProgrStatus().getStatus());
            }

            request.getEvents().add(event);
            log.info("new Event added into Request -> {}", request);
            requestTable.updateItem(request);

            return objectMapper.convertValue(request, RequestDto.class);

        } catch (DynamoDbException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryManagerException.DynamoDbException();
        }
    }

    public void deleteRequest(String requestId) {

        try {

            DynamoDbTable<Request> requestTable = dynamoDbEnhancedClient.table(REQUEST_TABLE_NAME,
                    TableSchema.fromBean(Request.class));

            Request request = requestTable.getItem(Key.builder().partitionValue(requestId).build());

            if (request == null) {
                log.info("Request id can't be deleted because it doesn't exists");
                throw new RepositoryManagerException.IdClientNotFoundException(requestId);
            }

            log.info("Request deleted from the table -> {}", request);
            requestTable.deleteItem(request);

        } catch (DynamoDbException e) {
            log.error(e.getMessage(), e);
            throw new RepositoryManagerException.DynamoDbException();
        }

    }

}
