package it.pagopa.pn.ec.commons.configuration.http;

import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;

@CustomLog
@Configuration
public class NettyWebFilter implements WebFilter {

    @Override
    public @NotNull Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest httpRequest = exchange.getRequest();

        ServerHttpRequestDecorator loggingServerHttpRequestDecorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
            String requestBody = "";

            @Override
            public @NotNull Flux<DataBuffer> getBody() {
                return super.getBody().doOnNext(dataBuffer -> {
                    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                        Channels.newChannel(byteArrayOutputStream).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
                        requestBody = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
                        exchange.getAttributes().put("requestBody", requestBody);
                    } catch (IOException e) {
                        log.error("Failed to read incoming http request");
                    }
                });
            }
        };

        return chain.filter(exchange.mutate().request(loggingServerHttpRequestDecorator).build());
    }

}
