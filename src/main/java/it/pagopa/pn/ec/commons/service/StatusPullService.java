package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.rest.v1.dto.CourtesyMessageProgressEvent;
import it.pagopa.pn.ec.rest.v1.dto.LegalMessageSentDetails;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusEvent;
import reactor.core.publisher.Mono;

public interface StatusPullService {

    Mono<CourtesyMessageProgressEvent> digitalPullService(String requestIdx, String xPagopaExtchCxId, String processId);
    Mono<PaperProgressStatusEvent> paperPullService(String requestIdx, String xPagopaExtchCxId);
    Mono<LegalMessageSentDetails> legalPullService(String requestIdx, String xPagopaExtchCxId, String processId);
}
