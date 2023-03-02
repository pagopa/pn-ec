package it.pagopa.pn.ec.commons.rest.call.aruba;

import it.pec.bridgews.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

@Component
@Slf4j
public class ArubaCallImpl implements ArubaCall {

    private final PecImapBridge pecImapBridge;

    public ArubaCallImpl(PecImapBridge pecImapBridge) {
        this.pecImapBridge = pecImapBridge;
    }

    @Override
    public Mono<GetMessagesResponse> getMessages(GetMessages getMessages) {
        return Mono.create(sink -> pecImapBridge.getMessagesAsync(getMessages, outputFuture -> {
            try {
                sink.success(outputFuture.get());
            } catch (Exception throwable) {
                endSoapRequest(sink, throwable);
            }
        })).cast(GetMessagesResponse.class);
    }

    @Override
    public Mono<GetMessageIDResponse> getMessageId(GetMessageID getMessageID) {
        return Mono.create(sink -> pecImapBridge.getMessageIDAsync(getMessageID, outputFuture -> {
            try {
                sink.success(outputFuture.get());
            } catch (Exception throwable) {
                endSoapRequest(sink, throwable);
            }
        })).cast(GetMessageIDResponse.class);
    }

    @Override
    public Mono<SendMailResponse> sendMail(SendMail sendMail) {
        return Mono.create(sink -> pecImapBridge.sendMailAsync(sendMail, outputFuture -> {
            try {
                sink.success(outputFuture.get());
            } catch (Exception throwable) {
                endSoapRequest(sink, throwable);
            }
        })).cast(SendMailResponse.class);
    }

    @Override
    public Mono<GetAttachResponse> getAttach(GetAttach getAttach) {
        return Mono.create(sink -> pecImapBridge.getAttachAsync(getAttach, outputFuture -> {
            try {
                sink.success(outputFuture.get());
            } catch (Exception throwable) {
                endSoapRequest(sink, throwable);
            }
        })).cast(GetAttachResponse.class);
    }

    private void endSoapRequest(MonoSink<Object> sink, Throwable throwable) {
        sink.error(throwable);
        Thread.currentThread().interrupt();
    }
}
