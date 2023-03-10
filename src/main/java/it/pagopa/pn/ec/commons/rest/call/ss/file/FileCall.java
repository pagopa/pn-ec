package it.pagopa.pn.ec.commons.rest.call.ss.file;

import it.pagopa.pn.ec.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.ec.rest.v1.dto.FileCreationResponse;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileCall {

    Mono<FileDownloadResponse> getFile(String fileKey, String xPagopaExtchCxId, boolean metadataOnly);

    Mono<FileCreationResponse> postFile(FileCreationRequest fileCreationRequest);
}
