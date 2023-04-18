package it.pagopa.pn.ec.repositorymanager.service;

import it.pagopa.pn.ec.repositorymanager.model.entity.RequestPersonal;
import reactor.core.publisher.Mono;

public interface RequestPersonalService {

    Mono<RequestPersonal> getRequestPersonal(String concatRequestId);
    Mono<RequestPersonal> insertRequestPersonal(RequestPersonal requestPersonal);
    Mono<RequestPersonal> deleteRequestPersonal(String concatRequestId);
}
