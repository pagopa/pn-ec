package it.pagopa.pn.ec.richiestemetadati.service;

import it.pagopa.pn.ec.rest.v1.dto.RequestMetadataPatchRequest;
import reactor.core.publisher.Mono;

public interface PaperRequestMetadataPatchService {

    public Mono<Void> patchIsOpenReworkRequest(String xPagopaExtchCxId,String requestId, RequestMetadataPatchRequest req);
}
