package it.pagopa.pn.ec.repositorymanager.service;

import it.pagopa.pn.ec.repositorymanager.model.entity.MessageIdRequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Patch;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Request;
import reactor.core.publisher.Mono;

public interface RequestService {

    Mono<Request> getRequest(String clientId, String requestIdx);
    Mono<Request> insertRequest(Request request);
    Mono<Request> patchRequest(String clientId, String requestIdx, Patch patch);
    Mono<Request> deleteRequest(String clientId, String requestIdx);
    Mono<Request> getRequestByMessageId(String messageId);
    Mono<Request> setMessageIdInRequestMetadata(String clientId, String requestIdx);
    Mono<Request> getRequestMetadataByMessageId(String messageId);
    Mono<Request> setRequestMetadataMessageId(String clientId, String requestIdx, MessageIdRequestMetadata messageIdToUpdate);
}
