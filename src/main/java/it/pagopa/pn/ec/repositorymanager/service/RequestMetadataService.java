package it.pagopa.pn.ec.repositorymanager.service;

import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Patch;
import reactor.core.publisher.Mono;

public interface RequestMetadataService {

    Mono<RequestMetadata> getRequestMetadata(String concatRequestId);
    Mono<RequestMetadata> insertRequestMetadata(RequestMetadata requestMetadata);
    Mono<RequestMetadata> patchRequestMetadata(String concatRequestId, Patch patch);
    Mono<RequestMetadata> deleteRequestMetadata(String concatRequestId);
    Mono<RequestMetadata> getRequestMetadataByMessageId(String concatRequestId);
    Mono<RequestMetadata> setMessageIdInRequestMetadata(String concatRequestId);
}
