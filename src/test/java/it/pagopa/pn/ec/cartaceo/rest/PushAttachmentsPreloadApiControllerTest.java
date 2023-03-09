package it.pagopa.pn.ec.cartaceo.rest;

import it.pagopa.pn.ec.cartaceo.service.PushAttachmentPreloadService;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ss.FilesEndpointProperties;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic400ErrorException;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.ID_CLIENT_HEADER_NAME;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
public class PushAttachmentsPreloadApiControllerTest {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private PushAttachmentPreloadService service;
    @MockBean
    private FileCall fileCall;
    @Autowired
    private FilesEndpointProperties filesEndpointProperties;

    private static final String PN_AAR = "PN_AAR";
    private static final String PRELOADED = "PRELOADED";

    private static final String BAD_CONTENT_TYPE = "BAD_CONTENT_TYPE";
    private static final String DOC_TYPE = "PN_EXTERNAL_LEGAL_FACTS";
    private static final String DOC_STATUS = "SAVED";

    private static final String URI = "/consolidatore-ingress/v1/attachement-preload";

    private static final PreLoadRequest preLoadRequest = new PreLoadRequest();

    @BeforeAll
    private static void buildPreLoadRequestCases() {
        preLoadRequest.setPreloadIdx("ID_TEST");
        preLoadRequest.setContentType("CONTENT_TEST");
        preLoadRequest.setSha256("BD94760347BABBB0B12ADFEB41FF01B90DD7F4C16F9B6");
    }

    private WebTestClient.ResponseSpec pushAttachmentsPreloadTestCall(BodyInserter<PreLoadRequestSchema, ReactiveHttpOutputMessage> bodyInserter) {

        return this.webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(URI).build())
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .body(bodyInserter)
                .exchange();
    }

    private PreLoadRequest buildOkPreloadRequest() {
        var preLoadRequest = new PreLoadRequest();
        preLoadRequest.setPreloadIdx("ID_TEST");
        preLoadRequest.setContentType("CONTENT_TEST");
        preLoadRequest.setSha256("BD94760347BABBB0B12ADFEB41FF01B90DD7F4C16F9B6");
        return preLoadRequest;
    }

    @Test
    void pushAttachmentsOk() {
        when(fileCall.postFile(any(FileCreationRequest.class))).thenReturn(Mono.just(new FileCreationResponse()));

        PreLoadRequestSchema preLoadRequestSchema = new PreLoadRequestSchema();
        preLoadRequestSchema.getPreloads().add(preLoadRequest);

        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isOk();
    }

    @Test
    void testEmptyPreloadsBadRequest() {
        when(fileCall.postFile(any(FileCreationRequest.class))).thenReturn(Mono.just(new FileCreationResponse()));

        PreLoadRequestSchema preLoadRequestSchema = new PreLoadRequestSchema();

        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void testContentTypeBadRequest() {
        PreLoadRequestSchema preLoadRequestSchema = new PreLoadRequestSchema();

        var preLoadRequest1 = preLoadRequest;
        preLoadRequest1.setContentType("BAD_CONTENT_TYPE");
        preLoadRequestSchema.getPreloads().add(preLoadRequest1);

        FileCreationRequest fileCreationRequest = new FileCreationRequest();
        fileCreationRequest.setContentType(BAD_CONTENT_TYPE);
        fileCreationRequest.setDocumentType(DOC_TYPE);
        fileCreationRequest.setStatus(DOC_STATUS);


        when(fileCall.postFile(fileCreationRequest)).thenReturn(Mono.error(new Generic400ErrorException("Bad Request", "Bad Request")));


        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isBadRequest();
    }

 /*   @Test
    void testErrorStatus() {

        when(fileCall.postFile(any(FileCreationRequest.class))).thenReturn(Mono.just(new FileCreationResponse()));

        PreLoadRequestSchema preLoadRequestSchema = new PreLoadRequestSchema();
        preLoadRequestSchema.getPreloads().add(preLoadRequest);

        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isBadRequest();
    }


    @Test
    void testErrorDocumentType() {
        when(fileCall.postFile(any(FileCreationRequest.class))).thenReturn(Mono.just(new FileCreationResponse()));
        when(fileCall.postFile(any(FileCreationRequest.class))).

        PreLoadRequestSchema preLoadRequestSchema = new PreLoadRequestSchema();
        preLoadRequestSchema.getPreloads().add(preLoadRequest);

        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isBadRequest();
    }*/


/*    @Test
    void testEmptyPreloadIdxBadRequest() {
        when(fileCall.postFile(any(FileCreationRequest.class))).thenReturn(Mono.just(new FileCreationResponse()));

        PreLoadRequestSchema preLoadRequestSchema = new PreLoadRequestSchema();
        PreLoadRequest preLoadRequestNoIdx = preLoadRequest;
        preLoadRequestNoIdx.setPreloadIdx(null);
        preLoadRequestSchema.getPreloads().add(preLoadRequestNoIdx);


        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void testEmptyContentTypeBadRequest() {
    }



    @Test
    void testEmptySha256BadRequest() {
    }*/


}
