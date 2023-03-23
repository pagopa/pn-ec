package it.pagopa.pn.ec.consolidatore.service;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ss.SafeStorageEndpointProperties;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic400ErrorException;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
public class PushAttachmentsPreloadTest {

    @Autowired
    private WebTestClient webTestClient;
    @MockBean
    private FileCall fileCall;
    @Autowired
    private SafeStorageEndpointProperties safeStorageEndpointProperties;
    private static final String BAD_CONTENT_TYPE = "BAD_CONTENT_TYPE";
    private static final String DOC_TYPE = "PN_EXTERNAL_LEGAL_FACTS";

    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String X_API_KEY = "X_API_KEY";

    private static final String URI = "/consolidatore-ingress/v1/attachement-preload";

    private static final PreLoadRequest preLoadRequest = new PreLoadRequest();

    @BeforeAll
    public static void buildPreLoadRequest() {
        preLoadRequest.setPreloadIdx("ID_TEST");
        preLoadRequest.setContentType("application/pdf");
        preLoadRequest.setSha256("BD94760347BABBB0B12ADFEB41FF01B90DD7F4C16F9B5");
    }

    private WebTestClient.ResponseSpec pushAttachmentsPreloadTestCall(BodyInserter<PreLoadRequestData, ReactiveHttpOutputMessage> bodyInserter) {

        return this.webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(URI).build())
                .header("x-pagopa-extch-service-id", CLIENT_ID)
                .header(safeStorageEndpointProperties.apiKeyHeaderName(), X_API_KEY)
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .body(bodyInserter)
                .exchange();
    }

    @Test
    void pushAttachmentsOk() {
        when(fileCall.postFile(anyString(), anyString(), any(FileCreationRequest.class))).thenReturn(Mono.just(new FileCreationResponse()));

        PreLoadRequestData preLoadRequestSchema = new PreLoadRequestData();
        preLoadRequestSchema.getPreloads().add(preLoadRequest);

        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isOk();
    }

    @Test
    void testEmptyPreloadsBadRequest() {
        when(fileCall.postFile(anyString(), anyString(), any(FileCreationRequest.class))).thenReturn(Mono.just(new FileCreationResponse()));

        PreLoadRequestData preLoadRequestSchema = new PreLoadRequestData();

        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void testContentTypeBadRequest() {
        PreLoadRequestData preLoadRequestSchema = new PreLoadRequestData();

        PreLoadRequest preLoadRequest1 = new PreLoadRequest();
        preLoadRequest1.setSha256("");
        preLoadRequest1.setContentType("BAD_CONTENT_TYPE");
        preLoadRequest1.setPreloadIdx(preLoadRequest.getPreloadIdx());
        preLoadRequestSchema.getPreloads().add(preLoadRequest1);

        FileCreationRequest fileCreationRequest = new FileCreationRequest();
        fileCreationRequest.setContentType(BAD_CONTENT_TYPE);
        fileCreationRequest.setDocumentType(DOC_TYPE);
        fileCreationRequest.setStatus("");
        fileCreationRequest.setChecksumValue(preLoadRequest1.getSha256());

        when(fileCall.postFile(CLIENT_ID, X_API_KEY, fileCreationRequest)).thenReturn(Mono.error(new Generic400ErrorException("Bad Request", "Bad Request")));

        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void testChecksumBadRequest() {
        PreLoadRequestData preLoadRequestSchema = new PreLoadRequestData();

        PreLoadRequest preLoadRequest1 = new PreLoadRequest();
        preLoadRequest1.setSha256("");
        preLoadRequest1.setContentType(preLoadRequest.getContentType());
        preLoadRequest1.setPreloadIdx(preLoadRequest.getPreloadIdx());
        preLoadRequestSchema.getPreloads().add(preLoadRequest1);

        FileCreationRequest fileCreationRequest = new FileCreationRequest();
        fileCreationRequest.setContentType(preLoadRequest1.getContentType());
        fileCreationRequest.setDocumentType(DOC_TYPE);
        fileCreationRequest.setStatus("");
        fileCreationRequest.setChecksumValue("");

        when(fileCall.postFile(CLIENT_ID, X_API_KEY, fileCreationRequest)).thenReturn(Mono.error(new Generic400ErrorException("Bad Request", "Bad Request")));

        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isBadRequest();
    }

}
