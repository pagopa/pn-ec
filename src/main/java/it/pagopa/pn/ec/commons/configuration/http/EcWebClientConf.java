package it.pagopa.pn.ec.commons.configuration.http;

import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
public class EcWebClientConf {

    @Value("${ec.internal.endpoint.base.path}")
    String ecInternalEndpointBasePath;

    private final ExchangeFilterFunction errorResponseFilter =
            ExchangeFilterFunction.ofResponseProcessor(EcWebClientConf::exchangeFilterResponseProcessor);

    @Bean
    public WebClient ecInternalWebClient(JettyHttpClientConf jettyHttpClientConf) {
        return WebClient.builder()
                        .clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()))
                        .baseUrl(ecInternalEndpointBasePath)
                        .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .filter(errorResponseFilter)
                        .build();
    }

    private static Mono<ClientResponse> exchangeFilterResponseProcessor(ClientResponse response) {
        List<HttpStatus> okStatus = List.of(OK, CREATED, ACCEPTED, NO_CONTENT);
        HttpStatus statusCodeResponse = response.statusCode();
        if (!okStatus.contains(statusCodeResponse)) {
            throw new EcInternalEndpointHttpException(statusCodeResponse.value());
        } else {
            return Mono.just(response);
        }
    }
}
