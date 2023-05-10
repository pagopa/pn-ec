package it.pagopa.pn.ec.consolidatore.service;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.consolidatore.ConsolidatoreEndpointProperties;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ss.SafeStorageEndpointProperties;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic400ErrorException;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.apache.commons.compress.archivers.sevenz.CLI;
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

import static org.mockito.ArgumentMatchers.*;
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
    private ConsolidatoreEndpointProperties consolidatoreEndpointProperties;

    private static final String BAD_CONTENT_TYPE = "BAD_CONTENT_TYPE";
    private static final String DOC_TYPE = "PN_EXTERNAL_LEGAL_FACTS";

    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String X_API_KEY = "";
    private static final String X_CHECKSUM_VALUE = "dffe706eb6fd101590f88f4f02e07f6bb6940c7a3998ff6";

    private static final String URI = "/consolidatore-ingress/v1/attachment-preload";

    private static final PreLoadRequest preLoadRequest = new PreLoadRequest();

    @BeforeAll
    public static void buildPreLoadRequest() {
        preLoadRequest.setPreloadIdx(CLIENT_ID);
        preLoadRequest.setContentType("application/pdf");
        preLoadRequest.setSha256(X_CHECKSUM_VALUE);
    }


    private WebTestClient.ResponseSpec pushAttachmentsPreloadTestCall(BodyInserter<PreLoadRequestData, ReactiveHttpOutputMessage> bodyInserter) {

        return this.webTestClient.put()
                .uri(uriBuilder -> uriBuilder.path(URI).build())
                .header(consolidatoreEndpointProperties.clientHeaderName(), CLIENT_ID)
                .header(consolidatoreEndpointProperties.apiKeyHeaderName(), X_API_KEY)
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .body(bodyInserter)
                .exchange();
    }

    @Test
    void pushAttachmentsOk() {

        when(fileCall.postFile(eq(CLIENT_ID), eq(X_API_KEY), eq(X_CHECKSUM_VALUE), anyString(), any(FileCreationRequest.class))).thenReturn(Mono.just(new FileCreationResponse()));

        PreLoadRequestData preLoadRequestSchema = new PreLoadRequestData();
        preLoadRequestSchema.getPreloads().add(preLoadRequest);

        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isOk();
    }

    @Test
    void testEmptyPreloadsBadRequest() {
        when(fileCall.postFile(eq(CLIENT_ID), eq(X_API_KEY), eq(X_CHECKSUM_VALUE), anyString(), any(FileCreationRequest.class))).thenReturn(Mono.just(new FileCreationResponse()));

        PreLoadRequestData preLoadRequestSchema = new PreLoadRequestData();

        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void testContentTypeBadRequest() {
        PreLoadRequestData preLoadRequestSchema = new PreLoadRequestData();

        PreLoadRequest badPreLoadRequest = new PreLoadRequest();
        badPreLoadRequest.setSha256(preLoadRequest.getSha256());
        badPreLoadRequest.setContentType("BAD_CONTENT_TYPE");
        badPreLoadRequest.setPreloadIdx(preLoadRequest.getPreloadIdx());
        preLoadRequestSchema.getPreloads().add(badPreLoadRequest);

        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void testChecksumBadRequest() {
        PreLoadRequestData preLoadRequestSchema = new PreLoadRequestData();

        PreLoadRequest badPreLoadRequest = new PreLoadRequest();
        badPreLoadRequest.setSha256("");
        badPreLoadRequest.setContentType(preLoadRequest.getContentType());
        badPreLoadRequest.setPreloadIdx(preLoadRequest.getPreloadIdx());
        preLoadRequestSchema.getPreloads().add(badPreLoadRequest);

        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isBadRequest();
    }

}
