package it.pagopa.pn.ec.commons.configuration.http;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@Configuration
@Slf4j
public class JettyHttpClientConf {
	
    @Value("jetty.maxConnectionsPerDestination")
    private int maxConnections;

    private final SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
    private static final List<String> CONTENT_TYPE_OF_RESPONSE_BODY_TO_LOG = List.of(APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE);

    @Bean
    public HttpClient getJettyHttpClient() {
        HttpClient myHC = new HttpClient(sslContextFactory) {
            @Override
            public Request newRequest(URI uri) {
                Request request = super.newRequest(uri);
                return enhance(request);
            }
        };
        myHC.setMaxConnectionsPerDestination(maxConnections);
//        myHC.setMaxRequestsQueuedPerDestination(2048);
        return myHC;
    }

    @Bean
    public HttpClient getTrustAllJettyHttpClient() {

        var context = new SslContextFactory.Client();
        context.setTrustAll(true);

        return new HttpClient(context) {
            @Override
            public Request newRequest(URI uri) {
                Request request = super.newRequest(uri);
                return enhance(request);
            }
        };

    }

    private Request enhance(Request request) {

        request.onRequestBegin(theRequest -> log.debug("Start {} request to {}", theRequest.getMethod(), theRequest.getURI()));

        request.onRequestFailure((theRequest, throwable) -> {
            log.error("Request failure : {} , {}", throwable, throwable.getMessage());
        });

        request.onRequestHeaders(theRequest -> {
            for (HttpField header : theRequest.getHeaders()) {
                log.debug("Header {} --> {}", header.getName(), header.getValue());
            }
        });

        request.onRequestContent((theRequest, content) -> {
            try {
                log.debug("Request body --> {}", decodeContent(content));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });

        request.onResponseContent((theResponse, content) -> {
            if (CONTENT_TYPE_OF_RESPONSE_BODY_TO_LOG.contains(theResponse.getHeaders().get(CONTENT_TYPE))) {
                try {
                    log.debug("Response body --> {}", decodeContent(content));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });

        return request;
    }

    private String decodeContent(ByteBuffer content) {
        byte[] bytes = new byte[content.remaining()];
        content.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
