package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.rest.v1.dto.CourtesyMessageProgressEvent;
import reactor.core.publisher.Flux;

public interface StatusPullService {

    Flux<CourtesyMessageProgressEvent> digitalPullService(String requestIdx, String xPagopaExtchCxId);
    Flux<CourtesyMessageProgressEvent> paperPullService(String requestIdx, String xPagopaExtchCxId);
}
