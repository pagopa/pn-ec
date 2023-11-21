package it.pagopa.pn.library.pec.service;

import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import reactor.core.publisher.Mono;

public interface PnPecService {
    Mono<Void> sendMail(byte[] message);

    Mono<PnGetMessagesResponse> getUnreadMessages(int limit);

    Mono<Void> markMessageAsRead(String messageID);

    Mono<Integer> getMessageCount();

    Mono<Void> deleteMessage(String messageID);

}
