package it.pagopa.pn.library.pec.service.impl;

import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.service.AlternativeProviderService;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@CustomLog
public class AlternativeProviderServiceImpl implements AlternativeProviderService {

    @Override
    public Mono<String> sendMail(byte[] message) {
        return Mono.just("");
    }

    @Override
    public Mono<PnGetMessagesResponse> getUnreadMessages(int limit) {
        return Mono.just(new PnGetMessagesResponse());
    }

    @Override
    public Mono<Void> markMessageAsRead(String messageID) { return Mono.empty(); }

    @Override
    public Mono<Integer> getMessageCount() {
        return Mono.just(0);
    }

    @Override
    public Mono<Void> deleteMessage(String messageID) {
        return Mono.empty();
    }


}