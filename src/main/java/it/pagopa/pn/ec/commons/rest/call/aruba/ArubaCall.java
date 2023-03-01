package it.pagopa.pn.ec.commons.rest.call.aruba;

import it.pagopa.pn.ec.commons.exception.aruba.ArubaSendException;
import it.pec.bridgews.*;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

public interface ArubaCall {

    Mono<GetMessagesResponse> getMessages(GetMessages getMessages);
    Mono<GetMessageIDResponse> getMessageId(GetMessageID getMessageID);
    Mono<SendMailResponse> sendMail(SendMail sendMail);
    Mono<GetAttachResponse> getAttach(GetAttach getAttach);

    //  Spiegazione per jitter https://www.baeldung.com/resilience4j-backoff-jitter#jitter
    Retry DEFAULT_RETRY_STRATEGY = Retry.backoff(3, Duration.ofSeconds(2))
//                                        .jitter(0.75)
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                throw new ArubaSendException.ArubaMaxRetriesExceededException();
            });
}
