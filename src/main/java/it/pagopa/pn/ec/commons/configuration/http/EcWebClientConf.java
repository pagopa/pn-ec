package it.pagopa.pn.ec.commons.configuration.http;

import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
@Slf4j
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
