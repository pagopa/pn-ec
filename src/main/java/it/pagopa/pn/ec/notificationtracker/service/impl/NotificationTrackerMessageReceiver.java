package it.pagopa.pn.ec.notificationtracker.service.impl;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.sqs.SqsMaxTimeElapsedException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.notificationtracker.service.NotificationTrackerService;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;


@Service
@CustomLog
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

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-sms-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receiveSMSObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_SMS);
        logIncomingMessage(notificationTrackerSqsName.statoSmsName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                                             transactionProcessConfigurationProperties.sms(),
                                                             notificationTrackerSqsName.statoSmsName(),
                                                             notificationTrackerSqsName.statoSmsErratoName(),
                                                             acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_SMS))
                .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_SMS, false, throwable.getMessage())))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-sms-errato-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receiveSMSObjectFromErrorQueue(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_SMS_ERROR);
        logIncomingMessage(notificationTrackerSqsName.statoSmsErratoName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleMessageFromErrorQueue(notificationTrackerQueueDto, notificationTrackerSqsName.statoSmsName(), acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_SMS_ERROR))
                .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_SMS_ERROR, false, throwable.getMessage())))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-email-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receiveEmailObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_EMAIL);
        logIncomingMessage(notificationTrackerSqsName.statoEmailName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                                             transactionProcessConfigurationProperties.email(),
                                                             notificationTrackerSqsName.statoEmailName(),
                                                             notificationTrackerSqsName.statoEmailErratoName(),
                                                             acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_EMAIL))
                .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_EMAIL, false, throwable.getMessage())))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-email-errato-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receiveEmailObjectFromErrorQueue(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_EMAIL_ERROR);
        logIncomingMessage(notificationTrackerSqsName.statoEmailErratoName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleMessageFromErrorQueue(notificationTrackerQueueDto, notificationTrackerSqsName.statoEmailName(), acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_EMAIL_ERROR))
                .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_EMAIL_ERROR, false, throwable.getMessage())))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-pec-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receivePecObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_PEC);
        logIncomingMessage(notificationTrackerSqsName.statoPecName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                                             transactionProcessConfigurationProperties.pec(),
                                                             notificationTrackerSqsName.statoPecName(),
                                                             notificationTrackerSqsName.statoPecErratoName(),
                                                             acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_PEC))
                .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_PEC, false, throwable.getMessage())))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-pec-errato-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receivePecObjectFromErrorQueue(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_PEC_ERROR);
        logIncomingMessage(notificationTrackerSqsName.statoPecErratoName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleMessageFromErrorQueue(notificationTrackerQueueDto, notificationTrackerSqsName.statoPecName(), acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_PEC_ERROR))
                .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_PEC_ERROR, false, throwable.getMessage())))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-cartaceo-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receiveCartaceoObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_CARTACEO);
        logIncomingMessage(notificationTrackerSqsName.statoCartaceoName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                                             transactionProcessConfigurationProperties.paper(),
                                                             notificationTrackerSqsName.statoCartaceoName(),
                                                             notificationTrackerSqsName.statoCartaceoErratoName(),
                                                             acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_CARTACEO))
                .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_CARTACEO, false, throwable.getMessage())))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-cartaceo-errato-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receiveCartaceoObjectFromErrorQueue(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_CARTACEO_ERROR);
        logIncomingMessage(notificationTrackerSqsName.statoCartaceoErratoName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleMessageFromErrorQueue(notificationTrackerQueueDto, notificationTrackerSqsName.statoCartaceoName(), acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_CARTACEO_ERROR))
                .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_CARTACEO_ERROR, false, throwable.getMessage())))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-sercq-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receiveSercqObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_SERCQ);
        logIncomingMessage(notificationTrackerSqsName.statoSercqName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                transactionProcessConfigurationProperties.sercq(),
                                notificationTrackerSqsName.statoSercqName(),
                                notificationTrackerSqsName.statoSercqErratoName(),
                                acknowledgment)
                        .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_SERCQ))
                        .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_SERCQ, false, throwable.getMessage())))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${sqs.queue.notification-tracker.stato-sercq-errato-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receiveSercqObjectFromErrorQueue(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_SERCQ_ERROR);
        logIncomingMessage(notificationTrackerSqsName.statoSercqErratoName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleMessageFromErrorQueue(notificationTrackerQueueDto, notificationTrackerSqsName.statoSercqName(), acknowledgment)
                        .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_SERCQ_ERROR))
                        .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_SERCQ_ERROR, false, throwable.getMessage())))
                        .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                            log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                            return Mono.empty();
                        })
                        .block();
    }
}
