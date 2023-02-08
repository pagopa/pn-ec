package it.pagopa.pn.ec.repositorymanager.service;

import it.pagopa.pn.ec.repositorymanager.entity.Events;
import it.pagopa.pn.ec.repositorymanager.entity.Request;
import reactor.core.publisher.Mono;

public interface RequestService {

    Mono<Request> getRequest(String requestIdx);
    Mono<Request> insertRequest(Request request);
    Mono<Request> updateEvents(String requestIdx, Events events);
    Mono<Request> deleteRequest(String requestIdx);
}
