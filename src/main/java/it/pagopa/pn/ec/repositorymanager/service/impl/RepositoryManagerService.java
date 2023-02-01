package it.pagopa.pn.ec.repositorymanager.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RepositoryManagerService {

//    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
//    private final ObjectMapper objectMapper;
//
//    public RepositoryManagerService(DynamoDbEnhancedClient dynamoDbEnhancedClient, ObjectMapper objectMapper) {
//        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
//        this.objectMapper = objectMapper;
//    }
//
//    public Mono<ClientConfigurationDto> insertClient(ClientConfigurationDto clientConfigurationDto) {
//        try {
//            DynamoDbTable<ClientConfiguration> clientConfigurationTable = dynamoDbEnhancedClient.table(ANAGRAFICA_TABLE_NAME,
//                                                                                                       TableSchema.fromBean(
//                                                                                                               ClientConfiguration.class));
//
//            ClientConfiguration clientConfiguration = objectMapper.convertValue(clientConfigurationDto, ClientConfiguration.class);
//
//            if (clientConfigurationTable.getItem(clientConfiguration) == null) {
//
//                log.info("New client added to the table -> {}", clientConfiguration);
//                clientConfigurationTable.putItem(clientConfiguration);
//
//                return Mono.just(objectMapper.convertValue(clientConfiguration, ClientConfigurationDto.class));
//            } else {
//                log.info("Client id already exists");
//                throw new RepositoryManagerException.IdClientAlreadyPresent(clientConfigurationDto.getxPagopaExtchCxId());
//            }
//        } catch (DynamoDbException e) {
//            log.error(e.getMessage(), e);
//            throw new RepositoryManagerException.DynamoDbException();
//        }
//    }
//
//    public ClientConfigurationDto getClient(String idClient) {
//        try {
//
//            DynamoDbTable<ClientConfiguration> clientConfigurationTable = dynamoDbEnhancedClient.table(ANAGRAFICA_TABLE_NAME,
//                                                                                                       TableSchema.fromBean(
//                                                                                                               ClientConfiguration.class));
//
//            ClientConfiguration clientConfiguration = clientConfigurationTable.getItem(Key.builder().partitionValue(idClient).build());
//
//            if (clientConfiguration == null) {
//                log.info("Client id doesn't exists");
//                throw new RepositoryManagerException.IdClientNotFoundException(idClient);
//            }
//
//            log.info("Client record -> {}", clientConfiguration);
//            return objectMapper.convertValue(clientConfiguration, ClientConfigurationDto.class);
//        } catch (DynamoDbException e) {
//            log.error(e.getMessage(), e);
//            throw new RepositoryManagerException.DynamoDbException();
//        }
//    }
//
//    public ClientConfigurationDto updateClient(String idClient, ClientConfigurationDto clientConfigurationDto) {
//
//        try {
//
//            DynamoDbTable<ClientConfiguration> clientConfigurationTable = dynamoDbEnhancedClient.table(ANAGRAFICA_TABLE_NAME,
//                                                                                                        TableSchema.fromBean(
//                                                                                                                ClientConfiguration.class));
//            ClientConfiguration clientConfiguration = clientConfigurationTable.getItem(Key.builder().partitionValue(idClient).build());
//
//            if (clientConfiguration == null) {
//                log.info("Client id doesn't exists");
//                throw new RepositoryManagerException.IdClientNotFoundException(idClient);
//            }
//
//            log.info("Client deleted from the table -> {}", clientConfiguration);
//            clientConfigurationTable.deleteItem(clientConfiguration);
//
//            ClientConfiguration clientConfigurationNew = objectMapper.convertValue(clientConfigurationDto, ClientConfiguration.class);
//            clientConfigurationNew.setCxId(idClient);
//
//            if(clientConfigurationTable.getItem(clientConfigurationNew) == null) {
//
//                log.info("Client updated -> {}", clientConfigurationNew);
//                clientConfigurationTable.putItem(clientConfigurationNew);
//
//                return objectMapper.convertValue(clientConfigurationNew, ClientConfigurationDto.class);
//            } else {
//                log.info("Client id already exists");
//                throw new RepositoryManagerException.IdClientAlreadyPresent(idClient);
//            }
//
//        } catch (DynamoDbException e) {
//            log.error(e.getMessage(), e);
//            throw new RepositoryManagerException.DynamoDbException();
//        }
//
//    }
//
//    public void deleteClient(String idClient) {
//
//        try {
//
//            DynamoDbTable<ClientConfiguration> clientConfigurationTable = dynamoDbEnhancedClient.table(ANAGRAFICA_TABLE_NAME,
//                                                                                               TableSchema.fromBean(ClientConfiguration.class));
//
//            ClientConfiguration clientConfiguration = clientConfigurationTable.getItem(Key.builder().partitionValue(idClient).build());
//
//            if (clientConfiguration == null) {
//                log.info("Client id can't be deleted because it doesn't exists");
//                throw new RepositoryManagerException.IdClientNotFoundException(idClient);
//            }
//
//            log.info("Client deleted from the table -> {}", clientConfiguration);
//            clientConfigurationTable.deleteItem(clientConfiguration);
//
//        } catch (DynamoDbException e) {
//            log.error(e.getMessage(), e);
//            throw new RepositoryManagerException.DynamoDbException();
//        }
//    }
//
//    public RequestDto insertRequest(RequestDto requestDto) {
//
//        EventsDto firstStatusEvent = requestDto.getEvents().get(0);
//        if(firstStatusEvent.getDigProgrStatus() != null) {
//            requestDto.setStatusRequest(firstStatusEvent.getDigProgrStatus().getStatus().name());
//        } else {
//            requestDto.setStatusRequest(firstStatusEvent.getPaperProgrStatus().getStatusDescription());
//        }
//
//        try {
//            DynamoDbTable<Request> requestTable = dynamoDbEnhancedClient.table(REQUEST_TABLE_NAME,
//                    TableSchema.fromBean(
//                            Request.class));
//
//            Request request = objectMapper.convertValue(requestDto, Request.class);
//
//            if (requestTable.getItem(request) == null) {
//
//                log.info("New request added to the table -> {}", request);
//                requestTable.putItem(request);
//
//                return objectMapper.convertValue(request, RequestDto.class);
//            } else {
//                log.info("Request id already exists");
//                throw new RepositoryManagerException.IdClientAlreadyPresent(requestDto.getRequestIdx());
//            }
//        } catch (DynamoDbException e) {
//            log.error(e.getMessage(), e);
//            throw new RepositoryManagerException.DynamoDbException();
//        }
//    }
//
//    public RequestDto getRequest(String requestId) {
//        try {
//
//            DynamoDbTable<Request> requestTable = dynamoDbEnhancedClient.table(REQUEST_TABLE_NAME,
//                    TableSchema.fromBean(
//                            Request.class));
//
//            Request request = requestTable.getItem(Key.builder().partitionValue(requestId).build());
//
//            if (request == null) {
//                log.info("Request id doesn't exists");
//                throw new RepositoryManagerException.IdClientNotFoundException(requestId);
//            }
//
//            log.info("Request -> {}", request);
//            return objectMapper.convertValue(request, RequestDto.class);
//        } catch (DynamoDbException e) {
//            log.error(e.getMessage(), e);
//            throw new RepositoryManagerException.DynamoDbException();
//        }
//    }
//
//    public RequestDto updateRequest(String requestId, EventsDto eventsDto) {
//
//        try {
//
//            DynamoDbTable<Request> requestTable = dynamoDbEnhancedClient.table(REQUEST_TABLE_NAME,
//                    TableSchema.fromBean(
//                            Request.class));
//            Request request = requestTable.getItem(Key.builder().partitionValue(requestId).build());
//
//            Events event = objectMapper.convertValue(eventsDto, Events.class);
//
//            if (request == null) {
//                log.info("Request id doesn't exists");
//                throw new RepositoryManagerException.IdClientNotFoundException(requestId);
//            }
//
//            if(eventsDto.getDigProgrStatus() != null) {
//                request.setStatusRequest(eventsDto.getDigProgrStatus().getStatus().name());
//            } else {
//                request.setStatusRequest(eventsDto.getPaperProgrStatus().getStatusDescription());
//            }
//
//            request.getEvents().add(event);
//            log.info("new Event added into Request -> {}", request);
//            requestTable.updateItem(request);
//
//            return objectMapper.convertValue(request, RequestDto.class);
//
//        } catch (DynamoDbException e) {
//            log.error(e.getMessage(), e);
//            throw new RepositoryManagerException.DynamoDbException();
//        }
//    }
//
//    public void deleteRequest(String requestId) {
//
//        try {
//
//            DynamoDbTable<Request> requestTable = dynamoDbEnhancedClient.table(REQUEST_TABLE_NAME,
//                    TableSchema.fromBean(Request.class));
//
//            Request request = requestTable.getItem(Key.builder().partitionValue(requestId).build());
//
//            if (request == null) {
//                log.info("Request id can't be deleted because it doesn't exists");
//                throw new RepositoryManagerException.IdClientNotFoundException(requestId);
//            }
//
//            log.info("Request deleted from the table -> {}", request);
//            requestTable.deleteItem(request);
//
//        } catch (DynamoDbException e) {
//            log.error(e.getMessage(), e);
//            throw new RepositoryManagerException.DynamoDbException();
//        }
//
//    }
//
}
