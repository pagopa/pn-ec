package it.pagopa.pn.ec.commons.rest.call.download;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

@Component
@Slf4j
public class DownloadCallImpl implements DownloadCall {

    private final WebClient downloadWebClient;

    public DownloadCallImpl(WebClient downloadWebClient) {
        this.downloadWebClient = downloadWebClient;
    }

    @Override
    public Mono<OutputStream> downloadFile(String url) {
        OutputStream outputStream = new ByteArrayOutputStream();
        return DataBufferUtils.write(downloadWebClient.get().uri(URI.create(url)).retrieve().bodyToFlux(DataBuffer.class), outputStream)
                              .map(DataBufferUtils::release)
                              .then(Mono.just(outputStream))
                              .doOnError(e -> log.error("Error in downloadFile class: {}", e.getMessage()));
    }
}
