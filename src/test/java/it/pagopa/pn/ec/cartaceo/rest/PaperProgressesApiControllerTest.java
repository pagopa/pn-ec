package it.pagopa.pn.ec.cartaceo.rest;

import it.pagopa.pn.ec.commons.exception.ClientNotAuthorizedException;
import it.pagopa.pn.ec.commons.exception.cartaceo.ConsolidatoreException;
import it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperDeliveryProgressesResponse;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperProgressStatusEvent;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperProgressStatusEventAttachmentsInner;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationInternalDto;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient(timeout = "50000")
class PaperProgressesApiControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PaperMessageCall paperMessageCall;

    private static final String GET_PAPER_PROGRESSES_ENDPOINT = "/external-channels/v1/paper-deliveries-progresses" + "/{requestIdx}";
    private static final ClientConfigurationInternalDto clientConfigurationInternalDto = new ClientConfigurationInternalDto();

    private WebTestClient.ResponseSpec getPaperProgressesTestCall(String requestIdx) {
        return this.webTestClient.get()
                .uri(UriComponentsBuilder.fromPath(GET_PAPER_PROGRESSES_ENDPOINT).build(requestIdx).toString())
                .accept(APPLICATION_JSON)
                .header(ID_CLIENT_HEADER_NAME, DEFAULT_ID_CLIENT_HEADER_VALUE)
                .exchange();
    }

    private PaperDeliveryProgressesResponse buildProgressesResponse() {
        var attachment = new PaperProgressStatusEventAttachmentsInner();
        attachment.setId("1");
        attachment.setDocumentId("documentId");
        attachment.setDocumentType("Plico");
        attachment.setUri("safestorage://prova.pdf");
        attachment.setSha256("stringstringstringstringstringstringstri");
        attachment.setDate(OffsetDateTime.now());

        var event = new PaperProgressStatusEvent();
        event.setRequestId(DEFAULT_REQUEST_IDX);
        event.setStatusCode("CON080");
        event.setStatusDescription("Stampato");
        event.setStatusDateTime(OffsetDateTime.now());
        event.setProductType("AR");
        event.setClientRequestTimeStamp(OffsetDateTime.now());
        event.setAttachments(List.of(attachment));

        var response = new PaperDeliveryProgressesResponse();
        response.setRequestId(DEFAULT_REQUEST_IDX);
        response.setEvents(List.of(event));
        return response;
    }

    @Test
    void getPaperEngageProgressesOk() {

        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(paperMessageCall.getProgress(anyString())).thenReturn(Mono.just(buildProgressesResponse()));

        getPaperProgressesTestCall(DEFAULT_REQUEST_IDX).expectStatus()
                .isOk()
                .expectBody(PaperDeliveryProgressesResponse.class)
                .value(response -> {
                    assertEquals(DEFAULT_REQUEST_IDX,
                            response.getRequestId());
                    assertEquals(1, response.getEvents().size());
                    assertEquals("documentId",
                            response.getEvents()
                                    .getFirst()
                                    .getAttachments()
                                    .getFirst()
                                    .getDocumentId());
                });
    }

    @Test
    void getPaperEngageProgressesOkNoEvents() {

        var emptyResponse = new PaperDeliveryProgressesResponse();
        emptyResponse.setRequestId(DEFAULT_REQUEST_IDX);
        emptyResponse.setEvents(List.of());

        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(paperMessageCall.getProgress(anyString())).thenReturn(Mono.just(emptyResponse));

        getPaperProgressesTestCall(DEFAULT_REQUEST_IDX).expectStatus()
                .isOk()
                .expectBody(PaperDeliveryProgressesResponse.class)
                .value(response -> assertTrue(response.getEvents()
                        .isEmpty()));
    }

    @Test
    void getPaperEngageProgressesRequestNotFound() {

        var operationResult = new OperationResultCodeResponse().resultCode("404.01").resultDescription("requestId never sent");

        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(paperMessageCall.getProgress(anyString())).thenReturn(Mono.error(new ConsolidatoreException.RequestIdNotFoundException(
                operationResult)));

        getPaperProgressesTestCall(DEFAULT_REQUEST_IDX).expectStatus()
                                                       .isNotFound()
                                                       .expectBody(OperationResultCodeResponse.class)
                                                       .value(response -> {
                                                           assertEquals("404.01", response.getResultCode());
                                                           assertEquals("requestId never sent", response.getResultDescription());
                                                       });
    }

    @Test
    void getPaperEngageProgressesConsolidatoreAuthenticationFailed() {

        var operationResult = new OperationResultCodeResponse().resultCode("401.00").resultDescription("Authentication Failed");

        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(paperMessageCall.getProgress(anyString())).thenReturn(Mono.error(new ConsolidatoreException.PermanentException(operationResult,
                                                                                                                           UNAUTHORIZED.value())));

        getPaperProgressesTestCall(DEFAULT_REQUEST_IDX).expectStatus()
                                                       .isEqualTo(BAD_GATEWAY)
                                                       .expectBody(Problem.class)
                                                       .value(problem -> {
                                                           assertEquals(BAD_GATEWAY.value(), problem.getStatus());
                                                           assertFalse(problem.getDetail().contains("Authentication Failed"));
                                                       });
    }

    @Test
    void getPaperEngageProgressesConsolidatoreForbidden() {

        var operationResult = new OperationResultCodeResponse().resultCode("403.00").resultDescription("Forbidden");

        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(paperMessageCall.getProgress(anyString())).thenReturn(Mono.error(new ConsolidatoreException.PermanentException(operationResult,
                                                                                                                           FORBIDDEN.value())));

        getPaperProgressesTestCall(DEFAULT_REQUEST_IDX).expectStatus()
                                                       .isEqualTo(BAD_GATEWAY)
                                                       .expectBody(Problem.class)
                                                       .value(problem -> assertEquals(BAD_GATEWAY.value(), problem.getStatus()));
    }

    @Test
    void getPaperEngageProgressesConsolidatoreGenericClientError() {

        var operationResult = new OperationResultCodeResponse().resultCode("400.01").resultDescription("Syntax Error");

        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(paperMessageCall.getProgress(anyString())).thenReturn(Mono.error(new ConsolidatoreException.PermanentException(operationResult,
                                                                                                                           BAD_REQUEST.value())));

        getPaperProgressesTestCall(DEFAULT_REQUEST_IDX).expectStatus()
                                                       .isEqualTo(BAD_GATEWAY)
                                                       .expectBody(Problem.class)
                                                       .value(problem -> assertEquals(BAD_GATEWAY.value(), problem.getStatus()));
    }

    @Test
    void getPaperEngageProgressesConsolidatoreServerError() {

        var operationResult = new OperationResultCodeResponse().resultCode("500.00").resultDescription("Internal server error");

        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(paperMessageCall.getProgress(anyString())).thenReturn(Mono.error(new ConsolidatoreException.TemporaryException(operationResult, INTERNAL_SERVER_ERROR.value())));

        getPaperProgressesTestCall(DEFAULT_REQUEST_IDX).expectStatus()
                                                       .isEqualTo(BAD_GATEWAY)
                                                       .expectBody(Problem.class)
                                                       .value(problem -> assertEquals(BAD_GATEWAY.value(), problem.getStatus()));
    }

    @Test
    void getPaperEngageProgressesConsolidatoreRateLimited() {

        var operationResult = new OperationResultCodeResponse().resultCode("429.00").resultDescription("Too many requests");

        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(paperMessageCall.getProgress(anyString())).thenReturn(Mono.error(new ConsolidatoreException.RateLimitedException(operationResult,
                                                                                                                             Duration.ofSeconds(42))));

        getPaperProgressesTestCall(DEFAULT_REQUEST_IDX).expectStatus()
                                                       .isEqualTo(SERVICE_UNAVAILABLE)
                                                       .expectHeader()
                                                       .valueEquals(RETRY_AFTER, "42")
                                                       .expectBody(Problem.class)
                                                       .value(problem -> assertEquals(SERVICE_UNAVAILABLE.value(), problem.getStatus()));
    }

    @Test
    void getPaperEngageProgressesConsolidatoreTimeout() {

        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(paperMessageCall.getProgress(anyString())).thenReturn(Mono.error(new ConsolidatoreException.CallTimeoutException("PT10S")));

        getPaperProgressesTestCall(DEFAULT_REQUEST_IDX).expectStatus()
                                                       .isEqualTo(GATEWAY_TIMEOUT)
                                                       .expectBody(Problem.class)
                                                       .value(problem -> assertEquals(GATEWAY_TIMEOUT.value(), problem.getStatus()));
    }

    @Test
    void getPaperEngageProgressesConsolidatoreUnreachable() {

        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(paperMessageCall.getProgress(anyString())).thenReturn(Mono.error(new ConsolidatoreException.ConnectionFailedException(
                "Connection refused")));

        getPaperProgressesTestCall(DEFAULT_REQUEST_IDX).expectStatus()
                                                       .isEqualTo(BAD_GATEWAY)
                                                       .expectBody(Problem.class)
                                                       .value(problem -> {
                                                           assertEquals(BAD_GATEWAY.value(), problem.getStatus());
                                                           assertFalse(problem.getDetail().contains("Connection refused"));
                                                       });
    }

    @Test
    void getPaperEngageProgressesUnauthorizedIdClient() {

        when(authService.clientAuth(anyString())).thenReturn(Mono.error(new ClientNotAuthorizedException(DEFAULT_ID_CLIENT_HEADER_VALUE)));

        getPaperProgressesTestCall(DEFAULT_REQUEST_IDX).expectStatus().isForbidden().expectBody(Problem.class);
    }

    @Test
    void getPaperEngageProgressesMalformedRequestIdx() {

        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));

        getPaperProgressesTestCall(BAD_REQUEST_IDX_SHORT).expectStatus().isBadRequest().expectBody(Problem.class);
    }
}
