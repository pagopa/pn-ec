package it.pagopa.pn.library.pec.service;

import it.pagopa.pn.library.pec.model.pojo.PnEcPecGetMessagesResponse;
import reactor.core.publisher.Mono;

public interface PnEcPecService {
    Mono<String> sendMail(byte[] message);

    Mono<Void> markMessageAsRead(String messageID, String providerName);

    Mono<Void> deleteMessage(String messageID, String senderMessageID);

    Mono<PnEcPecGetMessagesResponse> getUnreadMessages(int limit);

    Mono<Integer> getMessageCount();
}
