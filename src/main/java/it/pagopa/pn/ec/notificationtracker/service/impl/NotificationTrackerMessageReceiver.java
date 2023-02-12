package it.pagopa.pn.ec.notificationtracker.service.impl;

import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.notificationtracker.service.NotificationTrackerService;
import org.springframework.stereotype.Service;

import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;


@Service
public class NotificationTrackerMessageReceiver {

    private final NotificationTrackerService notificationTrackerService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;

    public NotificationTrackerMessageReceiver(NotificationTrackerService notificationTrackerService,
                                              NotificationTrackerSqsName notificationTrackerSqsName) {
        this.notificationTrackerService = notificationTrackerService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-sms-name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void receiveSMSObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto) {
        logIncomingMessage(notificationTrackerSqsName.statoSmsName(), notificationTrackerQueueDto);
        notificationTrackerService.validateSmsStatus(notificationTrackerQueueDto).subscribe();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-email-name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void receiveEmailObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto) {
        logIncomingMessage(notificationTrackerSqsName.statoEmailName(), notificationTrackerQueueDto);
        notificationTrackerService.validateEmailStatus(notificationTrackerQueueDto).subscribe();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-pec-name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void receivePecObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto) {
        logIncomingMessage(notificationTrackerSqsName.statoPecName(), notificationTrackerQueueDto);
        notificationTrackerService.validatePecStatus(notificationTrackerQueueDto).subscribe();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-cartaceo-name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void receiveCartaceoObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto) {
        logIncomingMessage(notificationTrackerSqsName.statoPecName(), notificationTrackerQueueDto);
        notificationTrackerService.validateCartaceoStatus(notificationTrackerQueueDto).subscribe();
    }
}
