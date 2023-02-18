package it.pagopa.pn.ec.commons.rest.call.uribuilder;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ss.FilesEndpointProperties;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ss.SafeStorageEndpointProperties;
import it.pagopa.pn.ec.commons.exception.ss.GetFileError;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class UriBuilderCallImpl implements UriBuilderCall {

    private final WebClient ssExternalEndpointBasePath;
    private final SafeStorageEndpointProperties safeStorageEndpointProperties;
    private final FilesEndpointProperties filesEndpointProperties;

    public UriBuilderCallImpl(WebClient ssWebClient, SafeStorageEndpointProperties safeStorageEndpointProperties,
                              FilesEndpointProperties filesEndpointProperties) {
        this.ssExternalEndpointBasePath = ssWebClient;
        this.safeStorageEndpointProperties = safeStorageEndpointProperties;
        this.filesEndpointProperties = filesEndpointProperties;
    }

    @Override
    public Mono<FileDownloadResponse> getFile(String fileKey, String xPagopaExtchCxId, boolean metadataOnly) {
        return ssExternalEndpointBasePath.get()
                                         .uri(uriBuilder -> uriBuilder.path(filesEndpointProperties.getFile())
                                                                      .queryParam("metadataOnly", metadataOnly)
                                                                      .build(fileKey))
                                         .header(safeStorageEndpointProperties.clientHeaderName(), xPagopaExtchCxId)
                                         .retrieve()
                                         .onStatus(HttpStatus::isError,
                                                   clientResponse -> Mono.error(new GetFileError(fileKey, xPagopaExtchCxId)))
                                         .bodyToMono(FileDownloadResponse.class);
    }
}
