package it.pagopa.pn.ec.commons.rest.call.uribuilder;

import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import reactor.core.publisher.Mono;

public interface UriBuilderCall {

    Mono<FileDownloadResponse> getFile(String fileKey, String xPagopaExtchCxId, boolean metadataOnly);
}
