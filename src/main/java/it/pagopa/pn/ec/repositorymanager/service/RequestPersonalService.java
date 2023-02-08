package it.pagopa.pn.ec.repositorymanager.service;

import it.pagopa.pn.ec.repositorymanager.entity.EventsPersonal;
import it.pagopa.pn.ec.repositorymanager.entity.RequestPersonal;
import reactor.core.publisher.Mono;

public interface RequestPersonalService {

    Mono<RequestPersonal> getRequestPersonal(String requestIdx);
    Mono<RequestPersonal> insertRequestPersonal(RequestPersonal requestPersonal);
    Mono<RequestPersonal> updateEventsPersonal(String requestIdx, EventsPersonal eventsPersonal);
    Mono<RequestPersonal> deleteRequestPersonal(String requestIdx);
}
