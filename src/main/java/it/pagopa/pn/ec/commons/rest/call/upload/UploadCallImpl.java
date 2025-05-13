package it.pagopa.pn.ec.commons.rest.call.upload;

import it.pagopa.pn.ec.rest.v1.dto.DocumentTypeConfiguration;
import lombok.CustomLog;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.net.URI;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.LogUtils.DOWNLOAD_FILE;

@Component
@CustomLog
public class UploadCallImpl implements UploadCall {

    private final WebClient uploadWebClient;

    public UploadCallImpl(WebClient uploadWebClient) {
        this.uploadWebClient = uploadWebClient;
    }

    @Override
    public Mono<Void> uploadFile(String fileKey, String url, String secret, String contentType, DocumentTypeConfiguration.ChecksumEnum checksum, String checksumValue, byte[] fileBytes) {
        log.info(CLIENT_METHOD_INVOCATION_WITH_ARGS, UPLOAD_FILE, url);
        String checksumHeaderName = switch (checksum) {
            case MD5 -> "Content-MD5";
            case SHA256 -> "x-amz-checksum-sha256";
        };
        return uploadWebClient.put()
                .uri(URI.create(url))
                .header("content-type", contentType)
                .header("x-amz-meta-secret", secret)
                .header(checksumHeaderName, checksumValue)
                .bodyValue(fileBytes)
                .retrieve()
                .toBodilessEntity()
                .then()
                .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, DOWNLOAD_FILE, url))
                .doOnError(e -> log.error("Error in uploadFile class: {}", e.getMessage()));
    }
}
