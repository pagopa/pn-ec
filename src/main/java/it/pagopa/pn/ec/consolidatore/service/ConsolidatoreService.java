package it.pagopa.pn.ec.consolidatore.service;

import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pn.ec.rest.v1.dto.PreLoadRequestData;
import it.pagopa.pn.ec.rest.v1.dto.PreLoadResponseData;
import reactor.core.publisher.Mono;

public interface ConsolidatoreService {

    Mono<PreLoadResponseData> presignedUploadRequest(String xPagopaExtchServiceId, String xApiKey, String xTraceId, Mono<PreLoadRequestData> attachments);

    Mono<FileDownloadResponse> getFile(String fileKey, String xPagopaExtchServiceId, String xApiKey, String xTraceId);

}
