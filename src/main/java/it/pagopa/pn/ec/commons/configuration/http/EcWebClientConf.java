package it.pagopa.pn.ec.commons.configuration.http;

import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
@Slf4j
public class EcWebClientConf {

    @Value("${ec.internal.endpoint.base.path}")
    String ecInternalEndpointBasePath;

    @Value("${ss.internal.endpoint.safe.storage}")
    String ssExternalEndpointBasePath;

    private final ExchangeFilterFunction errorResponseFilter =
            ExchangeFilterFunction.ofResponseProcessor(EcWebClientConf::exchangeFilterResponseProcessor);

    @Bean
    @Qualifier("ecInternalWebClient")
    public WebClient ecInternalWebClient(JettyHttpClientConf jettyHttpClientConf) {
        return WebClient.builder()
                        .clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()))
                        .baseUrl(ecInternalEndpointBasePath)
                        .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .filter(errorResponseFilter)
                        .build();
    }

    @Bean
    @Qualifier("ssExternalWebClient")
    public WebClient ssExternalWebClient(JettyHttpClientConf jettyHttpClientConf) {
        return WebClient.builder()
                .clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()))
                .baseUrl(ssExternalEndpointBasePath)
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .filter(errorResponseFilter)
                .build();
    }

    private static Mono<ClientResponse> exchangeFilterResponseProcessor(ClientResponse response) {
        String statusCodeResponse = String.valueOf(response.statusCode().value());
        if (!statusCodeResponse.startsWith("2")) {
            return response.bodyToMono(Problem.class).ofType(Problem.class).handle((problem, sink) -> {
                if (problem == null) {
                    sink.error(new EcInternalEndpointHttpException(statusCodeResponse));
                }
            }).thenReturn(response);
        } else {
            return Mono.just(response);
        }
    }
}
