package it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.consolidatore.PaperMessagesEndpointProperties;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.consolidatore.utils.PaperResult;
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

import java.util.List;
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
    void testPutRequestSyntaxErrorCode() {
        String receiverCityField = "receiverCity mandatory";
        OperationResultCodeResponse errorResponse = new OperationResultCodeResponse()
                .resultCode(PaperResult.SYNTAX_ERROR_CODE)
                .resultDescription(PaperResult.SYNTAX_ERROR_DESCRIPTION)
                .errorList(List.of(receiverCityField));

        PaperEngageRequest request = new PaperEngageRequest();

        // Endpoint mock
        when(paperMessagesEndpointProperties.putRequest()).thenReturn("/test-uri");

        // Full mocking WebClient chain
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/test-uri")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any(PaperEngageRequest.class))).thenReturn(requestHeadersSpec);

        // Mock exchangeToMono con la giusta classe di ritorno
        when(requestHeadersSpec.exchangeToMono(any())).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<OperationResultCodeResponse>> responseHandler = invocation.getArgument(0);

            // Simulate ClientResponse status 400
            when(clientResponse.statusCode()).thenReturn(HttpStatus.BAD_REQUEST);

            // simulate OperationResultCodeResponse response
            when(clientResponse.bodyToMono(OperationResultCodeResponse.class)).thenReturn(Mono.just(errorResponse));

            return responseHandler.apply(clientResponse);
        });

        Mono<OperationResultCodeResponse> result = paperMessageCallImpl.putRequest(request);

        StepVerifier.create(result)
                    .expectNextMatches(response ->
                                               PaperResult.SYNTAX_ERROR_CODE.equals(response.getResultCode()) &&
                                               PaperResult.SYNTAX_ERROR_DESCRIPTION.equals(response.getResultDescription()) &&
                                               response.getErrorList().contains(receiverCityField)
                                      )
                    .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPutRequestSuccess() {
        OperationResultCodeResponse successResponse = new OperationResultCodeResponse()
                .resultCode("200.00")
                .resultDescription("Success");

        PaperEngageRequest request = new PaperEngageRequest();

        when(paperMessagesEndpointProperties.putRequest()).thenReturn("/test-uri");
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/test-uri")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any(PaperEngageRequest.class))).thenReturn(requestHeadersSpec);

        when(requestHeadersSpec.exchangeToMono(any())).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<OperationResultCodeResponse>> responseHandler = invocation.getArgument(0);
            when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);
            when(clientResponse.bodyToMono(OperationResultCodeResponse.class)).thenReturn(Mono.just(successResponse));
            return responseHandler.apply(clientResponse);
        });

        StepVerifier.create(paperMessageCallImpl.putRequest(request))
                    .expectNextMatches(response ->
                                               "200.00".equals(response.getResultCode()) &&
                                               "Success".equals(response.getResultDescription()))
                    .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPutRequestRetryableError() {
        OperationResultCodeResponse errorResponse = new OperationResultCodeResponse()
                .resultCode("400.99") // Error code doesn't present in non-retryable errors
                .resultDescription("Generic client error");

        PaperEngageRequest request = new PaperEngageRequest();

        when(paperMessagesEndpointProperties.putRequest()).thenReturn("/test-uri");
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/test-uri")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any(PaperEngageRequest.class))).thenReturn(requestHeadersSpec);

        when(requestHeadersSpec.exchangeToMono(any())).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<OperationResultCodeResponse>> responseHandler = invocation.getArgument(0);
            when(clientResponse.statusCode()).thenReturn(HttpStatus.BAD_REQUEST);
            when(clientResponse.bodyToMono(OperationResultCodeResponse.class)).thenReturn(Mono.just(errorResponse));
            return responseHandler.apply(clientResponse);
        });

        StepVerifier.create(paperMessageCallImpl.putRequest(request))
                    .expectErrorMatches(throwable ->
                                                throwable instanceof RestCallException &&
                                                throwable.getMessage().contains("Errore HTTP: 400"))
                    .verify();
    }
}
