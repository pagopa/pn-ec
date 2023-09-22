package it.pagopa.pn.ec.consolidatore.service;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.consolidatore.ConsolidatoreEndpointProperties;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ss.SafeStorageEndpointProperties;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic400ErrorException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.apache.commons.compress.archivers.sevenz.CLI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

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
    private GestoreRepositoryCall gestoreRepositoryCall;
    @Autowired
    private ConsolidatoreEndpointProperties consolidatoreEndpointProperties;
    @MockBean
    private AuthService authService;

    private static final String BAD_CONTENT_TYPE = "BAD_CONTENT_TYPE";
    private static final String DOC_TYPE = "PN_EXTERNAL_LEGAL_FACTS";

    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String FILE_KEY = "PN_NOTIFICATION_ATTACHMENTS-1a1f2a2430a4494e96d39081c132d21c";
    private static final String X_API_KEY = "X_API_KEY";
    private static final String X_CHECKSUM_VALUE = "dffe706eb6fd101590f88f4f02e07f6bb6940c7a3998ff6";
    private static final String APPLICATION_PDF = "application/pdf";

    private static final String URI = "/consolidatore-ingress/v1/attachment-preload";
    private static final String URI_GET = "/consolidatore-ingress/v1/get-attachment/" + FILE_KEY;

    private static final PreLoadRequest preLoadRequest = new PreLoadRequest();
    private static final ClientConfigurationInternalDto clientConfigurationInternalDto = new ClientConfigurationInternalDto();
    private static final ClientConfigurationInternalDto clientConfigurationInternalDtoWithWrongApiKey = new ClientConfigurationInternalDto();


    @BeforeAll
    public static void buildPreLoadRequest() {
        preLoadRequest.setPreloadIdx(CLIENT_ID);
        preLoadRequest.setContentType("application/pdf");
        preLoadRequest.setSha256(X_CHECKSUM_VALUE);
    }

    @BeforeAll
    public static void buildClientConfigurationInternalDto() {
        clientConfigurationInternalDto.setApiKey(X_API_KEY);
        clientConfigurationInternalDto.setxPagopaExtchCxId(CLIENT_ID);
    }

    @BeforeAll
    public static void buildClientConfigurationInternalDtoWithWrongApiKey() {
        clientConfigurationInternalDtoWithWrongApiKey.setApiKey("TEST");
        clientConfigurationInternalDtoWithWrongApiKey.setxPagopaExtchCxId(CLIENT_ID);
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

    private WebTestClient.ResponseSpec getFileTestCall() {

        return this.webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(URI_GET).build())
                .header(consolidatoreEndpointProperties.clientHeaderName(), CLIENT_ID)
                .header(consolidatoreEndpointProperties.apiKeyHeaderName(), X_API_KEY)
                .accept(APPLICATION_JSON)
                .exchange();
    }

    @Test
    void pushAttachmentsOk() {
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(fileCall.postFile(eq(CLIENT_ID), eq(X_API_KEY), eq(X_CHECKSUM_VALUE), anyString(), any(FileCreationRequest.class))).thenReturn(Mono.just(new FileCreationResponse()));

        PreLoadRequestData preLoadRequestSchema = new PreLoadRequestData();
        preLoadRequestSchema.getPreloads().add(preLoadRequest);

        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isOk();
    }

    @ParameterizedTest
    @MethodSource("providePreLoadRequest")
    void pushAttachmentsBadPreLoadRequestKo(String preloadIdx, String contentType, String sha256) {
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(fileCall.postFile(eq(CLIENT_ID), eq(X_API_KEY), eq(X_CHECKSUM_VALUE), anyString(), any(FileCreationRequest.class))).thenReturn(Mono.just(new FileCreationResponse()));

        PreLoadRequestData preLoadRequestSchema = new PreLoadRequestData();
        PreLoadRequest request = new PreLoadRequest();
        request.setPreloadIdx(preloadIdx);
        request.setContentType(contentType);
        request.setSha256(sha256);
        preLoadRequestSchema.getPreloads().add(request);

        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void pushAttachmentsInvalidApiKey() {
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDtoWithWrongApiKey));
        when(fileCall.postFile(eq(CLIENT_ID), eq(X_API_KEY), eq(X_CHECKSUM_VALUE), anyString(), any(FileCreationRequest.class))).thenReturn(Mono.just(new FileCreationResponse()));

        PreLoadRequestData preLoadRequestSchema = new PreLoadRequestData();
        preLoadRequestSchema.getPreloads().add(preLoadRequest);

        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .is4xxClientError();
    }

    @Test
    void testEmptyPreloadsBadRequest() {
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
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
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));

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
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));

        pushAttachmentsPreloadTestCall(BodyInserters.fromValue(preLoadRequestSchema))
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void getFileOk() {
        when(fileCall.getFile(eq(FILE_KEY), eq(CLIENT_ID), eq(X_API_KEY), anyString())).thenReturn(Mono.just(new FileDownloadResponse()));
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        getFileTestCall().expectStatus().isOk();
    }

    @Test
    void getFileInvalidApiKey() {
        when(fileCall.getFile(eq(FILE_KEY), eq(CLIENT_ID), eq(X_API_KEY), anyString())).thenReturn(Mono.just(new FileDownloadResponse()));
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDtoWithWrongApiKey));
        getFileTestCall().expectStatus().is4xxClientError();
    }

    private static Stream<Arguments> providePreLoadRequest() {
        return Stream.of(Arguments.of(null, APPLICATION_PDF, X_CHECKSUM_VALUE), Arguments.of(CLIENT_ID, null, X_CHECKSUM_VALUE), Arguments.of(CLIENT_ID, APPLICATION_PDF, null));
    }

}
