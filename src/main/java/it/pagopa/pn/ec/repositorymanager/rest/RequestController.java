package it.pagopa.pn.ec.repositorymanager.rest;

import it.pagopa.pn.ec.commons.utils.RestUtils;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Patch;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Request;
import it.pagopa.pn.ec.repositorymanager.service.RequestService;
import it.pagopa.pn.ec.rest.v1.api.GestoreRequestApi;
import it.pagopa.pn.ec.rest.v1.dto.PatchDto;
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
    private static final String SEPARATORE = "~";

    public RequestController(RequestService requestService, RestUtils restUtils) {
        this.requestService = requestService;
        this.restUtils = restUtils;
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> getRequest(String clientId, String requestIdx, ServerWebExchange exchange) {
        log.info("Try to retrieve request with id -> {}", clientId + SEPARATORE + requestIdx);
        return requestService.getRequest(clientId, requestIdx).map(retrievedClient -> restUtils.endReadRequest(retrievedClient, RequestDto.class));
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> insertRequest(Mono<RequestDto> requestDto, ServerWebExchange exchange) {
        log.info("Try to insert request");
        return requestDto.map(requestDtoToInsert -> restUtils.startCreateRequest(requestDtoToInsert, Request.class))
                         .flatMap(requestService::insertRequest)
                         .map(insertedRequest -> restUtils.endCreateOrUpdateRequest(insertedRequest, RequestDto.class));
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> patchRequest(String clientId, String requestIdx, Mono<PatchDto> patchDto, ServerWebExchange exchange) {
        log.info("Try to update request with id -> {}", clientId + SEPARATORE + requestIdx);
        return patchDto.map(patchToUpdate -> restUtils.startUpdateRequest(patchToUpdate, Patch.class))
                             .flatMap(requestToUpdate -> requestService.patchRequest(clientId, requestIdx, requestToUpdate))
                             .map(updatedRequest -> restUtils.endCreateOrUpdateRequest(updatedRequest, RequestDto.class));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteRequest(String clientId, String requestIdx, ServerWebExchange exchange) {
        log.info("Try to delete request with id -> {}", clientId + SEPARATORE + requestIdx);
        return requestService.deleteRequest(clientId, requestIdx)
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
    public Mono<ResponseEntity<RequestDto>> setMessageIdInRequestMetadata(String clientId, String requestIdx, ServerWebExchange exchange) {
        log.info("Try to set messageId in request with id '{}'", requestIdx);
        return requestService.setMessageIdInRequestMetadata(clientId, requestIdx)
                             .map(retrievedClient -> restUtils.endReadRequest(retrievedClient, RequestDto.class));
    }
}
