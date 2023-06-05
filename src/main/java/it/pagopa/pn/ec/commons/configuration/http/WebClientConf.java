package it.pagopa.pn.ec.commons.configuration.http;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.consolidatore.ConsolidatoreEndpointProperties;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ec.ExternalChannelEndpointProperties;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ss.SafeStorageEndpointProperties;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.statemachine.StateMachineEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
public class WebClientConf {

    private final JettyHttpClientConf jettyHttpClientConf;

    public WebClientConf(JettyHttpClientConf jettyHttpClientConf) {
        this.jettyHttpClientConf = jettyHttpClientConf;
    }

    private WebClient.Builder defaultWebClientBuilder() {
        return WebClient.builder().clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()));
    }

    private WebClient.Builder defaultJsonWebClientBuilder() {
        return defaultWebClientBuilder().defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
    }

    @Bean
    public WebClient ecWebClient(ExternalChannelEndpointProperties externalChannelEndpointProperties) {
        return defaultJsonWebClientBuilder().baseUrl(externalChannelEndpointProperties.containerBaseUrl()).build();
    }

    @Bean
    public WebClient ssWebClient(SafeStorageEndpointProperties safeStorageEndpointProperties) {
        return defaultJsonWebClientBuilder().baseUrl(safeStorageEndpointProperties.containerBaseUrl()).defaultHeaders(httpHeaders -> {
            httpHeaders.set(safeStorageEndpointProperties.clientHeaderName(), safeStorageEndpointProperties.clientHeaderValue());
            httpHeaders.set(safeStorageEndpointProperties.apiKeyHeaderName(), safeStorageEndpointProperties.apiKeyHeaderValue());
        }).build();
    }

    @Bean
    public WebClient downloadWebClient() {
        return defaultWebClientBuilder().build();
    }

    @Bean
    public WebClient uploadWebClient() {
        return defaultWebClientBuilder().build();
    }

    @Bean
    public WebClient stateMachineWebClient(StateMachineEndpointProperties stateMachineEndpointProperties) {
        return defaultJsonWebClientBuilder().baseUrl(stateMachineEndpointProperties.containerBaseUrl()).build();
    }

    @Bean
    public WebClient consolidatoreWebClient(ConsolidatoreEndpointProperties consolidatoreEndpointProperties) {
        return defaultJsonWebClientBuilder().baseUrl(consolidatoreEndpointProperties.baseUrl()).defaultHeaders(httpHeaders -> {
            httpHeaders.set(consolidatoreEndpointProperties.clientHeaderName(), consolidatoreEndpointProperties.clientHeaderValue());
            httpHeaders.set(consolidatoreEndpointProperties.apiKeyHeaderName(), consolidatoreEndpointProperties.apiKeyHeaderValue());
        }).build();
    }
}
