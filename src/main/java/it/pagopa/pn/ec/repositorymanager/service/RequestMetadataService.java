package it.pagopa.pn.ec.repositorymanager.service;

import it.pagopa.pn.ec.repositorymanager.model.entity.Events;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import reactor.core.publisher.Mono;

public interface RequestMetadataService {

    Mono<RequestMetadata> getRequestMetadata(String requestId);
    Mono<RequestMetadata> insertRequestMetadata(RequestMetadata requestMetadata);
    Mono<RequestMetadata> updateEventsMetadata(String requestId, Events events);
    Mono<RequestMetadata> deleteRequestMetadata(String requestId);
    Mono<RequestMetadata> getRequestMetadataByMessageId(String requestId);
    Mono<RequestMetadata> setMessageIdInRequestMetadata(String requestId);
}
