package it.pagopa.pn.ec.repositorymanager.service;

import it.pagopa.pn.ec.repositorymanager.entity.EventsMetadata;
import it.pagopa.pn.ec.repositorymanager.entity.RequestMetadata;
import reactor.core.publisher.Mono;

public interface RequestMetadataService {

    Mono<RequestMetadata> getRequestMetadata(String requestId);
    Mono<RequestMetadata> insertRequestMetadata(RequestMetadata requestMetadata);
    Mono<RequestMetadata> updateEventsMetadata(String requestId, EventsMetadata eventsMetadata);
    Mono<RequestMetadata> deleteRequestMetadata(String requestId);
}
