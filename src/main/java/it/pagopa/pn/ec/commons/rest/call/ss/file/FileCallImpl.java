package it.pagopa.pn.ec.commons.rest.call.ss.file;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ss.FilesEndpointProperties;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic400ErrorException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.AttachmentNotAvailableException;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class FileCallImpl implements FileCall {

    private final WebClient ssExternalEndpointBasePath;
    private final FilesEndpointProperties filesEndpointProperties;

    private static final String GET_FILE_ERROR_TITLE = "Chiamata a SafeStorage non valida";

    public FileCallImpl(WebClient ssWebClient, FilesEndpointProperties filesEndpointProperties) {
        this.ssExternalEndpointBasePath = ssWebClient;
        this.filesEndpointProperties = filesEndpointProperties;
    }

    private static String getFileErrorDetails(String fileKey, String xPagopaExtchCxId) {
        return String.format("Error retrieving attachment '%s' by client '%s'", fileKey, xPagopaExtchCxId);
    }

    @Override
    public Mono<FileDownloadResponse> getFile(String fileKey, String xPagopaExtchCxId, boolean metadataOnly) {
        return ssExternalEndpointBasePath.get()
                                         .uri(uriBuilder -> uriBuilder.path(filesEndpointProperties.getFile())
                                                                      .queryParam("metadataOnly", metadataOnly)
                                                                      .build(fileKey))
                                         .retrieve()
                                         .onStatus(status -> status.equals(HttpStatus.BAD_REQUEST) || status.equals(HttpStatus.FORBIDDEN),
                                                   clientResponse -> Mono.error(new Generic400ErrorException(GET_FILE_ERROR_TITLE,
                                                                                                             getFileErrorDetails(fileKey,
                                                                                                                                 xPagopaExtchCxId))))
                                         .onStatus(NOT_FOUND::equals,
                                                   clientResponse -> Mono.error(new AttachmentNotAvailableException(fileKey)))
                                         .bodyToMono(FileDownloadResponse.class);
    }
}