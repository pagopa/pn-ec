package it.pagopa.pn.library.pec.service.impl;

import it.pagopa.pn.library.pec.configurationproperties.ArubaServiceProperties;
import it.pagopa.pn.library.pec.exception.ArubaCallException;
import it.pagopa.pn.library.pec.exception.ArubaCallMaxRetriesExceededException;
import it.pagopa.pn.library.pec.model.pojo.ArubaSecretValue;
import it.pagopa.pn.library.pec.service.ArubaService;
import it.pec.bridgews.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.retry.Retry;

import java.time.Duration;


@Component
@Slf4j
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
    public Mono<GetMessageCountResponse> getMessagesCount(GetMessageCount getMessageCount) {
        getMessageCount.setUser(arubaSecretValue.getPecUsername());
        getMessageCount.setPass(arubaSecretValue.getPecPassword());
        return Mono.create(sink -> pecImapBridgeClient.getMessageCountAsync(getMessageCount, res -> {
            try {
                var result = res.get();
                checkErrors(result.getErrcode(), result.getErrstr());
                sink.success(result);
            } catch (Exception e) {
                endSoapRequest(sink, e);
            }
        })).cast(GetMessageCountResponse.class).retryWhen(getArubaCallRetryStrategy());
    }

    @Override
    public Mono<DeleteMailResponse> deleteMail(DeleteMail deleteMail) {
        deleteMail.setUser(arubaSecretValue.getPecUsername());
        deleteMail.setPass(arubaSecretValue.getPecPassword());
        return Mono.create(sink -> pecImapBridgeClient.deleteMailAsync(deleteMail, res -> {
            try {
                var result = res.get();
                checkErrors(result.getErrcode(), result.getErrstr());
                sink.success(result);
            } catch (Exception e) {
                endSoapRequest(sink, e);
            }
        })).cast(DeleteMailResponse.class).retryWhen(getArubaCallRetryStrategy());
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
