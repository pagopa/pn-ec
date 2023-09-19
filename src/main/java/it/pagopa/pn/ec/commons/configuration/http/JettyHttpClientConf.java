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
	
    @Value("${jetty.maxConnectionsPerDestination}")
    private int maxConnections;

    private final SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();

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

        request.onRequestFailure((theRequest, throwable) -> log.error("Request failure : {} , {}", throwable, throwable.getMessage()));

        return request;
    }

}
