package it.pagopa.pn.library.pec.service;

import it.pec.bridgews.*;
import reactor.core.publisher.Mono;

public interface ArubaService {

    Mono<GetMessageCountResponse> getMessageCount(GetMessageCount getMessageCount);

    Mono<DeleteMailResponse> deleteMail(DeleteMail deleteMail);

    Mono<SendMailResponse> sendMail(SendMail sendMail) ;

    Mono<GetMessagesResponse> getMessages(GetMessages getMessages) ;

    Mono<GetMessageIDResponse> getMessageId(GetMessageID getMessageID) ;

    }
