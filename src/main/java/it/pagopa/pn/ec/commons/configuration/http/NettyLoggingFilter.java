package it.pagopa.pn.ec.commons.configuration.http;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class NettyLoggingFilter implements WebFilter {

    @Override
    public @NotNull Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest httpRequest = exchange.getRequest();
        final String httpUrl = httpRequest.getURI().toString();
        String httpMethod=httpRequest.getMethodValue();

        ServerHttpRequestDecorator loggingServerHttpRequestDecorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
            String requestBody = "";

            @Override
            public @NotNull Flux<DataBuffer> getBody() {
                return super.getBody().doOnNext(dataBuffer -> {
                    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                        Channels.newChannel(byteArrayOutputStream).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
                        requestBody = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
                        log.info("{} {} - Request Body: {}", httpMethod, httpUrl, requestBody);
                    } catch (IOException e) {
                        log.error("Failed to log incoming http request");
                    }
                });
            }
        };

        ServerHttpResponseDecorator loggingServerHttpResponseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {
            String responseBody = "";
            @Override
            public @NotNull Mono<Void> writeWith(@NotNull Publisher<? extends DataBuffer> body) {
                Mono<DataBuffer> buffer = Mono.from(body);
                return super.writeWith(buffer.doOnNext(dataBuffer -> {
                    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                        Channels.newChannel(byteArrayOutputStream).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
                        responseBody = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
                        log.info("Response body : {}", responseBody);
                    } catch (Exception e) {
                        log.error("Failed to log http response");
                    }
                }));
            }
        };
        return chain.filter(exchange.mutate().request(loggingServerHttpRequestDecorator).response(loggingServerHttpResponseDecorator).build());
    }

}
