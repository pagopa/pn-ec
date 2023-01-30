package it.pagopa.pn.ec.repositorymanager.service;

import it.pagopa.pn.ec.repositorymanager.entity.Events;
import it.pagopa.pn.ec.repositorymanager.entity.Request;
import reactor.core.publisher.Mono;

public interface RequestService {

    Mono<Request> getRequest(String requestId);
    Mono<Request> insertRequest(Request request);
    Mono<Request> updateEvents(String requestId, Events events);
    Mono<Request> deleteRequest(String requestId);
}
