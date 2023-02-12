package it.pagopa.pn.ec.notificationtracker.service;

import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import reactor.core.publisher.Mono;

public interface NotificationTrackerService {

    Mono<Void> validateSmsStatus(final NotificationTrackerQueueDto notificationTrackerQueueDto);
    Mono<Void> validateEmailStatus(final NotificationTrackerQueueDto notificationTrackerQueueDto);
    Mono<Void> validatePecStatus(final NotificationTrackerQueueDto notificationTrackerQueueDto);
    Mono<Void> validateCartaceoStatus(final NotificationTrackerQueueDto notificationTrackerQueueDto);
}
