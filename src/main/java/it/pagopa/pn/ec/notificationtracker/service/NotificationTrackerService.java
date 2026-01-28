package it.pagopa.pn.ec.notificationtracker.service;

import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import reactor.core.publisher.Mono;
import  io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;

public interface NotificationTrackerService {

    Mono<Void> handleRequestStatusChange(NotificationTrackerQueueDto notificationTrackerQueueDto, String processId, String ntStatoQueueName, String ntStatoDlqQueueName, Acknowledgement acknowledgment);
    Mono<Void> handleMessageFromErrorQueue(NotificationTrackerQueueDto notificationTrackerQueueDto, String ntStatoQueueName, Acknowledgement acknowledgment);
}
