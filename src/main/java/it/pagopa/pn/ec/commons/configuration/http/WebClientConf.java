package it.pagopa.pn.ec.commons.configuration.http;

import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
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
    private final PnEcConfig pnEcConfig;

    public WebClientConf(JettyHttpClientConf jettyHttpClientConf, PnEcConfig pnEcConfig) {
        this.jettyHttpClientConf = jettyHttpClientConf;
        this.pnEcConfig = pnEcConfig;
    }

    private WebClient.Builder defaultWebClientBuilder(String baseUrl) {
        return WebClient.builder()
                .uriBuilderFactory(getDisabledEncodingFactory(baseUrl))
                .clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()));
    }

    private WebClient.Builder trustAllWebClientBuilder(String baseUrl) {
        return WebClient.builder()
                .uriBuilderFactory(getDisabledEncodingFactory(baseUrl))
                .clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getTrustAllJettyHttpClient()));
    }

    private WebClient.Builder defaultJsonWebClientBuilder(String baseUrl) {
        return defaultWebClientBuilder(baseUrl).defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
    }

    private WebClient.Builder trustAllJsonWebClientBuilder(String baseUrl) {
        return trustAllWebClientBuilder(baseUrl).defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
    }

    @Bean
    public WebClient ecWebClient() {
        String baseUrl = pnEcConfig.getCommons().getEndpoint().getExternalChannel().getContainerBaseUrl();
        return defaultJsonWebClientBuilder(baseUrl)
                .build();
    }

    @Bean
    public WebClient ssWebClient() {
        var safeStorageEndpointProperties = pnEcConfig.getCommons().getEndpoint().getSafeStorage();
        String baseUrl = safeStorageEndpointProperties.getContainerBaseUrl();
        return defaultJsonWebClientBuilder(baseUrl)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.set(safeStorageEndpointProperties.getClientHeaderName(), safeStorageEndpointProperties.getClientHeaderValue());
                    httpHeaders.set(safeStorageEndpointProperties.getApiKeyHeaderName(), safeStorageEndpointProperties.getApiKeyHeaderValue());
                }).build();
    }

//    @Bean
//    public WebClient downloadWebClient(String url) {
//
//        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(url);
//        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
//        return WebClient.builder().uriBuilderFactory(factory).build();
//    }

    @Bean
    public WebClient uploadWebClient() {
        return defaultWebClientBuilder("").build();
    }

    @Bean
    public WebClient stateMachineWebClient() {
        String baseUrl = pnEcConfig.getStateMachine().getEndpoint().getContainerBaseUrl();
        return defaultJsonWebClientBuilder(baseUrl)
                .build();
    }

    @Bean
    public WebClient consolidatoreWebClient() {
        var consolidatoreEndpointProperties = pnEcConfig.getCommons().getEndpoint().getConsolidatore();
        String consolidatoreBaseUrl = consolidatoreEndpointProperties.getBaseUrl();

        if (consolidatoreBaseUrl.startsWith(HTTPS) && Boolean.TRUE.equals(consolidatoreEndpointProperties.getTrustAll())) {
            return trustAllConsolidatoreWebClient(consolidatoreEndpointProperties);
        } else return defaultConsolidatoreWebClient(consolidatoreEndpointProperties);
    }

    @Bean
    public WebClient pdfRasterWebClient(){
        var pdfRasterEndpointProperties = pnEcConfig.getPdfRaster().getEndpoint();
        var safeStorageEndpointProperties = pnEcConfig.getCommons().getEndpoint().getSafeStorage();
        String pdfRasterBaseUrl = pdfRasterEndpointProperties.getBaseUrl();

        return defaultJsonWebClientBuilder(pdfRasterBaseUrl)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.set(safeStorageEndpointProperties.getClientHeaderName(),pdfRasterEndpointProperties.getClientHeaderValue());
                    httpHeaders.set(safeStorageEndpointProperties.getApiKeyHeaderName(),pdfRasterEndpointProperties.getClientHeaderApiKey());
                }).build();
    }

    private WebClient defaultConsolidatoreWebClient(PnEcConfig.Commons.Endpoint.Consolidatore consolidatoreEndpointProperties)
    {
        String consolidatoreBaseUrl = consolidatoreEndpointProperties.getBaseUrl();
        return defaultJsonWebClientBuilder(consolidatoreBaseUrl)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.set(consolidatoreEndpointProperties.getClientHeaderName(), consolidatoreEndpointProperties.getClientHeaderValue());
                    httpHeaders.set(consolidatoreEndpointProperties.getApiKeyHeaderName(), consolidatoreEndpointProperties.getApiKeyHeaderValue());
                }).build();
    }

    private WebClient trustAllConsolidatoreWebClient(PnEcConfig.Commons.Endpoint.Consolidatore consolidatoreEndpointProperties) {
        String consolidatoreBaseUrl = consolidatoreEndpointProperties.getBaseUrl();
        return trustAllJsonWebClientBuilder(consolidatoreBaseUrl)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.set(consolidatoreEndpointProperties.getClientHeaderName(), consolidatoreEndpointProperties.getClientHeaderValue());
                    httpHeaders.set(consolidatoreEndpointProperties.getApiKeyHeaderName(), consolidatoreEndpointProperties.getApiKeyHeaderValue());
                }).build();
    }

    private DefaultUriBuilderFactory getDisabledEncodingFactory(String baseUrl) {
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        return factory;
    }

}
