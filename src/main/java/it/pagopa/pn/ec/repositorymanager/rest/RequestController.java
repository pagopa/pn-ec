package it.pagopa.pn.ec.repositorymanager.rest;

import it.pagopa.pn.ec.commons.utils.RestUtils;
import it.pagopa.pn.ec.repositorymanager.model.entity.Events;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Request;
import it.pagopa.pn.ec.repositorymanager.service.RequestService;
import it.pagopa.pn.ec.rest.v1.api.GestoreRequestApi;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.OK;

@RestController
@Slf4j
public class RequestController implements GestoreRequestApi {

    private final RequestService requestService;
    private final RestUtils restUtils;

    public RequestController(RequestService requestService, RestUtils restUtils) {
        this.requestService = requestService;
        this.restUtils = restUtils;
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> getRequest(String requestIdx, ServerWebExchange exchange) {
        log.info("Try to retrieve request with id -> {}", requestIdx);
        return requestService.getRequest(requestIdx).map(retrievedClient -> restUtils.endReadRequest(retrievedClient, RequestDto.class));
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> insertRequest(Mono<RequestDto> requestDto, ServerWebExchange exchange) {
        return requestDto.map(requestDtoToInsert -> restUtils.startCreateRequest(requestDtoToInsert, Request.class))
                         .flatMap(requestService::insertRequest)
                         .map(insertedRequest -> restUtils.endCreateOrUpdateRequest(insertedRequest, RequestDto.class));
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> updateEvents(String requestIdx, Mono<EventsDto> eventsPatchDto, ServerWebExchange exchange) {
        return eventsPatchDto.map(eventsToUpdate -> restUtils.startUpdateRequest(eventsToUpdate, Events.class))
                             .flatMap(requestToUpdate -> requestService.updateEvents(requestIdx, requestToUpdate))
                             .map(updatedRequest -> restUtils.endCreateOrUpdateRequest(updatedRequest, RequestDto.class));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteRequest(String requestIdx, ServerWebExchange exchange) {
        log.info("Try to delete request with id -> {}", requestIdx);
        return requestService.deleteRequest(requestIdx)
                             .map(retrievedRequest -> restUtils.endDeleteRequest(retrievedRequest, RequestDto.class))
                             .thenReturn(new ResponseEntity<>(OK));
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> getRequestByMessageId(String messageId, ServerWebExchange exchange) {
        log.info("Try to retrieve request with messageId -> {}", messageId);
        return requestService.getRequestByMessageId(messageId)
                             .map(retrievedRequest -> restUtils.endReadRequest(retrievedRequest, RequestDto.class));
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> setMessageIdInRequestMetadata(String requestIdx, ServerWebExchange exchange) {
        log.info("Try to set messageId in request with id '{}'", requestIdx);
        return requestService.setMessageIdInRequestMetadata(requestIdx)
                             .map(retrievedClient -> restUtils.endReadRequest(retrievedClient, RequestDto.class));
    }
}
