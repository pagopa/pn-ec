package it.pagopa.pn.ec.notificationtracker.service.impl;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.notificationtracker.service.NotificationTrackerService;
import org.springframework.stereotype.Service;

import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;


@Service
public class NotificationTrackerMessageReceiver {

    private final NotificationTrackerService notificationTrackerService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;

    public NotificationTrackerMessageReceiver(NotificationTrackerService notificationTrackerService,
                                              NotificationTrackerSqsName notificationTrackerSqsName,
                                              TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
        this.notificationTrackerService = notificationTrackerService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-sms-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void receiveSMSObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgment acknowledgment) {
        logIncomingMessage(notificationTrackerSqsName.statoSmsName(), notificationTrackerQueueDto);
        notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                                             transactionProcessConfigurationProperties.sms(),
                                                             notificationTrackerSqsName.statoSmsName(),
                                                             notificationTrackerSqsName.statoSmsErratoName(),
                                                             acknowledgment).subscribe();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-sms-errato-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void receiveSMSObjectFromErrorQueue(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgment acknowledgment) {
        logIncomingMessage(notificationTrackerSqsName.statoSmsErratoName(), notificationTrackerQueueDto);
        notificationTrackerService.handleMessageFromErrorQueue(notificationTrackerQueueDto, notificationTrackerSqsName.statoSmsName(), acknowledgment).subscribe();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-email-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void receiveEmailObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgment acknowledgment) {
        logIncomingMessage(notificationTrackerSqsName.statoEmailName(), notificationTrackerQueueDto);
        notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                                             transactionProcessConfigurationProperties.email(),
                                                             notificationTrackerSqsName.statoEmailName(),
                                                             notificationTrackerSqsName.statoEmailErratoName(),
                                                             acknowledgment).subscribe();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-email-errato-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void receiveEmailObjectFromErrorQueue(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgment acknowledgment) {
        logIncomingMessage(notificationTrackerSqsName.statoEmailErratoName(), notificationTrackerQueueDto);
        notificationTrackerService.handleMessageFromErrorQueue(notificationTrackerQueueDto, notificationTrackerSqsName.statoEmailName(), acknowledgment).subscribe();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-pec-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void receivePecObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgment acknowledgment) {
        logIncomingMessage(notificationTrackerSqsName.statoPecName(), notificationTrackerQueueDto);
        notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                                             transactionProcessConfigurationProperties.pec(),
                                                             notificationTrackerSqsName.statoPecName(),
                                                             notificationTrackerSqsName.statoPecErratoName(),
                                                             acknowledgment).subscribe();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-pec-errato-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void receivePecObjectFromErrorQueue(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgment acknowledgment) {
        logIncomingMessage(notificationTrackerSqsName.statoPecErratoName(), notificationTrackerQueueDto);
        notificationTrackerService.handleMessageFromErrorQueue(notificationTrackerQueueDto, notificationTrackerSqsName.statoPecName(), acknowledgment).subscribe();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-cartaceo-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void receiveCartaceoObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgment acknowledgment) {
        logIncomingMessage(notificationTrackerSqsName.statoCartaceoName(), notificationTrackerQueueDto);
        notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                                             transactionProcessConfigurationProperties.paper(),
                                                             notificationTrackerSqsName.statoCartaceoName(),
                                                             notificationTrackerSqsName.statoCartaceoErratoName(),
                                                             acknowledgment).subscribe();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-cartaceo-errato-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void receiveCartaceoObjectFromErrorQueue(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgment acknowledgment) {
        logIncomingMessage(notificationTrackerSqsName.statoCartaceoErratoName(), notificationTrackerQueueDto);
        notificationTrackerService.handleMessageFromErrorQueue(notificationTrackerQueueDto, notificationTrackerSqsName.statoCartaceoName(), acknowledgment).subscribe();
    }

}
