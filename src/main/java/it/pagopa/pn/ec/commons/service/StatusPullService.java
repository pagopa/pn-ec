package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.rest.v1.dto.CourtesyMessageProgressEvent;
import it.pagopa.pn.ec.rest.v1.dto.LegalMessageSentDetails;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StatusPullService {

    Mono<CourtesyMessageProgressEvent> digitalPullService(String requestIdx, String xPagopaExtchCxId, String processId);
    Flux<CourtesyMessageProgressEvent> paperPullService(String requestIdx, String xPagopaExtchCxId);
    Mono<LegalMessageSentDetails> legalPullService(String requestIdx, String xPagopaExtchCxId, String processId);
}
