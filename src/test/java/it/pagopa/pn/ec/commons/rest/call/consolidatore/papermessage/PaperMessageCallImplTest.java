package it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.consolidatore.PaperMessagesEndpointProperties;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperMessageCallImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private ClientResponse clientResponse;

    @Mock
    private PaperMessagesEndpointProperties paperMessagesEndpointProperties;

    private PaperMessageCallImpl paperMessageCallImpl;

    @BeforeEach
    void setup() {
        paperMessageCallImpl = new PaperMessageCallImpl(webClient, paperMessagesEndpointProperties);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPutRequestSyntaxError40001() {
        String errorResponseJson = "{\"resultCode\":\"400.01\",\"resultDescription\":\"Syntax Error\",\"errorList\":[\"receiverCity obbligatorio\"]}";
        PaperEngageRequest request = new PaperEngageRequest();

        // endpoint mock
        when(paperMessagesEndpointProperties.putRequest()).thenReturn("/test-uri");

        // full mocking WebClient chain
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/test-uri")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any(PaperEngageRequest.class))).thenReturn(requestHeadersSpec);

        // mock exchangeToMono con wildcard corretto
        when(requestHeadersSpec.exchangeToMono(any())).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<OperationResultCodeResponse>> responseHandler = invocation.getArgument(0);

            // simulate ClientResponse status 400
            when(clientResponse.statusCode()).thenReturn(HttpStatus.BAD_REQUEST);
            when(clientResponse.bodyToMono(String.class)).thenReturn(Mono.just(errorResponseJson));

            return responseHandler.apply(clientResponse);
        });

        Mono<OperationResultCodeResponse> result = paperMessageCallImpl.putRequest(request);

        StepVerifier.create(result)
                    .expectNextMatches(response ->
                                               "400.01".equals(response.getResultCode()) &&
                                               "Syntax Error".equals(response.getResultDescription()) &&
                                               response.getErrorList().contains("receiverCity obbligatorio")
                                      )
                    .verifyComplete();
    }
}
