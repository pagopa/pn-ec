package it.pagopa.pn.ec.notificationtracker.service;

import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.notificationtracker.rest.NotificationTrackerController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class NotificationTrackerMessageReceiver {

    private final NotificationTrackerController controller;
    private final NotificationTrackerSqsName notificationTrackerSqsName;

    public NotificationTrackerMessageReceiver(NotificationTrackerController controller,
                                              NotificationTrackerSqsName notificationTrackerSqsName) {
        this.controller = controller;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-sms-name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void receiveSMSObjectMessage(final NotificationTrackerQueueDto message) {
        log.info("START PULL FROM  -> {}", notificationTrackerSqsName.statoSmsName());
        log.info("BODY  -> {}", message);

        controller.getStatoSmS(message).subscribe();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-email-name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void receiveEmailObjectMessage(final NotificationTrackerQueueDto message) {
        log.info("START PULL FROM  -> {}", notificationTrackerSqsName.statoEmailName());
        log.info("Pull -> {}", message);
        controller.getEmailStatus(message).subscribe();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-pec-name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void receivePecObjectMessage(final NotificationTrackerQueueDto message) {
        log.info("START PULL FROM  -> {}", notificationTrackerSqsName.statoPecName());
        controller.getPecStatus(message).subscribe();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-cartaceo-name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void receiveCartaceoObjectMessage(final NotificationTrackerQueueDto message) {
        log.info("START PULL FROM  -> {}", notificationTrackerSqsName.statoCartaceoName());
        controller.getCartaceoStatus(message).subscribe();
    }
}
