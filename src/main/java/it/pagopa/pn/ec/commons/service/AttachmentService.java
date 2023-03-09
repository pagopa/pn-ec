package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AttachmentService {


    Flux<FileDownloadResponse> getAllegatiPresignedUrlOrMetadata(List<String> attachmentUrls, String xPagopaExtchCxId, boolean metadataOnly);
}
