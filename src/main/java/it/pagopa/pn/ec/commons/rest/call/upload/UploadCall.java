package it.pagopa.pn.ec.commons.rest.call.upload;

import it.pagopa.pn.ec.rest.v1.dto.DocumentTypeConfiguration;
import reactor.core.publisher.Mono;

import java.io.OutputStream;

public interface UploadCall {
    Mono<Void> uploadFile(String fileKey, String url, String secret, String contentType, DocumentTypeConfiguration.ChecksumEnum checksum, String checksumValue, byte[] fileBytes);
}
