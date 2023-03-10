package it.pagopa.pn.ec.repositorymanager.service;

import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Patch;
import reactor.core.publisher.Mono;

public interface RequestMetadataService {

    Mono<RequestMetadata> getRequestMetadata(String requestId);
    Mono<RequestMetadata> insertRequestMetadata(RequestMetadata requestMetadata);
    Mono<RequestMetadata> patchRequestMetadata(String requestId, Patch patch);
    Mono<RequestMetadata> deleteRequestMetadata(String requestId);
    Mono<RequestMetadata> getRequestMetadataByMessageId(String requestId);
    Mono<RequestMetadata> setMessageIdInRequestMetadata(String requestId);
}
