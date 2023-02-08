package it.pagopa.pn.ec.repositorymanager.rest;

import it.pagopa.pn.ec.commons.utils.RestUtils;
import it.pagopa.pn.ec.repositorymanager.entity.Events;
import it.pagopa.pn.ec.repositorymanager.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.entity.RequestPersonal;
import it.pagopa.pn.ec.repositorymanager.service.RequestMetadataService;
import it.pagopa.pn.ec.repositorymanager.service.RequestPersonalService;
import it.pagopa.pn.ec.repositorymanager.service.RequestService;
import it.pagopa.pn.ec.rest.v1.api.GestoreRequestApi;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.http.HttpStatus.OK;

@RestController
@Slf4j
public class RequestController implements GestoreRequestApi {

    private final RequestService requestService;
    private final RequestMetadataService requestMetadataService;
    private final RequestPersonalService requestPersonalService;
    private final RestUtils restUtils;

    public RequestController(RequestService requestService, RequestMetadataService requestMetadataService, RestUtils restUtils,
                             RequestPersonalService requestPersonalService) {
        this.requestService = requestService;
        this.requestMetadataService = requestMetadataService;
        this.requestPersonalService = requestPersonalService;
        this.restUtils = restUtils;
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> getRequest(String requestIdx, ServerWebExchange exchange) {
        log.info("Try to retrieve request with id -> {}", requestIdx);
        return requestService.getRequest(requestIdx).map(retrievedClient -> restUtils.endReadRequest(retrievedClient, RequestDto.class));
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> insertRequest(Mono<RequestDto> requestDto, ServerWebExchange exchange) {

        AtomicReference<RequestDto> requestDtoAtomicReference = new AtomicReference<>();

        return requestDto.doOnNext(requestDto1 -> {
            requestDtoAtomicReference.set(requestDto1);
        }).map(requestDtoToInsert -> {
            requestDtoAtomicReference.set(requestDtoToInsert);
            return restUtils.startCreateRequest(requestDtoToInsert, RequestMetadata.class);
        }).flatMap(requestMetadataService::insertRequestMetadata).flatMap(requestMetadata -> {
            RequestPersonal requestPersonal = restUtils.startCreateRequest(requestDtoAtomicReference.get(), RequestPersonal.class);
            return requestPersonalService.insertRequestPersonal(requestPersonal);
        }).map(insertedRequest -> restUtils.endCreateOrUpdateRequest(insertedRequest, RequestDto.class));
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> updateRequest(String requestIdx, Mono<EventsDto> eventsPatchDto, ServerWebExchange exchange) {
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
}
