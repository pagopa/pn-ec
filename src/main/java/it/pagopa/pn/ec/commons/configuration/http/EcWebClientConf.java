package it.pagopa.pn.ec.commons.configuration.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
@Slf4j
public class EcWebClientConf {

    @Value("${ec.internal.endpoint.base.path}")
    String ecInternalEndpointBasePath;

    @Value("${ss.internal.endpoint.safe.storage}")
    String ssExternalEndpointBasePath;

    @Bean
    public WebClient ecInternalWebClient(JettyHttpClientConf jettyHttpClientConf) {
        return WebClient.builder()
                        .clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()))
                        .baseUrl(ecInternalEndpointBasePath)
                        .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .build();
    }

    @Bean
    public WebClient ssExternalWebClient(JettyHttpClientConf jettyHttpClientConf) {
        return WebClient.builder()
                .clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()))
                .baseUrl(ssExternalEndpointBasePath)
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .build();
    }

}
