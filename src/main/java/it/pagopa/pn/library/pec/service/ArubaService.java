package it.pagopa.pn.library.pec.service;

import it.pec.bridgews.DeleteMail;
import it.pec.bridgews.DeleteMailResponse;
import it.pec.bridgews.GetMessageCount;
import it.pec.bridgews.GetMessageCountResponse;
import reactor.core.publisher.Mono;

public interface ArubaService {

    Mono<GetMessageCountResponse> getMessagesCount(GetMessageCount getMessageCount);

    Mono<DeleteMailResponse> deleteMail(DeleteMail deleteMail);

}
