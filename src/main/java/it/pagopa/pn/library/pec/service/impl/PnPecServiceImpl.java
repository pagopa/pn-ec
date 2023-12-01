package it.pagopa.pn.library.pec.service.impl;

import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.service.ArubaService;
import it.pagopa.pn.library.pec.service.PnPecService;
import it.pec.bridgews.DeleteMail;
import it.pec.bridgews.GetMessageCount;
import it.pec.bridgews.GetMessageCountResponse;
import lombok.CustomLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@CustomLog
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
    public Mono<Integer> getMessageCount() {
        log.debug(INVOKING_OPERATION_LABEL, PEC_GET_MESSAGE_COUNT);
        return arubaService.getMessageCount(new GetMessageCount())
                .map(GetMessageCountResponse::getCount)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PEC_GET_MESSAGE_COUNT, result))
                .doOnError(throwable -> log.error(EXCEPTION_IN_PROCESS, PEC_GET_MESSAGE_COUNT, throwable, throwable.getMessage()));
    }

    @Override
    public Mono<Void> deleteMessage(String messageID) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PEC_DELETE_MESSAGE, messageID);
        DeleteMail deleteMail = new DeleteMail();
        deleteMail.setIsuid(2);
        deleteMail.setMailid(messageID);
        return arubaService.deleteMail(deleteMail)
                .then()
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_NO_RESULT_LABEL, PEC_DELETE_MESSAGE, messageID))
                .doOnError(throwable -> log.error(EXCEPTION_IN_PROCESS_FOR, PEC_DELETE_MESSAGE, messageID, throwable, throwable.getMessage()));
    }
}
