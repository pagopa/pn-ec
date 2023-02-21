package it.pagopa.pn.ec.repositorymanager.service;

import it.pagopa.pn.ec.repositorymanager.model.entity.Events;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import reactor.core.publisher.Mono;

public interface RequestMetadataService {

    Mono<RequestMetadata> getRequestMetadata(String xPagopaExtchCxId, String requestId);
    Mono<RequestMetadata> insertRequestMetadata(RequestMetadata requestMetadata);
    Mono<RequestMetadata> updateEventsMetadata(String xPagopaExtchCxId, String requestId, Events events);
    Mono<RequestMetadata> deleteRequestMetadata(String xPagopaExtchCxId, String requestId);
}
