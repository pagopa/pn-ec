package it.pagopa.pn.ec.commons.rest.call.aruba;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.commons.exception.aruba.ArubaCallException;
import it.pagopa.pn.ec.commons.exception.aruba.ArubaCallMaxRetriesExceededException;
import it.pagopa.pn.ec.commons.exception.aruba.GetMessageIdException;
import it.pagopa.pn.ec.commons.exception.aruba.GetMessagesException;
import it.pagopa.pn.ec.pec.model.pojo.ArubaSecretValue;
import it.pec.bridgews.*;
import lombok.CustomLog;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@Component
@CustomLog
public class ArubaCallImpl implements ArubaCall {

    private final PecImapBridge pecImapBridge;
    private final ArubaSecretValue arubaSecretValue;
	private final ArubaCallProperties arubaCallProperties;

	@Autowired
    public ArubaCallImpl(PecImapBridge pecImapBridge, ArubaSecretValue arubaSecretValue, ArubaCallProperties arubaCallProperties) {
        this.pecImapBridge = pecImapBridge;
        this.arubaSecretValue = arubaSecretValue;
        this.arubaCallProperties = arubaCallProperties;
    }

    private RetryBackoffSpec getArubaCallRetryStrategy(String clientMethodName) {
        var mdcContextMap = MDCUtils.retrieveMDCContextMap();
        return Retry.backoff(Long.parseLong(arubaCallProperties.maxAttempts()), Duration.ofSeconds(Long.parseLong(arubaCallProperties.minBackoff())))
                .doBeforeRetry(retrySignal -> {
                    MDCUtils.enrichWithMDC(retrySignal, mdcContextMap);
                    log.debug("Retry number {} for '{}', caused by : {}", retrySignal.totalRetries(), clientMethodName, retrySignal.failure().getMessage(), retrySignal.failure());
                });
    }

    @Override
    public Mono<GetMessagesResponse> getMessages(GetMessages getMessages) {
        getMessages.setUser(arubaSecretValue.getPecUsername());
        getMessages.setPass(arubaSecretValue.getPecPassword());
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, ARUBA_GET_MESSAGES, getMessages);
        return Mono.create(sink -> pecImapBridge.getMessagesAsync(getMessages, outputFuture -> {
            try {
                var result = outputFuture.get();
                checkErrors(result.getErrcode(), result.getErrstr());
                sink.success(result);
            } catch (Exception throwable) {
                endSoapRequest(sink, throwable, ARUBA_GET_MESSAGES);
            }
        })).cast(GetMessagesResponse.class).retryWhen(getArubaCallRetryStrategy(ARUBA_GET_MESSAGES)
               .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    throw new GetMessagesException();
            }))
           .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_GET_MESSAGES, result));
    }

    @Override
    public Mono<GetMessageIDResponse> getMessageId(GetMessageID getMessageID) {
        getMessageID.setUser(arubaSecretValue.getPecUsername());
        getMessageID.setPass(arubaSecretValue.getPecPassword());
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, ARUBA_GET_MESSAGE_ID, getMessageID);
        return Mono.create(sink -> pecImapBridge.getMessageIDAsync(getMessageID, outputFuture -> {
            try {
                var result = outputFuture.get();
                checkErrors(result.getErrcode(), result.getErrstr());
                sink.success(result);
            } catch (Exception throwable) {
                endSoapRequest(sink, throwable, ARUBA_GET_MESSAGE_ID);
            }
        })).cast(GetMessageIDResponse.class).retryWhen(getArubaCallRetryStrategy(ARUBA_GET_MESSAGE_ID).onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
              throw new GetMessageIdException(getMessageID.getMailid());
        }))
           .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_GET_MESSAGE_ID, result));
    }

    @Override
    public Mono<SendMailResponse> sendMail(SendMail sendMail) {
        sendMail.setUser(arubaSecretValue.getPecUsername());
        sendMail.setPass(arubaSecretValue.getPecPassword());
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, ARUBA_SEND_MAIL, sendMail);
        var mdcContextMap = MDCUtils.retrieveMDCContextMap();
        return Mono.create(sink -> pecImapBridge.sendMailAsync(sendMail, outputFuture -> {
            try {
                var result = outputFuture.get();
                checkErrors(result.getErrcode(), result.getErrstr());
                sink.success(result);
            } catch (Exception throwable) {
                MDCUtils.enrichWithMDC(throwable, mdcContextMap);
                endSoapRequest(sink, throwable, ARUBA_SEND_MAIL);
            }
        })).cast(SendMailResponse.class).retryWhen(getArubaCallRetryStrategy(ARUBA_SEND_MAIL).onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
            throw new ArubaCallMaxRetriesExceededException();
        })).doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_SEND_MAIL, result));
    }

    @Override
    public Mono<GetAttachResponse> getAttach(GetAttach getAttach) {
        getAttach.setUser(arubaSecretValue.getPecUsername());
        getAttach.setPass(arubaSecretValue.getPecPassword());
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, ARUBA_GET_ATTACH, getAttach);
        return Mono.create(sink -> pecImapBridge.getAttachAsync(getAttach, outputFuture -> {
            try {
                var result = outputFuture.get();
                checkErrors(result.getErrcode(), result.getErrstr());
                sink.success(result);
            } catch (Exception throwable) {
                endSoapRequest(sink, throwable, ARUBA_GET_ATTACH);
            }
        })).cast(GetAttachResponse.class).retryWhen(getArubaCallRetryStrategy(ARUBA_GET_ATTACH))
           .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_GET_ATTACH, result));
    }

    private void checkErrors(Integer errorCode, String errorStr) {
        if (!errorCode.equals(0))
            throw new ArubaCallException(errorStr);
    }

    private void endSoapRequest(MonoSink<Object> sink, Throwable throwable, String methodName) {
        log.error(EXCEPTION_IN_PROCESS, methodName, throwable, throwable.getMessage());
        sink.error(throwable);
        Thread.currentThread().interrupt();
    }

}
