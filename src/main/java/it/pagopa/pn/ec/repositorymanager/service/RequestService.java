package it.pagopa.pn.ec.repositorymanager.service;

import it.pagopa.pn.ec.repositorymanager.model.entity.Events;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Request;
import reactor.core.publisher.Mono;

public interface RequestService {

    Mono<Request> getRequest(String xPagopaExtchCxId, String requestIdx);
    Mono<Request> insertRequest(String xPagopaExtchCxId, Request request);
    Mono<Request> updateEvents(String xPagopaExtchCxId, String requestIdx, Events events);
    Mono<Request> deleteRequest(String xPagopaExtchCxId, String requestIdx);
}
