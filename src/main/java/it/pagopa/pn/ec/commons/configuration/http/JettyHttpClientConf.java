package it.pagopa.pn.ec.commons.configuration.http;

import it.pagopa.pn.commons.utils.MDCUtils;
import lombok.CustomLog;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Map;
import org.eclipse.jetty.client.transport.HttpClientTransportOverHTTP;
import org.eclipse.jetty.io.ClientConnector;

import static it.pagopa.pn.ec.commons.utils.LogUtils.MDC_CORR_ID_KEY;

@Configuration
@CustomLog
public class JettyHttpClientConf {
	
    @Value("${jetty.maxConnectionsPerDestination}")
    private int maxConnections;
    @Value("${pn.log.cx-id-header}")
    private String corrIdHeaderName;


    @Bean
    public HttpClient getJettyHttpClient() throws Exception {
        ClientConnector clientConnector = new ClientConnector();
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        clientConnector.setSslContextFactory(sslContextFactory);

        HttpClient httpClient = new HttpClient(new HttpClientTransportOverHTTP(clientConnector));
        httpClient.setMaxConnectionsPerDestination(maxConnections);

        enhance(httpClient);

        httpClient.start();
        return httpClient;
    }

    @Bean
    public HttpClient getTrustAllJettyHttpClient() throws Exception {
        ClientConnector clientConnector = new ClientConnector();
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setTrustAll(true);  // <-- QUESTA È L'UNICA DIFFERENZA
        clientConnector.setSslContextFactory(sslContextFactory);

        HttpClient httpClient = new HttpClient(new HttpClientTransportOverHTTP(clientConnector));

        enhance(httpClient);

        httpClient.start();
        return httpClient;
    }

    private void enhance(HttpClient httpClient) {
        httpClient.addEventListener(new org.eclipse.jetty.client.Request.Listener() {
            @Override
            public void onBegin(org.eclipse.jetty.client.Request request) {
                Map<String, String> mdcContextMap = MDCUtils.retrieveMDCContextMap();
                MDCUtils.enrichWithMDC(request, mdcContextMap);
                if (mdcContextMap != null && mdcContextMap.containsKey(MDC_CORR_ID_KEY)) {
                    request.headers(headers -> headers.put(corrIdHeaderName, MDC.get(MDC_CORR_ID_KEY)));
                }
                log.info("Start {} request to {}", request.getMethod(), request.getURI());
            }

            @Override
            public void onFailure(org.eclipse.jetty.client.Request request, Throwable failure) {
                Map<String, String> mdcContextMap = MDCUtils.retrieveMDCContextMap();
                MDCUtils.enrichWithMDC(request, mdcContextMap);
                log.info("Request failure : {} , {}", failure, failure.getMessage());
            }
        });
    }

}
