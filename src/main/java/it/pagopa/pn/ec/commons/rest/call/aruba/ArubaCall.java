package it.pagopa.pn.ec.commons.rest.call.aruba;

import it.pec.bridgews.*;
import reactor.core.publisher.Mono;

public interface ArubaCall {

    Mono<GetMessagesResponse> getMessages(GetMessages getMessages);
    Mono<GetMessageIDResponse> getMessageId(GetMessageID getMessageID);
    Mono<SendMailResponse> sendMail(SendMail sendMail);
    Mono<GetAttachResponse> getAttach(GetAttach getAttach);

}
