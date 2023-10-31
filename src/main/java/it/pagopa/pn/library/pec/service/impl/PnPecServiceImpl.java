package it.pagopa.pn.library.pec.service.impl;

import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.service.ArubaService;
import it.pagopa.pn.library.pec.service.PnPecService;
import it.pec.bridgews.DeleteMail;
import it.pec.bridgews.GetMessageCount;
import it.pec.bridgews.GetMessageCountResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PnPecServiceImpl implements PnPecService {

    @Autowired
    private ArubaService arubaService;

    @Override
    public Mono<Void> sendMail(byte[] message) {
        return null;
    }

    @Override
    public Mono<PnGetMessagesResponse> getUnreadMessages(int limit) {
        return null;
    }

    @Override
    public Mono<Void> markMessageAsRead(String messageID) {
        return null;
    }

    @Override
    public Mono<Integer> getMessagesCount() {
        return arubaService.getMessagesCount(new GetMessageCount())
                .map(GetMessageCountResponse::getCount);
    }

    @Override
    public Mono<Void> deleteMessage(String messageID) {
        DeleteMail deleteMail = new DeleteMail();
        deleteMail.setIsuid(2);
        deleteMail.setMailid(messageID);
        return arubaService.deleteMail(deleteMail).then();
    }
}
