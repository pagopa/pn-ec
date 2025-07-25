package it.pagopa.pn.ec.commons.configuration.http;

import it.pagopa.pn.commons.utils.MDCUtils;
import lombok.CustomLog;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.URI;
import java.util.Map;

import static it.pagopa.pn.ec.commons.utils.LogUtils.MDC_CORR_ID_KEY;

@Configuration
@CustomLog
public class JettyHttpClientConf {
	
    @Value("${jetty.maxConnectionsPerDestination}")
    private int maxConnections;
    @Value("${pn.log.cx-id-header}")
    private String corrIdHeaderName;

    private final SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();

    @Bean
    public HttpClient getJettyHttpClient() {
        HttpClient myHC = new HttpClient(sslContextFactory) {
            @Override
            public Request newRequest(URI uri) {
                Request request = super.newRequest(uri);
                return enhance(request, MDCUtils.retrieveMDCContextMap());
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
                return enhance(request, MDCUtils.retrieveMDCContextMap());
            }
        };

    }

    private Request enhance(Request request, Map<String, String> mdcContextMap) {

        return request.onRequestBegin(theRequest -> {
                    MDCUtils.enrichWithMDC(theRequest, mdcContextMap);
                    if (mdcContextMap != null && mdcContextMap.containsKey(MDC_CORR_ID_KEY)) {
                        request.header(corrIdHeaderName, MDC.get(MDC_CORR_ID_KEY));
                    }
                    log.info("Start {} request to {}", theRequest.getMethod(), theRequest.getURI());
                })
                .onRequestFailure((theRequest, throwable) -> {
                    MDCUtils.enrichWithMDC(theRequest, mdcContextMap);
                    log.info("Request failure : {} , {}", throwable, throwable.getMessage());
                });


    }

}
