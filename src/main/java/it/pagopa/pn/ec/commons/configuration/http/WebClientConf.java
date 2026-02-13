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
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
public class WebClientConf {

    private final String HTTPS="https";
    private final JettyHttpClientConf jettyHttpClientConf;

    public WebClientConf(JettyHttpClientConf jettyHttpClientConf) {
        this.jettyHttpClientConf = jettyHttpClientConf;
    }

    private WebClient.Builder defaultWebClientBuilder() {
        return WebClient.builder().clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()));
    }

    private WebClient.Builder trustAllWebClientBuilder() {
        return WebClient.builder().clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getTrustAllJettyHttpClient()));
    }

    private WebClient.Builder defaultJsonWebClientBuilder() {
        return defaultWebClientBuilder().defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
    }

    private WebClient.Builder trustAllJsonWebClientBuilder() {
        return trustAllWebClientBuilder().defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
    }

    @Bean
    public WebClient ecWebClient(ExternalChannelEndpointProperties externalChannelEndpointProperties) {
        String baseUrl = externalChannelEndpointProperties.containerBaseUrl();
        return defaultJsonWebClientBuilder().baseUrl(baseUrl)
                .uriBuilderFactory(getDisabledEncodingFactory(baseUrl))
                .clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()))
                .build();
    }

    @Bean
    public WebClient ssWebClient(SafeStorageEndpointProperties safeStorageEndpointProperties) {
        String baseUrl = safeStorageEndpointProperties.containerBaseUrl();
        return defaultJsonWebClientBuilder().baseUrl(baseUrl)
                .uriBuilderFactory(getDisabledEncodingFactory(baseUrl))
                .clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()))
                .defaultHeaders(httpHeaders -> {
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
        String baseUrl = stateMachineEndpointProperties.containerBaseUrl();
        return defaultJsonWebClientBuilder().baseUrl(baseUrl)
                .uriBuilderFactory(getDisabledEncodingFactory(baseUrl))
                .clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()))
                .build();
    }

    @Bean
    public WebClient consolidatoreWebClient(ConsolidatoreEndpointProperties consolidatoreEndpointProperties) {
        String consolidatoreBaseUrl = consolidatoreEndpointProperties.baseUrl();

        if (consolidatoreBaseUrl.startsWith(HTTPS) && consolidatoreEndpointProperties.trustAll()) {
            return trustAllConsolidatoreWebClient(consolidatoreEndpointProperties);
        } else return defaultConsolidatoreWebClient(consolidatoreEndpointProperties);
    }

    @Bean
    public WebClient pdfRasterWebClient(PdfRasterEndpointProperties pdfRasterEndpointProperties,SafeStorageEndpointProperties safeStorageEndpointProperties){
        String pdfRasterBaseUrl = pdfRasterEndpointProperties.baseUrl();

        return defaultJsonWebClientBuilder().baseUrl(pdfRasterBaseUrl)
                .uriBuilderFactory(getDisabledEncodingFactory(pdfRasterBaseUrl))
                .clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()))
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.set(safeStorageEndpointProperties.clientHeaderName(),pdfRasterEndpointProperties.clientHeaderValue());
                    httpHeaders.set(safeStorageEndpointProperties.apiKeyHeaderName(),pdfRasterEndpointProperties.clientHeaderApiKey());
                }).build();
    }

    private WebClient defaultConsolidatoreWebClient(ConsolidatoreEndpointProperties consolidatoreEndpointProperties)
    {
        String consolidatoreBaseUrl = consolidatoreEndpointProperties.baseUrl();
        return defaultJsonWebClientBuilder().baseUrl(consolidatoreBaseUrl)
                .uriBuilderFactory(getDisabledEncodingFactory(consolidatoreBaseUrl))
                .clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()))
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.set(consolidatoreEndpointProperties.clientHeaderName(), consolidatoreEndpointProperties.clientHeaderValue());
                    httpHeaders.set(consolidatoreEndpointProperties.apiKeyHeaderName(), consolidatoreEndpointProperties.apiKeyHeaderValue());
                }).build();
    }

    private WebClient trustAllConsolidatoreWebClient(ConsolidatoreEndpointProperties consolidatoreEndpointProperties) {
        String consolidatoreBaseUrl = consolidatoreEndpointProperties.baseUrl();
        return trustAllJsonWebClientBuilder().baseUrl(consolidatoreBaseUrl)
                .uriBuilderFactory(getDisabledEncodingFactory(consolidatoreBaseUrl))
                .clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()))
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.set(consolidatoreEndpointProperties.clientHeaderName(), consolidatoreEndpointProperties.clientHeaderValue());
                    httpHeaders.set(consolidatoreEndpointProperties.apiKeyHeaderName(), consolidatoreEndpointProperties.apiKeyHeaderValue());
                }).build();
    }

    private DefaultUriBuilderFactory getDisabledEncodingFactory(String baseUrl) {
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        return factory;
    }

}
