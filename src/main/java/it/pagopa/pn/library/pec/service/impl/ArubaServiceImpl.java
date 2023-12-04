package it.pagopa.pn.library.pec.service.impl;

import it.pagopa.pn.ec.commons.exception.aruba.ArubaSendException;
import it.pagopa.pn.library.pec.configurationproperties.ArubaServiceProperties;
import it.pagopa.pn.library.pec.exception.aruba.ArubaCallException;
import it.pagopa.pn.library.pec.exception.aruba.ArubaCallMaxRetriesExceededException;
import it.pagopa.pn.library.pec.model.pojo.ArubaSecretValue;
import it.pagopa.pn.library.pec.service.ArubaService;
import it.pec.bridgews.*;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.retry.Retry;
import java.time.Duration;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;


@Component
@CustomLog
public class ArubaServiceImpl implements ArubaService {


    private final PecImapBridge pecImapBridgeClient;

    private final ArubaSecretValue arubaSecretValue;

    private final ArubaServiceProperties arubaServiceProperties;

    @Autowired
    public ArubaServiceImpl(PecImapBridge pecImapBridgeClient, ArubaSecretValue arubaSecretValue, ArubaServiceProperties arubaServiceProperties) {
        this.pecImapBridgeClient = pecImapBridgeClient;
        this.arubaSecretValue = arubaSecretValue;
        this.arubaServiceProperties = arubaServiceProperties;
    }

    private Retry getArubaCallRetryStrategy() {
        return Retry.backoff(Long.parseLong(arubaServiceProperties.maxAttempts()), Duration.ofSeconds(Long.parseLong(arubaServiceProperties.minBackoff())))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    throw new ArubaCallMaxRetriesExceededException();
                });
    }

    @Override
    public Mono<GetMessageCountResponse> getMessageCount(GetMessageCount getMessageCount) {
        getMessageCount.setUser(arubaSecretValue.getPecUsername());
        getMessageCount.setPass(arubaSecretValue.getPecPassword());
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, ARUBA_GET_MESSAGE_COUNT, getMessageCount);
        return Mono.create(sink -> pecImapBridgeClient.getMessageCountAsync(getMessageCount, res -> {
                    try {
                        var result = res.get();
                        checkErrors(result.getErrcode(), result.getErrstr());
                        sink.success(result);
                    } catch (Exception e) {
                        endSoapRequest(sink, e);
                    }
                })).cast(GetMessageCountResponse.class).retryWhen(getArubaCallRetryStrategy())
                .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_GET_MESSAGE_COUNT, result));
    }

    @Override
    public Mono<DeleteMailResponse> deleteMail(DeleteMail deleteMail) {
        deleteMail.setUser(arubaSecretValue.getPecUsername());
        deleteMail.setPass(arubaSecretValue.getPecPassword());
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, ARUBA_DELETE_MAIL, deleteMail);
        return Mono.create(sink -> pecImapBridgeClient.deleteMailAsync(deleteMail, res -> {
                    try {
                        var result = res.get();
                        checkErrors(result.getErrcode(), result.getErrstr());
                        sink.success(result);
                    } catch (Exception e) {
                        endSoapRequest(sink, e);
                    }
                })).cast(DeleteMailResponse.class).retryWhen(getArubaCallRetryStrategy())
                .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_DELETE_MAIL, result));
    }

    @Override
    public Mono<SendMailResponse> sendMail(SendMail sendMail) {
        sendMail.setUser(arubaSecretValue.getPecUsername());
        sendMail.setPass(arubaSecretValue.getPecPassword());
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, ARUBA_SEND_MAIL, sendMail);
        return Mono.create(sink -> pecImapBridgeClient.sendMailAsync(sendMail, outputFuture -> {
            try {
                var result = outputFuture.get();
                checkErrors(result.getErrcode(), result.getErrstr());
                sink.success(result);
            } catch (Exception throwable) {
                endSoapRequest(sink, throwable);
            }
        })).cast(SendMailResponse.class).retryWhen(getArubaCallRetryStrategy()).doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_SEND_MAIL, result));
    }

    @Override
    public Mono<GetMessagesResponse> getMessages(GetMessages getMessages) {
        getMessages.setUser(arubaSecretValue.getPecUsername());
        getMessages.setPass(arubaSecretValue.getPecPassword());
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, ARUBA_GET_MESSAGES, getMessages);
        return Mono.create(sink -> pecImapBridgeClient.getMessagesAsync(getMessages, outputFuture -> {
                    try {
                        var result = outputFuture.get();
                        checkErrors(result.getErrcode(), result.getErrstr());
                        sink.success(result);
                    } catch (Exception throwable) {
                        endSoapRequest(sink, throwable);
                    }
                })).cast(GetMessagesResponse.class).retryWhen(getArubaCallRetryStrategy())
                .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_GET_MESSAGES, result));
    }


    @Override
    public Mono<GetMessageIDResponse> getMessageId(GetMessageID getMessageID) {
        getMessageID.setUser(arubaSecretValue.getPecUsername());
        getMessageID.setPass(arubaSecretValue.getPecPassword());
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, ARUBA_GET_MESSAGE_ID, getMessageID);
        return Mono.create(sink -> pecImapBridgeClient.getMessageIDAsync(getMessageID, outputFuture -> {
                    try {
                        var result = outputFuture.get();
                        checkErrors(result.getErrcode(), result.getErrstr());
                        sink.success(result);
                    } catch (Exception throwable) {
                        endSoapRequest(sink, throwable);
                    }
                })).cast(GetMessageIDResponse.class).retryWhen(getArubaCallRetryStrategy())
                .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_GET_MESSAGE_ID, result));
    }

    private void checkErrors(Integer errorCode, String errorStr) {
        if (!errorCode.equals(0))
            throw new ArubaCallException(errorStr);
    }

    private void endSoapRequest(MonoSink<Object> sink, Throwable throwable) {
        log.error(throwable.getMessage());
        sink.error(throwable);
        Thread.currentThread().interrupt();
    }

}
