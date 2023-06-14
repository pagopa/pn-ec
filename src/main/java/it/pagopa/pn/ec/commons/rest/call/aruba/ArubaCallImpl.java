package it.pagopa.pn.ec.commons.rest.call.aruba;

import it.pagopa.pn.ec.commons.exception.aruba.ArubaCallException;
import it.pagopa.pn.ec.pec.model.pojo.ArubaSecretValue;
import it.pec.bridgews.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

@Component
@Slf4j
public class ArubaCallImpl implements ArubaCall {

    private final PecImapBridge pecImapBridge;
    private final ArubaSecretValue arubaSecretValue;

    public ArubaCallImpl(PecImapBridge pecImapBridge, ArubaSecretValue arubaSecretValue) {
        this.pecImapBridge = pecImapBridge;
        this.arubaSecretValue = arubaSecretValue;
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
        })).cast(GetMessagesResponse.class).retryWhen(ARUBA_CALL_RETRY_STRATEGY);
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
        })).cast(GetMessageIDResponse.class).retryWhen(ARUBA_CALL_RETRY_STRATEGY);
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
        })).cast(SendMailResponse.class).retryWhen(ARUBA_CALL_RETRY_STRATEGY);
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
        })).cast(GetAttachResponse.class).retryWhen(ARUBA_CALL_RETRY_STRATEGY);
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
