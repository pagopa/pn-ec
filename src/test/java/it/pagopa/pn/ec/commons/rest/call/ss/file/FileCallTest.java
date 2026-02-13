package it.pagopa.pn.ec.commons.rest.call.ss.file;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ss.SafeStorageEndpointProperties;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic400ErrorException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.AttachmentNotAvailableException;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.consolidatore.exception.ClientNotAuthorizedOrFoundException;
import it.pagopa.pn.ec.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.ec.rest.v1.dto.FileCreationResponse;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import lombok.CustomLog;

import java.io.IOException;
import java.io.OutputStream;

@SpringBootTestWebEnv
@CustomLog
class FileCallTest {

    public static final String FILE_KEY = "fileKey";
    public static final String CLIENT_ID = "clientId";
    public static final String X_API_KEY = "xApiKey";
    public static final String CHECKSUM_VALUE = "checksumValue";
    public static final String X_TRACE_ID = "xTraceId";
    private static MockWebServer mockBackEnd;

    @Autowired
    private FileCall fileCall;
    @Autowired
    private DownloadCall downloadCall;
    @Autowired
    private SafeStorageEndpointProperties safeStorageEndpointProperties;

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
        log.info("safeStorageEndpointProperties.containerBaseUrl {}", safeStorageEndpointProperties.containerBaseUrl());

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
//
//    @Test
//    void getDownloadFile_Success() {
//
//        Mono<OutputStream> fileDownloadResponseMono = downloadCall.downloadFile("https://pn-safestorage-eu-south-1-089813480515.s3.eu-south-1.amazonaws.com/PN_NOTIFICATION_ATTACHMENTS-e508b0eadc2e46958330ea7ec111ccf7.pdf?x-amzn-trace-id=62070a0d-4d1b-41a3-b913-2fec5c387bec&X-Amz-Security-Token=IQoJb3JpZ2luX2VjECUaCmV1LXNvdXRoLTEiRzBFAiEAqotCLtdk3KSzjK6Fxwnka7g8QAOYSFopbyLRcnuV%2F%2BMCIEWSjqlMcd%2FkdJG7YZ3GD9v8uKfaZpJXmpdrJO7ZrTbXKpcECO7%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEQABoMMDg5ODEzNDgwNTE1Igx3BzkCIWwhZCDaoR4q6wN3rfhOJUucVVv1OCeznbg3QXu4xYsQx1UBThpMwJtxxqNFLWFaXqhOAoThBOvC44Atoyn24HPzuxC9iMuO3CYSLPP21mEUF5d72zE%2FIMzNHhmfEJ8jlw2tt72771bYpgU0iQA10sw1LX007sQC5Ra8T%2FSo0PNNlA0oe5%2BPf%2F3PnrvRx2TI17CxyWePRM1DHrdIoD8HnloIYlkUlwyFah%2FSIcwdpy9fL%2BMICeHUKY3gJ%2F44l1DT%2BSAKus5rnKtU6s%2FCb273SnfA8Whg1uJHVxIMgHyRpSDv6z6YyH7JtPVXoCd3rpyJerFz3S6ffdSux%2BDU9h3gWkzqp6w%2FRF2LFZfYOZ1XGaG2uU26229%2F5Pz1Fq8MQ%2BqWY2Emjyd6xZZfaOrKMNHBMaa7KmY9UogE5R%2BcLKI3%2F2pltHDGPWDGtkOhRKv2cs0IFFg7sRt3i49dctjgvvglG4Frc0LQUGbrPPiIzx2S9tOe06vH3Jyn%2FZ%2F3ox%2Fr8za2mTYBPWm3YA4G08romIw86tObxE4pAZKKkN3IN%2Bxt3p4ARpkH%2FBs6ptCazierXI7j2Tgw%2F7pFSgKHn0t5h%2FHj782qmiQoKDDLo2VShP6rQQNZlshqJ8IJAJnDfxfrqA4MuXCh8jati%2FzDwhhJURYwEfnuBNg0hzCTxrzMBjqmAfwxlVBAwXo4AWhbm6ofjcgm1spXEYbRiQAJNioN7cWMYiiN03XnUnT2XJIWQIhUIx%2BURP201sMiWkTmD5Y5HSwMS03xqFKPXu04W9eTwEz%2BDYVe%2FofHQxhgDc9YBnGdpxeIFyg5k4GrGk5FRvxVSBVMblMrklMkEmf8EEFenCMl4Dx7eD3NzB0Y7%2F7k7nQgMcV4dG9uyMPvF44CGynUEkt0gpFx%2FTI%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20260213T145034Z&X-Amz-SignedHeaders=host&X-Amz-Expires=3600&X-Amz-Credential=ASIARJ2KM6RBXDGK5WAC%2F20260213%2Feu-south-1%2Fs3%2Faws4_request&X-Amz-Signature=e5c70a8b325cd86f0a8596aa003c426fd07ef13b0485018f6eb5b975eee6185d&x-pagopa-pn-cx-id=pn-delivery%7EPnEcMsCucumberTest-c0fIBAqW2Y6a3jTyVXQoqP5F5kfToX");
//
//    }
}
