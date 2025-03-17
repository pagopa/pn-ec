package it.pagopa.pn.ec.repositorymanager.service;

import it.pagopa.pn.ec.repositorymanager.model.entity.DiscardedEvent;
import reactor.core.publisher.Mono;

public interface DiscardedEventsService {

    Mono<DiscardedEvent> insertDiscardedEvent(DiscardedEvent discardedEvent);

}
