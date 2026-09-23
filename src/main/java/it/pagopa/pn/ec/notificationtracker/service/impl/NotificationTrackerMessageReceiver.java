package it.pagopa.pn.ec.notificationtracker.service.impl;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
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
    private final PnEcConfig.NotificationTracker.SqsQueue notificationTrackerSqsName;
    private final PnEcConfig.Commons.TransactionProcess transactionProcessProperties;

    public NotificationTrackerMessageReceiver(NotificationTrackerService notificationTrackerService,
                                              PnEcConfig pnEcConfig) {
        this.notificationTrackerService = notificationTrackerService;
        this.notificationTrackerSqsName = pnEcConfig.getNotificationTracker().getSqsQueue();
        this.transactionProcessProperties = pnEcConfig.getCommons().getTransactionProcess();
    }

    @SqsListener(value = "${pn.ec.notification-tracker.sqs-queue.stato-sms-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receiveSMSObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_SMS);
        logIncomingMessage(notificationTrackerSqsName.getStatoSmsName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                                             transactionProcessProperties.getSms(),
                                                             notificationTrackerSqsName.getStatoSmsName(),
                                                             notificationTrackerSqsName.getStatoSmsErratoName(),
                                                             acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_SMS))
                .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_SMS, false, throwable.getMessage(), throwable)))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${pn.ec.notification-tracker.sqs-queue.stato-sms-errato-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receiveSMSObjectFromErrorQueue(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_SMS_ERROR);
        logIncomingMessage(notificationTrackerSqsName.getStatoSmsErratoName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleMessageFromErrorQueue(notificationTrackerQueueDto, notificationTrackerSqsName.getStatoSmsName(), acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_SMS_ERROR))
                .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_SMS_ERROR, false, throwable.getMessage(), throwable)))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${pn.ec.notification-tracker.sqs-queue.stato-email-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receiveEmailObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_EMAIL);
        logIncomingMessage(notificationTrackerSqsName.getStatoEmailName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                                             transactionProcessProperties.getEmail(),
                                                             notificationTrackerSqsName.getStatoEmailName(),
                                                             notificationTrackerSqsName.getStatoEmailErratoName(),
                                                             acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_EMAIL))
                .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_EMAIL, false, throwable.getMessage(), throwable)))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${pn.ec.notification-tracker.sqs-queue.stato-email-errato-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receiveEmailObjectFromErrorQueue(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_EMAIL_ERROR);
        logIncomingMessage(notificationTrackerSqsName.getStatoEmailErratoName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleMessageFromErrorQueue(notificationTrackerQueueDto, notificationTrackerSqsName.getStatoEmailName(), acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_EMAIL_ERROR))
                .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_EMAIL_ERROR, false, throwable.getMessage(), throwable)))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${pn.ec.notification-tracker.sqs-queue.stato-pec-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receivePecObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_PEC);
        logIncomingMessage(notificationTrackerSqsName.getStatoPecName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                                             transactionProcessProperties.getPec(),
                                                             notificationTrackerSqsName.getStatoPecName(),
                                                             notificationTrackerSqsName.getStatoPecErratoName(),
                                                             acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_PEC))
                .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_PEC, false, throwable.getMessage(), throwable)))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${pn.ec.notification-tracker.sqs-queue.stato-pec-errato-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receivePecObjectFromErrorQueue(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_PEC_ERROR);
        logIncomingMessage(notificationTrackerSqsName.getStatoPecErratoName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleMessageFromErrorQueue(notificationTrackerQueueDto, notificationTrackerSqsName.getStatoPecName(), acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_PEC_ERROR))
                .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_PEC_ERROR, false, throwable.getMessage(), throwable)))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${pn.ec.notification-tracker.sqs-queue.stato-cartaceo-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receiveCartaceoObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_CARTACEO);
        logIncomingMessage(notificationTrackerSqsName.getStatoCartaceoName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                                             transactionProcessProperties.getPaper(),
                                                             notificationTrackerSqsName.getStatoCartaceoName(),
                                                             notificationTrackerSqsName.getStatoCartaceoErratoName(),
                                                             acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_CARTACEO))
                .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_CARTACEO, false, throwable.getMessage(), throwable)))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${pn.ec.notification-tracker.sqs-queue.stato-cartaceo-errato-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receiveCartaceoObjectFromErrorQueue(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_CARTACEO_ERROR);
        logIncomingMessage(notificationTrackerSqsName.getStatoCartaceoErratoName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleMessageFromErrorQueue(notificationTrackerQueueDto, notificationTrackerSqsName.getStatoCartaceoName(), acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_CARTACEO_ERROR))
                .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_CARTACEO_ERROR, false, throwable.getMessage(), throwable)))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${pn.ec.notification-tracker.sqs-queue.stato-sercq-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receiveSercqObjectMessage(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_SERCQ);
        logIncomingMessage(notificationTrackerSqsName.getStatoSercqName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleRequestStatusChange(notificationTrackerQueueDto,
                                transactionProcessProperties.getSercq(),
                                notificationTrackerSqsName.getStatoSercqName(),
                                notificationTrackerSqsName.getStatoSercqErratoName(),
                                acknowledgment)
                        .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_SERCQ))
                        .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_SERCQ, false, throwable.getMessage(), throwable)))
                .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                    log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                    return Mono.empty();
                })
                .block();
    }

    @SqsListener(value = "${pn.ec.notification-tracker.sqs-queue.stato-sercq-errato-name}", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
    public void receiveSercqObjectFromErrorQueue(final NotificationTrackerQueueDto notificationTrackerQueueDto, Acknowledgement acknowledgment) {
        String concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(NT_RECEIVE_SERCQ_ERROR);
        logIncomingMessage(notificationTrackerSqsName.getStatoSercqErratoName(), notificationTrackerQueueDto);
        MDCUtils.addMDCToContextAndExecute(notificationTrackerService.handleMessageFromErrorQueue(notificationTrackerQueueDto, notificationTrackerSqsName.getStatoSercqName(), acknowledgment)
                        .doOnSuccess(result -> log.logEndingProcess(NT_RECEIVE_SERCQ_ERROR))
                        .doOnError(throwable -> log.logEndingProcess(NT_RECEIVE_SERCQ_ERROR, false, throwable.getMessage(), throwable)))
                        .onErrorResume(SqsMaxTimeElapsedException.class, ex -> {
                            log.info("Message skipped caused by max retry time elapsed: {}", concatRequestId);
                            return Mono.empty();
                        })
                        .block();
    }
}
