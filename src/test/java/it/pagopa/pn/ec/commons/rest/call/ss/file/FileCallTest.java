package it.pagopa.pn.ec.commons.rest.call.ss.file;

import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic400ErrorException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.AttachmentNotAvailableException;
import it.pagopa.pn.ec.consolidatore.exception.ClientNotAuthorizedOrFoundException;
import it.pagopa.pn.ec.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.ec.rest.v1.dto.FileCreationResponse;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

@SpringBootTestWebEnv
class FileCallTest {

    public static final String FILE_KEY = "fileKey";
    public static final String CLIENT_ID = "clientId";
    public static final String X_API_KEY = "xApiKey";
    public static final String CHECKSUM_VALUE = "checksumValue";
    public static final String X_TRACE_ID = "xTraceId";
    private static MockWebServer mockBackEnd;

    @Autowired
    private FileCall fileCall;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry r) {
        r.add("internal-endpoint.ss.container-base-url", () -> "http://localhost:" + mockBackEnd.getPort());
    }

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @AfterEach
    void afterEach() {
        // Setting default dispatcher after every test
        mockBackEnd.setDispatcher(new QueueDispatcher());
    }

    @Test
    void getFile_Success() {
        FileDownloadResponse response = new FileDownloadResponse();

        mockBackEnd.enqueue(new MockResponse()
                .setBody("{\"fileContent\":\"file content\"}")
                .addHeader("Content-Type", "application/json"));

        Mono<FileDownloadResponse> fileDownloadResponseMono = fileCall.getFile(FILE_KEY, CLIENT_ID, X_API_KEY, X_TRACE_ID);

        StepVerifier.create(fileDownloadResponseMono)
                .expectNextMatches(fileDownloadResponse -> fileDownloadResponse.equals(response))
                .verifyComplete();
    }

    @Test
    void getFile_Unauthorized() {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(403));

        Mono<FileDownloadResponse> fileDownloadResponseMono = fileCall.getFile(FILE_KEY, CLIENT_ID, X_API_KEY, X_TRACE_ID);

        StepVerifier.create(fileDownloadResponseMono)
                .expectError(ClientNotAuthorizedOrFoundException.class)
                .verify();
    }

    @Test
    void getFile_Generic400() {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(410));

        Mono<FileDownloadResponse> fileDownloadResponseMono = fileCall.getFile(FILE_KEY, CLIENT_ID, X_API_KEY, X_TRACE_ID);

        StepVerifier.create(fileDownloadResponseMono)
                .expectError(Generic400ErrorException.class)
                .verify();
    }

    @Test
    void getFile_NoApiKey_Success() {
        FileDownloadResponse response = new FileDownloadResponse();

        mockBackEnd.enqueue(new MockResponse()
                .setBody("{\"fileContent\":\"file content\"}")
                .addHeader("Content-Type", "application/json"));

        Mono<FileDownloadResponse> fileDownloadResponseMono = fileCall.getFile(FILE_KEY, CLIENT_ID, false);

        StepVerifier.create(fileDownloadResponseMono)
                .expectNextMatches(fileDownloadResponse -> fileDownloadResponse.equals(response))
                .verifyComplete();
    }

    @Test
    void getFile_NoApiKey_NotFound() {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(404));

        Mono<FileDownloadResponse> fileDownloadResponseMono = fileCall.getFile(FILE_KEY, CLIENT_ID, false);

        StepVerifier.create(fileDownloadResponseMono)
                .expectError(AttachmentNotAvailableException.class)
                .verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 410})
    void getFile_NoApiKey_Generic400(int statusCode) {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(statusCode));

        Mono<FileDownloadResponse> fileDownloadResponseMono = fileCall.getFile(FILE_KEY, CLIENT_ID, false);

        StepVerifier.create(fileDownloadResponseMono)
                .expectError(Generic400ErrorException.class)
                .verify();
    }

    @Test
    void postFile_Success() {
        FileCreationRequest request = new FileCreationRequest();
        FileCreationResponse response = new FileCreationResponse();

        mockBackEnd.enqueue(new MockResponse()
                .setBody("{\"fileId\":\"fileId\"}")
                .addHeader("Content-Type", "application/json"));

        Mono<FileCreationResponse> fileCreationResponseMono = fileCall.postFile(CLIENT_ID, X_API_KEY, CHECKSUM_VALUE, X_TRACE_ID, request);

        StepVerifier.create(fileCreationResponseMono)
                .expectNextMatches(fileCreationResponse -> fileCreationResponse.equals(response))
                .verifyComplete();
    }

    @Test
    void postFile_NoApiKey_Success() {
        FileCreationRequest request = new FileCreationRequest();
        FileCreationResponse response = new FileCreationResponse();

        mockBackEnd.enqueue(new MockResponse()
                .setBody("{\"fileId\":\"fileId\"}")
                .addHeader("Content-Type", "application/json"));

        Mono<FileCreationResponse> fileCreationResponseMono = fileCall.postFile(CLIENT_ID, CHECKSUM_VALUE, request);

        StepVerifier.create(fileCreationResponseMono)
                .expectNextMatches(fileCreationResponse -> fileCreationResponse.equals(response))
                .verifyComplete();
    }

}
