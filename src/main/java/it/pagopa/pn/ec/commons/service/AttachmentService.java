package it.pagopa.pn.ec.commons.service;

import java.util.List;

import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AttachmentService {

    Mono<FileDownloadResponse> getAllegatiPresignedUrlOrMetadata(String attachmentUrls, String xPagopaExtchCxId, boolean metadataOnly);

    Flux<FileDownloadResponse> getAllegatiPresignedUrlOrMetadata(List<String> attachmentUrls, String xPagopaExtchCxId, boolean metadataOnly);
}
