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
        notificationTrackerQueueDto.setProcessId(transactionProcessConfigurationProperties.sms());
        notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                                             notificationTrackerSqsName.statoSmsName(),
                                                             notificationTrackerSqsName.statoSmsErratoName(),
                                                             acknowledgment)
                                                             .subscribe();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-email-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void receiveEmailObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgment acknowledgment) {
        logIncomingMessage(notificationTrackerSqsName.statoEmailName(), notificationTrackerQueueDto);
        notificationTrackerQueueDto.setProcessId(transactionProcessConfigurationProperties.email());
        notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                                             notificationTrackerSqsName.statoEmailName(),
                                                             notificationTrackerSqsName.statoEmailErratoName(),
                                                             acknowledgment)
                                                             .subscribe();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-pec-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void receivePecObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgment acknowledgment) {
        logIncomingMessage(notificationTrackerSqsName.statoPecName(), notificationTrackerQueueDto);
        notificationTrackerQueueDto.setProcessId(transactionProcessConfigurationProperties.pec());
        notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                                             notificationTrackerSqsName.statoPecName(),
                                                             notificationTrackerSqsName.statoPecErratoName(),
                                                             acknowledgment)
                                                             .subscribe();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-cartaceo-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void receiveCartaceoObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgment acknowledgment) {
        logIncomingMessage(notificationTrackerSqsName.statoCartaceoName(), notificationTrackerQueueDto);
        notificationTrackerQueueDto.setProcessId(transactionProcessConfigurationProperties.paper());
        notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                                             notificationTrackerSqsName.statoCartaceoName(),
                                                             notificationTrackerSqsName.statoCartaceoErratoName(),
                                                             acknowledgment)
                                                             .subscribe();
    }
}
