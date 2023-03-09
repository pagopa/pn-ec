package it.pagopa.pn.ec.commons.rest.call.download;

import reactor.core.publisher.Mono;

import java.io.OutputStream;

public interface DownloadCall {

    Mono<OutputStream> downloadFile(String url);
}
