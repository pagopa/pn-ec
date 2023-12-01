package it.pagopa.pn.ec.repositorymanager.rest;

import it.pagopa.pn.ec.commons.utils.RestUtils;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Patch;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Request;
import it.pagopa.pn.ec.repositorymanager.service.RequestService;
import it.pagopa.pn.ec.rest.v1.api.GestoreRequestApi;
import it.pagopa.pn.ec.rest.v1.dto.PatchDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import lombok.CustomLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;
import static org.springframework.http.HttpStatus.OK;

@RestController
@CustomLog
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
        String id = concatRequestId(clientId, requestIdx);
        log.logStartingProcess(GET_REQUEST);
        return requestService.getRequest(clientId, requestIdx).map(retrievedClient -> restUtils.endReadRequest(retrievedClient, RequestDto.class))
                .doOnSuccess(result -> log.logEndingProcess(GET_REQUEST))
                .doOnError(throwable -> log.logEndingProcess(GET_REQUEST, false, throwable.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> insertRequest(Mono<RequestDto> requestDto, ServerWebExchange exchange) {
        log.logStartingProcess(INSERT_REQUEST);
        return requestDto.map(requestDtoToInsert -> restUtils.startCreateRequest(requestDtoToInsert, Request.class))
                         .flatMap(requestService::insertRequest)
                         .map(insertedRequest -> restUtils.endCreateOrUpdateRequest(insertedRequest, RequestDto.class))
                .doOnSuccess(result -> log.logEndingProcess(INSERT_REQUEST))
                .doOnError(throwable -> log.logEndingProcess(INSERT_REQUEST, false, throwable.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> patchRequest(String clientId, String requestIdx, Mono<PatchDto> patchDto, ServerWebExchange exchange) {
        String id = concatRequestId(clientId, requestIdx);
        log.logStartingProcess(PATCH_REQUEST);
        return patchDto.map(patchToUpdate -> restUtils.startUpdateRequest(patchToUpdate, Patch.class))
                .flatMap(requestToUpdate -> requestService.patchRequest(clientId, requestIdx, requestToUpdate))
                .map(updatedRequest -> restUtils.endCreateOrUpdateRequest(updatedRequest, RequestDto.class))
                .doOnSuccess(result -> log.logEndingProcess(PATCH_REQUEST))
                .doOnError(throwable -> log.logEndingProcess(PATCH_REQUEST, false, throwable.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteRequest(String clientId, String requestIdx, ServerWebExchange exchange) {
        String id = concatRequestId(clientId, requestIdx);
        log.logStartingProcess(DELETE_REQUEST);
        return requestService.deleteRequest(clientId, requestIdx)
                .map(retrievedRequest -> restUtils.endDeleteRequest(retrievedRequest, RequestDto.class))
                .doOnSuccess(result -> log.logEndingProcess(DELETE_REQUEST))
                .doOnError(throwable -> log.logEndingProcess(DELETE_REQUEST, false, throwable.getMessage()))
                .thenReturn(new ResponseEntity<>(OK));
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> getRequestByMessageId(String messageId, ServerWebExchange exchange) {
        log.logStartingProcess(GET_REQUEST_BY_MESSAGE_ID);
        return requestService.getRequestByMessageId(messageId)
                             .map(retrievedRequest -> restUtils.endReadRequest(retrievedRequest, RequestDto.class))
                .doOnSuccess(result -> log.logEndingProcess(GET_REQUEST_BY_MESSAGE_ID))
                .doOnError(throwable -> log.logEndingProcess(GET_REQUEST_BY_MESSAGE_ID, false, throwable.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> setMessageIdInRequestMetadata(String clientId, String requestIdx, ServerWebExchange exchange) {
        String id = concatRequestId(clientId, requestIdx);
        log.logStartingProcess(SET_MESSAGE_ID_IN_REQUEST_METADATA);
        return requestService.setMessageIdInRequestMetadata(clientId, requestIdx)
                .map(retrievedClient -> restUtils.endReadRequest(retrievedClient, RequestDto.class))
                .doOnSuccess(result -> log.logEndingProcess(SET_MESSAGE_ID_IN_REQUEST_METADATA))
                .doOnError(throwable -> log.logEndingProcess(SET_MESSAGE_ID_IN_REQUEST_METADATA, false, throwable.getMessage()));
    }

}
