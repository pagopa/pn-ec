package it.pagopa.pn.ec.commons.rest.call.file;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

@Component
public class FileCallImpl implements FileCall {

    private final WebClient downloadWebClient;

    public FileCallImpl(WebClient downloadWebClient) {
        this.downloadWebClient = downloadWebClient;
    }

    public Mono<OutputStream> downloadFile(String url) {
        return downloadWebClient.get().uri(url).retrieve().bodyToFlux(DataBuffer.class)
                                .map(DataBuffer::asInputStream)
                                .reduce(ByteArrayOutputStream::new, (outputStream, inputStream) -> {
                                    try {
                                        inputStream.transferTo(outputStream);
                                        return outputStream;
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .map(ByteArrayOutputStream::toByteArray)
                                .map(ByteArrayInputStream::new)
                                .map(inputStream -> (OutputStream) new BufferedOutputStream(new ByteArrayOutputStream(inputStream)))
                                .single();
    }
}
