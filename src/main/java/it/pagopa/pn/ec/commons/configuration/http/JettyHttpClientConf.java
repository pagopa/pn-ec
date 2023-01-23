package it.pagopa.pn.ec.commons.configuration.http;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class JettyHttpClientConf {

    private final CharsetDecoder charsetDecoder = StandardCharsets.UTF_8.newDecoder();
    private final SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();

    @Bean
    public HttpClient getJettyHttpClient() {
        return new HttpClient(sslContextFactory) {
            @Override
            public Request newRequest(URI uri) {
                Request request = super.newRequest(uri);
                return enhance(request);
            }
        };
    }

    private Request enhance(Request request) {

        request.onRequestBegin(theRequest -> log.info("Start {} request to {}", theRequest.getMethod(), theRequest.getURI()));

        request.onRequestHeaders(theRequest -> {
            for (HttpField header : theRequest.getHeaders()) {
                log.info("Header {} --> {}", header.getName(), header.getValue());
            }
        });

        request.onRequestContent((theRequest, content) -> {
            try {
                log.info("Request body --> {}", charsetDecoder.decode(content));
            } catch (CharacterCodingException e) {
                log.error(e.getMessage(), e);
            }
        });

        request.onResponseContent((theResponse, content) -> {
            try {
                log.info("Response body --> {}", charsetDecoder.decode(content));
            } catch (CharacterCodingException e) {
                log.error(e.getMessage(), e);
            }
        });

        return request;
    }
}
