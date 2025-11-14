package it.pagopa.pn.ec.richiestemetadati;

import it.pagopa.pn.ec.commons.exception.ClientNotAuthorizedException;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import it.pagopa.pn.ec.rest.v1.dto.RequestMetadataPatchRequest;
import it.pagopa.pn.ec.richiestemetadati.service.impl.PaperRequestMetadataPatchServiceImpl;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient(timeout = "50000")
class PaperRequestMetadataPatchControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PaperRequestMetadataPatchServiceImpl paperRequestMetadataPatchService;

    private static final String PATCH_METADATA_ENDPOINT = "/external-channels/v1/paper-request-metadata/{requestIdx}";
    private static final String X_PAGOPA_EXTCH_CX_ID_HEADER = "x-pagopa-extch-cx-id";
    private static final RequestMetadataPatchRequest requestMetadataPatchRequest = new RequestMetadataPatchRequest();

    @BeforeEach
    void setUp() {
        requestMetadataPatchRequest.setIsOpenReworkRequest(true);
    }

    private WebTestClient.ResponseSpec patchMetadataTestCall(
            BodyInserter<RequestMetadataPatchRequest, ReactiveHttpOutputMessage> bodyInserter,
            String requestIdx,
            String clientId) {

        return this.webTestClient.patch()
                .uri(uriBuilder -> uriBuilder.path(PATCH_METADATA_ENDPOINT).build(requestIdx))
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .body(bodyInserter)
                .header(X_PAGOPA_EXTCH_CX_ID_HEADER, clientId)
                .exchange();
    }

    @Test
    void patchRequestMetadataOk() {
        when(paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                anyString(), anyString(), any(RequestMetadataPatchRequest.class)))
                .thenReturn(Mono.empty());

        patchMetadataTestCall(
                BodyInserters.fromValue(requestMetadataPatchRequest),
                DEFAULT_REQUEST_IDX,
                DEFAULT_ID_CLIENT_HEADER_VALUE)
                .expectStatus().isNoContent();
    }

    @Test
    void patchRequestMetadataRequestNotFound() {
        when(paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                anyString(), anyString(), any(RequestMetadataPatchRequest.class)))
                .thenReturn(Mono.error(new RepositoryManagerException.RequestNotFoundException("RequestMetadata not found")));

        patchMetadataTestCall(
                BodyInserters.fromValue(requestMetadataPatchRequest),
                DEFAULT_REQUEST_IDX,
                DEFAULT_ID_CLIENT_HEADER_VALUE)
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class);
    }

    @Test
    void patchRequestMetadataPaperRequestNotPresent() {
        when(paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                anyString(), anyString(), any(RequestMetadataPatchRequest.class)))
                .thenReturn(Mono.error(new RepositoryManagerException.RequestMalformedException("PaperRequestMetadata is null")));

        patchMetadataTestCall(
                BodyInserters.fromValue(requestMetadataPatchRequest),
                DEFAULT_REQUEST_IDX,
                DEFAULT_ID_CLIENT_HEADER_VALUE)
                .expectStatus()
                .isBadRequest()
                .expectBody(Problem.class);
    }

    @Test
    void patchRequestMetadataBadBody() {
        patchMetadataTestCall(
                BodyInserters.empty(),
                DEFAULT_REQUEST_IDX,
                DEFAULT_ID_CLIENT_HEADER_VALUE)
                .expectStatus().isBadRequest()
                .expectBody(Problem.class);
    }

    @Test
    void patchRequestMetadataMalformedRequestIdx() {
        patchMetadataTestCall(
                BodyInserters.fromValue(requestMetadataPatchRequest),
                BAD_REQUEST_IDX_SHORT,
                DEFAULT_ID_CLIENT_HEADER_VALUE)
                .expectStatus().isBadRequest()
                .expectBody(Problem.class);
    }

    @Test
    void patchRequestMetadataUnauthorizedClient() {
        when(paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                anyString(), anyString(), any(RequestMetadataPatchRequest.class)))
                .thenReturn(Mono.error(new ClientNotAuthorizedException(DEFAULT_ID_CLIENT_HEADER_VALUE)));

        patchMetadataTestCall(
                BodyInserters.fromValue(requestMetadataPatchRequest),
                DEFAULT_REQUEST_IDX,
                DEFAULT_ID_CLIENT_HEADER_VALUE)
                .expectStatus().isForbidden();
    }

    @Test
    void patchRequestMetadataServiceError() {
        when(paperRequestMetadataPatchService.patchIsOpenReworkRequest(
                anyString(), anyString(), any(RequestMetadataPatchRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        patchMetadataTestCall(
                BodyInserters.fromValue(requestMetadataPatchRequest),
                DEFAULT_REQUEST_IDX,
                DEFAULT_ID_CLIENT_HEADER_VALUE)
                .expectStatus().is5xxServerError();
    }


}