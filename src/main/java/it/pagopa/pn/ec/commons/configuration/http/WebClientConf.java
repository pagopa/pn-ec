package it.pagopa.pn.ec.commons.configuration.http;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.consolidatore.ConsolidatoreEndpointProperties;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ec.ExternalChannelEndpointProperties;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.pdfraster.PdfRasterEndpointProperties;
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

    private final String HTTPS="https";
    private final JettyHttpClientConf jettyHttpClientConf;

    public WebClientConf(JettyHttpClientConf jettyHttpClientConf) {
        this.jettyHttpClientConf = jettyHttpClientConf;
    }

    private WebClient.Builder defaultWebClientBuilder() throws Exception {
        return WebClient.builder().clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()));
    }

    private WebClient.Builder trustAllWebClientBuilder() throws Exception {
        return WebClient.builder().clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getTrustAllJettyHttpClient()));
    }

    private WebClient.Builder defaultJsonWebClientBuilder() throws Exception {
        return defaultWebClientBuilder().defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
    }

    private WebClient.Builder trustAllJsonWebClientBuilder() throws Exception {
        return trustAllWebClientBuilder().defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
    }

    @Bean
    public WebClient ecWebClient(ExternalChannelEndpointProperties externalChannelEndpointProperties) throws Exception {
        return defaultJsonWebClientBuilder().baseUrl(externalChannelEndpointProperties.containerBaseUrl()).build();
    }

    @Bean
    public WebClient ssWebClient(SafeStorageEndpointProperties safeStorageEndpointProperties) throws Exception {
        return defaultJsonWebClientBuilder().baseUrl(safeStorageEndpointProperties.containerBaseUrl()).defaultHeaders(httpHeaders -> {
            httpHeaders.set(safeStorageEndpointProperties.clientHeaderName(), safeStorageEndpointProperties.clientHeaderValue());
            httpHeaders.set(safeStorageEndpointProperties.apiKeyHeaderName(), safeStorageEndpointProperties.apiKeyHeaderValue());
        }).build();
    }

    @Bean
    public WebClient downloadWebClient() throws Exception {
        return defaultWebClientBuilder().build();
    }

    @Bean
    public WebClient uploadWebClient() throws Exception {
        return defaultWebClientBuilder().build();
    }

    @Bean
    public WebClient stateMachineWebClient(StateMachineEndpointProperties stateMachineEndpointProperties) throws Exception {
        return defaultJsonWebClientBuilder().baseUrl(stateMachineEndpointProperties.containerBaseUrl()).build();
    }

    @Bean
    public WebClient consolidatoreWebClient(ConsolidatoreEndpointProperties consolidatoreEndpointProperties) throws Exception {
        String consolidatoreBaseUrl = consolidatoreEndpointProperties.baseUrl();

        if (consolidatoreBaseUrl.startsWith(HTTPS) && consolidatoreEndpointProperties.trustAll()) {
            return trustAllConsolidatoreWebClient(consolidatoreEndpointProperties);
        } else return defaultConsolidatoreWebClient(consolidatoreEndpointProperties);
    }

    @Bean
    public WebClient pdfRasterWebClient(PdfRasterEndpointProperties pdfRasterEndpointProperties,SafeStorageEndpointProperties safeStorageEndpointProperties) throws Exception {
        String pdfRasterBaseUrl = pdfRasterEndpointProperties.baseUrl();

        return defaultJsonWebClientBuilder().baseUrl(pdfRasterBaseUrl).defaultHeaders(httpHeaders -> {
            httpHeaders.set(safeStorageEndpointProperties.clientHeaderName(),pdfRasterEndpointProperties.clientHeaderValue());
            httpHeaders.set(safeStorageEndpointProperties.apiKeyHeaderName(),pdfRasterEndpointProperties.clientHeaderApiKey());
        }).build();
    }

    private WebClient defaultConsolidatoreWebClient(ConsolidatoreEndpointProperties consolidatoreEndpointProperties) throws Exception {
        return defaultJsonWebClientBuilder().baseUrl(consolidatoreEndpointProperties.baseUrl()).defaultHeaders(httpHeaders -> {
            httpHeaders.set(consolidatoreEndpointProperties.clientHeaderName(), consolidatoreEndpointProperties.clientHeaderValue());
            httpHeaders.set(consolidatoreEndpointProperties.apiKeyHeaderName(), consolidatoreEndpointProperties.apiKeyHeaderValue());
        }).build();
    }

    private WebClient trustAllConsolidatoreWebClient(ConsolidatoreEndpointProperties consolidatoreEndpointProperties) throws Exception {
        return trustAllJsonWebClientBuilder().baseUrl(consolidatoreEndpointProperties.baseUrl()).defaultHeaders(httpHeaders -> {
            httpHeaders.set(consolidatoreEndpointProperties.clientHeaderName(), consolidatoreEndpointProperties.clientHeaderValue());
            httpHeaders.set(consolidatoreEndpointProperties.apiKeyHeaderName(), consolidatoreEndpointProperties.apiKeyHeaderValue());
        }).build();
    }

}
