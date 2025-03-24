package it.pagopa.pn.ec.notificationtracker.service;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

public interface PutEvents {

    <T>Mono<PutEventsResponse> putEventExternal(T objectToNotify, String processId);
}
