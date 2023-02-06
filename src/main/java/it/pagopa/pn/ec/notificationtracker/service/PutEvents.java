package it.pagopa.pn.ec.notificationtracker.service;

import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;

import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutionException;

public interface PutEvents {

    Mono<Void> putEventExternal(NotificationTrackerQueueDto eventInfo) throws ExecutionException, InterruptedException;


}
