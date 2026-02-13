package it.pagopa.pn.ec.commons.rest.call.download;

import lombok.CustomLog;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;


import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@Component
@CustomLog
public class DownloadCallImpl implements DownloadCall {


    public DownloadCallImpl() {
    }

    @Override
    public Mono<OutputStream> downloadFile(String url) {
        OutputStream outputStream = new ByteArrayOutputStream();
        log.info(CLIENT_METHOD_INVOCATION_WITH_ARGS, DOWNLOAD_FILE, url);

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(url);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        WebClient downloadWebClient = WebClient.builder().uriBuilderFactory(factory).build();

        return DataBufferUtils.write(downloadWebClient.get().uri(url).retrieve().bodyToFlux(DataBuffer.class), outputStream)
                              .map(DataBufferUtils::release)
                              .then(Mono.just(outputStream))
                              .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, DOWNLOAD_FILE, url))
                              .doOnError(e -> {
                                  log.error("Error in downloadFile class: {}", e.getMessage());
                              });
                              }
}
