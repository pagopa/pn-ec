package it.pagopa.pn.library.pec.service.impl;

import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.rest.call.aruba.ArubaCall;
import it.pagopa.pn.ec.commons.utils.EmailUtils;
import it.pagopa.pn.ec.pec.model.pojo.ArubaSecretValue;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnListOfMessages;
import it.pagopa.pn.library.pec.service.ArubaService;
import it.pagopa.pn.library.pec.service.PnPecService;
import it.pec.bridgews.*;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Arrays;
import javax.mail.Address;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@CustomLog
@Service
public class PnPecServiceImpl implements PnPecService {

    @Autowired
    private ArubaService arubaService;

    @Autowired
    private ArubaCall arubaCall;

    private final ArubaSecretValue arubaSecretVaule = new ArubaSecretValue();

    @Override
    public Mono<Void> sendMail(byte[] message) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PEC_SEND_MAIL, message);
        return Mono.just(message).map(EmailUtils::getMimeMessage)
                .<EmailField>handle((mimeMessage, sink) -> {
                    try {
                        sink.next(EmailField.builder()
                                .msgId(EmailUtils.getMessageIdFromMimeMessage(mimeMessage))
                                .from(arubaSecretVaule.getPecUsername())
                                .to(Arrays.toString(Arrays.stream(mimeMessage.getRecipients(Message.RecipientType.TO)).map(Address::toString).toArray()))
                                .subject(mimeMessage.getSubject())
                                .text(mimeMessage.getContent().toString())
                                .contentType(mimeMessage.getContentType())
                                .build());
                    } catch (MessagingException | IOException e) {
                        sink.error(new RuntimeException(e));
                    }
                })
                .map(EmailUtils::getMimeMessageInCDATATag)
                .map(data -> {
                    SendMail sendMail = new SendMail();
                    sendMail.setData(data);
                    return sendMail;
                })
                .flatMap(arubaService::sendMail)
                .then()
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_NO_RESULT_LABEL, PEC_SEND_MAIL, result))
                .doOnError(throwable -> log.error(EXCEPTION_IN_PROCESS, PEC_SEND_MAIL, throwable, throwable.getMessage()));
    }

    @Override
    public Mono<PnGetMessagesResponse> getUnreadMessages(int limit) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PEC_GET_UNREAD_MESSAGES, limit);
        GetMessages getMessages = new GetMessages();
        getMessages.setUnseen(1);
        getMessages.setOuttype(2);
        getMessages.setLimit(limit);
    return arubaService.getMessages(getMessages)
            .map(GetMessagesResponse::getArrayOfMessages)
            .map(messages -> {
                PnGetMessagesResponse pnGetMessagesResponse = new PnGetMessagesResponse();
                pnGetMessagesResponse.setPnListOfMessages(new PnListOfMessages(messages.getItem()));
                return pnGetMessagesResponse;
            })
            .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PEC_GET_UNREAD_MESSAGES, result))
            .doOnError(throwable -> log.error(EXCEPTION_IN_PROCESS, PEC_GET_UNREAD_MESSAGES, throwable, throwable.getMessage()));
    }

    @Override
    public Mono<Void> markMessageAsRead(String messageID) {
        GetMessageID getMessageID = new GetMessageID();
        getMessageID.setMailid(messageID);
        getMessageID.setIsuid(2);
        getMessageID.setMarkseen(1);
        return arubaService.getMessageId(getMessageID)
                .then()
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_NO_RESULT_LABEL, PEC_MARK_MESSAGE_AS_READ, result))
                .doOnError(throwable -> log.error(EXCEPTION_IN_PROCESS, PEC_MARK_MESSAGE_AS_READ, throwable, throwable.getMessage()));
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
