package it.pagopa.pn.ec.notificationtracker.service;

import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

public interface PutEvents {

    Mono<PutEventsResponse> putEventExternal(NotificationTrackerQueueDto notificationTrackerQueueDto);
}
