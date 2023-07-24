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

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.LogUtils.ENDING_PROCESS_ON_WITH_ERROR_LABEL;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;
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
        String id = concatRequestId(clientId, requestIdx);
        log.info(STARTING_PROCESS_ON_LABEL, SEND_PAPER_ENGAGE_REQUEST, id);
        return requestService.getRequest(clientId, requestIdx).map(retrievedClient -> restUtils.endReadRequest(retrievedClient, RequestDto.class))
                .doOnSuccess(result -> log.info(ENDING_PROCESS_ON_LABEL, GET_REQUEST, id))
                .doOnError(throwable -> log.warn(ENDING_PROCESS_ON_WITH_ERROR_LABEL, GET_REQUEST, id, throwable, throwable.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> insertRequest(Mono<RequestDto> requestDto, ServerWebExchange exchange) {
        log.info(STARTING_PROCESS_LABEL, INSERT_REQUEST);
        return requestDto.map(requestDtoToInsert -> restUtils.startCreateRequest(requestDtoToInsert, Request.class))
                         .flatMap(requestService::insertRequest)
                         .map(insertedRequest -> restUtils.endCreateOrUpdateRequest(insertedRequest, RequestDto.class))
                .doOnSuccess(result -> log.info(ENDING_PROCESS_LABEL, INSERT_REQUEST))
                .doOnError(throwable -> log.warn(ENDING_PROCESS_WITH_ERROR_LABEL, INSERT_REQUEST, throwable, throwable.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> patchRequest(String clientId, String requestIdx, Mono<PatchDto> patchDto, ServerWebExchange exchange) {
        String id = concatRequestId(clientId, requestIdx);
        log.info(STARTING_PROCESS_ON_LABEL, PATCH_REQUEST, id);
        return patchDto.map(patchToUpdate -> restUtils.startUpdateRequest(patchToUpdate, Patch.class))
                .flatMap(requestToUpdate -> requestService.patchRequest(clientId, requestIdx, requestToUpdate))
                .map(updatedRequest -> restUtils.endCreateOrUpdateRequest(updatedRequest, RequestDto.class))
                .doOnSuccess(result -> log.info(ENDING_PROCESS_ON_LABEL, PATCH_REQUEST, id))
                .doOnError(throwable -> log.warn(ENDING_PROCESS_ON_WITH_ERROR_LABEL, PATCH_REQUEST, id, throwable, throwable.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteRequest(String clientId, String requestIdx, ServerWebExchange exchange) {
        String id = concatRequestId(clientId, requestIdx);
        log.info(STARTING_PROCESS_ON_LABEL, DELETE_REQUEST, id);
        return requestService.deleteRequest(clientId, requestIdx)
                .map(retrievedRequest -> restUtils.endDeleteRequest(retrievedRequest, RequestDto.class))
                .doOnSuccess(result -> log.info(ENDING_PROCESS_ON_LABEL, DELETE_REQUEST, clientId + SEPARATORE + requestIdx))
                .doOnError(throwable -> log.warn(ENDING_PROCESS_ON_WITH_ERROR_LABEL, DELETE_REQUEST, id, throwable, throwable.getMessage()))
                .thenReturn(new ResponseEntity<>(OK));
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> getRequestByMessageId(String messageId, ServerWebExchange exchange) {
        log.info(STARTING_PROCESS_ON_LABEL, GET_REQUEST_BY_MESSAGE_ID, messageId);
        return requestService.getRequestByMessageId(messageId)
                             .map(retrievedRequest -> restUtils.endReadRequest(retrievedRequest, RequestDto.class))
                .doOnSuccess(result -> log.info(ENDING_PROCESS_ON_LABEL, GET_REQUEST_BY_MESSAGE_ID, messageId))
                .doOnError(throwable -> log.warn(ENDING_PROCESS_ON_WITH_ERROR_LABEL, GET_REQUEST_BY_MESSAGE_ID, messageId, throwable, throwable.getMessage()));
    }

    @Override
    public Mono<ResponseEntity<RequestDto>> setMessageIdInRequestMetadata(String clientId, String requestIdx, ServerWebExchange exchange) {
        String id = concatRequestId(clientId, requestIdx);
        log.info(STARTING_PROCESS_ON_LABEL, SET_MESSAGE_ID_IN_REQUEST_METADATA, id);
        return requestService.setMessageIdInRequestMetadata(clientId, requestIdx)
                .map(retrievedClient -> restUtils.endReadRequest(retrievedClient, RequestDto.class))
                .doOnSuccess(result -> log.info(ENDING_PROCESS_ON_LABEL, SET_MESSAGE_ID_IN_REQUEST_METADATA, id))
                .doOnError(throwable -> log.warn(ENDING_PROCESS_ON_WITH_ERROR_LABEL, SET_MESSAGE_ID_IN_REQUEST_METADATA, id, throwable, throwable.getMessage()));
    }

}
