package it.pagopa.pn.ec.commons.rest.call.aruba;

import it.pagopa.pn.ec.commons.exception.MaxRetriesExceededException;
import it.pagopa.pn.ec.commons.exception.aruba.ArubaCallException;
import it.pagopa.pn.ec.pec.model.pojo.ArubaSecretValue;
import it.pec.bridgews.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.retry.Retry;

@Component
@Slf4j
public class ArubaCallImpl implements ArubaCall {

    private final PecImapBridge pecImapBridge;
    private final ArubaSecretValue arubaSecretValue;
	private final ArubaCallProperties arubaCallProperties;

	@Autowired
    public ArubaCallImpl(PecImapBridge pecImapBridge, ArubaSecretValue arubaSecretValue, ArubaCallProperties arubaCallProperties) {
        this.pecImapBridge = pecImapBridge;
        this.arubaSecretValue = arubaSecretValue;
        this.arubaCallProperties = arubaCallProperties;
        log.debug("---> ARUBA CALL RETRY STRATEGY <--- MaxAttempts : {} , MinBackoff : {}", arubaCallProperties.maxAttempts(), arubaCallProperties.minBackoff());
    }

    private Retry getArubaCallRetryStrategy () {
    	return Retry.backoff(Long.parseLong(arubaCallProperties.maxAttempts()), Duration.ofSeconds(Long.parseLong(arubaCallProperties.minBackoff())))
        		.onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
            throw new MaxRetriesExceededException();
        });
    }

    @Override
    public Mono<GetMessagesResponse> getMessages(GetMessages getMessages) {
        getMessages.setUser(arubaSecretValue.getPecUsername());
        getMessages.setPass(arubaSecretValue.getPecPassword());
        log.debug("---> START GET MESSAGES FROM ARUBA <--- Unseen : {} , Outtype : {} , Limit : {}", getMessages.getUnseen(), getMessages.getOuttype(), getMessages.getLimit());
        return Mono.create(sink -> pecImapBridge.getMessagesAsync(getMessages, outputFuture -> {
            try {
                var result = outputFuture.get();
                checkErrors(result.getErrcode(), result.getErrstr());
                sink.success(result);
            } catch (Exception throwable) {
                endSoapRequest(sink, throwable);
            }
        })).cast(GetMessagesResponse.class).retryWhen(getArubaCallRetryStrategy());
    }

    @Override
    public Mono<GetMessageIDResponse> getMessageId(GetMessageID getMessageID) {
        getMessageID.setUser(arubaSecretValue.getPecUsername());
        getMessageID.setPass(arubaSecretValue.getPecPassword());
        log.debug("---> START GET MESSAGE ID FROM ARUBA <--- MailId : {} , Markseen : {}", getMessageID.getMailid(), getMessageID.getMarkseen());
        return Mono.create(sink -> pecImapBridge.getMessageIDAsync(getMessageID, outputFuture -> {
            try {
                var result = outputFuture.get();
                checkErrors(result.getErrcode(), result.getErrstr());
                sink.success(result);
            } catch (Exception throwable) {
                endSoapRequest(sink, throwable);
            }
        })).cast(GetMessageIDResponse.class).retryWhen(getArubaCallRetryStrategy());
    }

    @Override
    public Mono<SendMailResponse> sendMail(SendMail sendMail) {
        sendMail.setUser(arubaSecretValue.getPecUsername());
        sendMail.setPass(arubaSecretValue.getPecPassword());
        log.debug("---> START SEND MAIL FROM {} <---", sendMail.getUser());
        return Mono.create(sink -> pecImapBridge.sendMailAsync(sendMail, outputFuture -> {
            try {
                var result = outputFuture.get();
                checkErrors(result.getErrcode(), result.getErrstr());
                sink.success(result);
            } catch (Exception throwable) {
                endSoapRequest(sink, throwable);
            }
        })).cast(SendMailResponse.class).retryWhen(getArubaCallRetryStrategy());
    }

    @Override
    public Mono<GetAttachResponse> getAttach(GetAttach getAttach) {
        getAttach.setUser(arubaSecretValue.getPecUsername());
        getAttach.setPass(arubaSecretValue.getPecPassword());
        log.debug("---> START GET ATTACH <--- MailId : {} , Markseen : {}", getAttach.getMailid(), getAttach.getMarkseen());
        return Mono.create(sink -> pecImapBridge.getAttachAsync(getAttach, outputFuture -> {
            try {
                var result = outputFuture.get();
                checkErrors(result.getErrcode(), result.getErrstr());
                sink.success(result);
            } catch (Exception throwable) {
                endSoapRequest(sink, throwable);
            }
        })).cast(GetAttachResponse.class).retryWhen(getArubaCallRetryStrategy());
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
