package it.pagopa.pn.ec.commons.rest.call.aruba;

import it.pagopa.pn.ec.commons.exception.aruba.ArubaCallMaxRetriesExceededException;
import it.pec.bridgews.*;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

public interface ArubaCall {

    Mono<GetMessagesResponse> getMessages(GetMessages getMessages);
    Mono<GetMessageIDResponse> getMessageId(GetMessageID getMessageID);
    Mono<SendMailResponse> sendMail(SendMail sendMail);
    Mono<GetAttachResponse> getAttach(GetAttach getAttach);

    Retry ARUBA_CALL_RETRY_STRATEGY = Retry.backoff(3, Duration.ofSeconds(2)).onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
        throw new ArubaCallMaxRetriesExceededException();
    });
}
