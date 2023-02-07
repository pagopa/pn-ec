package it.pagopa.pn.ec.commons.configuration.http;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ec.ExternalChannelEndpointProperties;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ss.SafeStorageEndpointProperties;
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
public class WebClientConf {

    private final JettyHttpClientConf jettyHttpClientConf;
    private final ExternalChannelEndpointProperties externalChannelEndpointProperties;
    private final SafeStorageEndpointProperties safeStorageEndpointProperties;

    @Value("${statemachine.url}")
    String stateMachineBasePath;

    public WebClientConf(JettyHttpClientConf jettyHttpClientConf, ExternalChannelEndpointProperties externalChannelEndpointProperties,
                         SafeStorageEndpointProperties safeStorageEndpointProperties) {
        this.jettyHttpClientConf = jettyHttpClientConf;
        this.externalChannelEndpointProperties = externalChannelEndpointProperties;
        this.safeStorageEndpointProperties = safeStorageEndpointProperties;
    }

    private WebClient.Builder defaultWebClientBuilder() {
        return WebClient.builder()
                        .clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()))
                        .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
    }

    @Bean
    public WebClient ecWebClient() {
        return defaultWebClientBuilder().baseUrl(externalChannelEndpointProperties.containerBasePath()).build();
    }

    @Bean
    public WebClient ssWebClient() {
        return defaultWebClientBuilder().baseUrl(safeStorageEndpointProperties.containerBasePath()).build();
    }

    @Bean
    public WebClient stateMachineWebClient() {
        return defaultWebClientBuilder().baseUrl(stateMachineBasePath).build();
    }
}
