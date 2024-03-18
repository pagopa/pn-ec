package it.pagopa.pn.library.pec.service.impl;

import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.service.AlternativeProviderService;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@Component
@CustomLog
public class AlternativeProviderServiceImpl implements AlternativeProviderService {

    @Override
    public Mono<String> sendMail(byte[] message) {
        log.debug(CLIENT_METHOD_INVOCATION, ALT_SEND_MAIL);
        return Mono.just("alt-provider-message-id")
                .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ALT_SEND_MAIL, result));
    }

    @Override
    public Mono<PnGetMessagesResponse> getUnreadMessages(int limit) {
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, ALT_GET_UNREAD_MESSAGES, limit);
        return Mono.just(new PnGetMessagesResponse())
                .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ALT_GET_UNREAD_MESSAGES, result));
    }

    @Override
    public Mono<Void> markMessageAsRead(String messageID) {
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, ALT_MARK_MESSAGE_AS_READ, messageID);
        return Mono.empty();
    }

    @Override
    public Mono<Integer> getMessageCount() {
        log.debug(CLIENT_METHOD_INVOCATION, ALT_GET_MESSAGE_COUNT);
        return Mono.just(0)
                .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ALT_GET_MESSAGE_COUNT, result));
    }

    @Override
    public Mono<Void> deleteMessage(String messageID) {
        log.debug(CLIENT_METHOD_INVOCATION, ALT_DELETE_MAIL);
        return Mono.empty();
    }


}