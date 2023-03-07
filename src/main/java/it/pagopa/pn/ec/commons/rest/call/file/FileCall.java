package it.pagopa.pn.ec.commons.rest.call.file;

import org.springframework.core.io.FileSystemResource;
import reactor.core.publisher.Mono;

public interface FileCall {

    Mono<FileSystemResource> downloadFile(String url);
}
