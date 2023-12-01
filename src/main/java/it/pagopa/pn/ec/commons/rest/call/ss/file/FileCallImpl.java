package it.pagopa.pn.ec.commons.rest.call.ss.file;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ss.FilesEndpointProperties;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ss.SafeStorageEndpointProperties;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic400ErrorException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.AttachmentNotAvailableException;
import it.pagopa.pn.ec.consolidatore.exception.ClientNotAuthorizedOrFoundException;
import it.pagopa.pn.ec.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.ec.rest.v1.dto.FileCreationResponse;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import lombok.CustomLog;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Component
@CustomLog
public class FileCallImpl implements FileCall {

    private final WebClient ssWebClient;
    private final SafeStorageEndpointProperties safeStorageEndpointProperties;

    private final FilesEndpointProperties filesEndpointProperties;

    private static final String GET_FILE_ERROR_TITLE = "Chiamata a SafeStorage non valida";

    public FileCallImpl(WebClient ssWebClient, SafeStorageEndpointProperties safeStorageEndpointProperties, FilesEndpointProperties filesEndpointProperties) {
        this.ssWebClient = ssWebClient;
        this.safeStorageEndpointProperties = safeStorageEndpointProperties;
        this.filesEndpointProperties = filesEndpointProperties;
    }

    private static String getFileErrorDetails(String fileKey, String xPagopaExtchCxId) {
        return String.format("Error retrieving attachment '%s' by client '%s'", fileKey, xPagopaExtchCxId);
    }

    @Override
    public Mono<FileDownloadResponse> getFile(String fileKey, String xPagopaExtchCxId, boolean metadataOnly) {
        log.logInvokingExternalService(SAFE_STORAGE_SERVICE, GET_FILE);
        return ssWebClient.get()
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
                .onStatus(status-> status.equals(HttpStatus.GONE),
                        clientResponse -> Mono.error(new Generic400ErrorException(GET_FILE_ERROR_TITLE, "Resource is no longer available. It may have been removed or deleted.")))
                .bodyToMono(FileDownloadResponse.class);
    }


    @Override
    public Mono<FileDownloadResponse> getFile(String fileKey, String xPagopaExtchServiceId, String xApiKey, String xTraceId) {
        log.logInvokingExternalService(SAFE_STORAGE_SERVICE, GET_FILE);
        return ssWebClient.get()
                .uri(uriBuilder -> uriBuilder.path(filesEndpointProperties.getFile())
                        .build(fileKey))
                .header(safeStorageEndpointProperties.clientHeaderName(), xPagopaExtchServiceId)
                .header(safeStorageEndpointProperties.apiKeyHeaderName(), xApiKey)
                .header(safeStorageEndpointProperties.traceIdHeaderName(), xTraceId)
                .retrieve()
                .onStatus(HttpStatus.FORBIDDEN::equals, clientResponse -> Mono.error(new ClientNotAuthorizedOrFoundException(xPagopaExtchServiceId)))
                .onStatus(status-> status.equals(HttpStatus.GONE),
                        clientResponse -> Mono.error(new Generic400ErrorException(GET_FILE_ERROR_TITLE, "Resource is no longer available. It may have been removed or deleted.")))
                .bodyToMono(FileDownloadResponse.class);
    }

    @Override
    public Mono<FileCreationResponse> postFile(String xPagopaExtchServiceId, String xApiKey, String checksumValue, String xTraceId, FileCreationRequest fileCreationRequest) {
        log.logInvokingExternalService(SAFE_STORAGE_SERVICE, POST_FILE);
        return ssWebClient.post().uri(filesEndpointProperties.postFile())
                .header(safeStorageEndpointProperties.clientHeaderName(), xPagopaExtchServiceId)
                .header(safeStorageEndpointProperties.apiKeyHeaderName(), xApiKey)
                .header(safeStorageEndpointProperties.checksumValueHeaderName(), checksumValue)
                .header(safeStorageEndpointProperties.traceIdHeaderName(), xTraceId)
                .body(BodyInserters.fromValue(fileCreationRequest))
                .retrieve()
                .bodyToMono(FileCreationResponse.class);
    }


}
