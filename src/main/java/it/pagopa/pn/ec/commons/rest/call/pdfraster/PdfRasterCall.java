package it.pagopa.pn.ec.commons.rest.call.pdfraster;

import it.pagopa.pn.ec.pdfraster.model.dto.PdfRasterResponse;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import reactor.core.publisher.Mono;

public interface PdfRasterCall {

    Mono<PdfRasterResponse> convertPdf(String fileKey) throws RestCallException;
}
