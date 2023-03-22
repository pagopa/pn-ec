package it.pagopa.pn.ec.notificationtracker.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import reactor.core.publisher.Mono;

public interface NotificationTrackerService {

    Mono<Void> handleRequestStatusChange(NotificationTrackerQueueDto notificationTrackerQueueDto, String ntStatoErratoQueueName, Acknowledgment acknowledgment);
}
