package it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import it.pagopa.pn.ec.commons.exception.cartaceo.ConsolidatoreException;
import it.pagopa.pn.ec.consolidatore.utils.PaperResult;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTestWebEnv
@Slf4j
class PaperMessageCallImplTest {
    @Autowired
    private PaperMessageCall paperMessageCall;
    @MockitoSpyBean
    RateLimiter rateLimiter;
    private static MockWebServer mockBackEnd;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry r) {
        // Overriding of internal base url property to point to mock server
        r.add("internal-endpoint.consolidatore.base-path", () -> "http://localhost:" + mockBackEnd.getPort());
    }

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
    }

    @BeforeEach
    void resetRateLimiterAndSemaphore() throws Exception {
        // reset
        RateLimiter newRateLimiter = RateLimiter.of("testLimiter",
                RateLimiterConfig.custom()
                        .limitForPeriod(2)
                        .limitRefreshPeriod(Duration.ofSeconds(5))
                        .timeoutDuration(Duration.ZERO)
                        .build());

        java.lang.reflect.Field field = paperMessageCall.getClass().getDeclaredField("rateLimiter");
        field.setAccessible(true);
        field.set(paperMessageCall, newRateLimiter);
        // reset
        java.lang.reflect.Field semField = paperMessageCall.getClass().getDeclaredField("semaphore");
        semField.setAccessible(true);
        semField.set(paperMessageCall, new Semaphore(2));
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @AfterEach
    public void afterEach() {
        // Setting default dispatcher after every test
        mockBackEnd.setDispatcher(new QueueDispatcher());
    }

    @Test
    void testRateLimiterProperty() {
        log.info("RateLimiterConfig: {}", rateLimiter.getRateLimiterConfig());
        log.info("RateLimiter max requests: {}", rateLimiter.getRateLimiterConfig().getLimitForPeriod());
        log.info("RateLimiter refresh period (seconds): {}", rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod().getSeconds());
    }


    @Test
    void testPutRequestSuccess() {
        OperationResultCodeResponse operationResultCodeResponse = new OperationResultCodeResponse()
                .resultCode("200.00")
                .resultDescription("Success");

        mockBackEnd.enqueue(buildMockResponse(operationResultCodeResponse));

        StepVerifier.create(paperMessageCall.putRequest(new PaperEngageRequest()))
                    .expectNextMatches(response ->
                                               "200.00".equals(response.getResultCode()) &&
                                               "Success".equals(response.getResultDescription()))
                    .verifyComplete();
    }

    @Test
    void testPutRequestPermanentError() {
        //GIVEN
        String receiverCityField = "receiverCity mandatory";
        OperationResultCodeResponse operationResultCodeResponse = new OperationResultCodeResponse()
                .resultCode(PaperResult.SYNTAX_ERROR_CODE)
                .resultDescription(PaperResult.SYNTAX_ERROR_DESCRIPTION)
                .errorList(List.of(receiverCityField));

        mockBackEnd.enqueue(buildMockResponse(operationResultCodeResponse, 400));

        //WHEN
        Mono<OperationResultCodeResponse> result = paperMessageCall.putRequest(new PaperEngageRequest());

        //THEN
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        PaperResult.SYNTAX_ERROR_CODE.equals(response.getResultCode()) &&
                                PaperResult.SYNTAX_ERROR_DESCRIPTION.equals(response.getResultDescription()) &&
                                response.getErrorList().contains(receiverCityField)
                )
                .verifyComplete();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testPutRequestPermanentError_EmptyResultCode(String resultCode) {
        OperationResultCodeResponse operationResultCodeResponse = new OperationResultCodeResponse()
                .resultCode(resultCode) // Error code is null or empty
                .resultDescription("Generic client error");

        mockBackEnd.enqueue(buildMockResponse(operationResultCodeResponse, 400));

        StepVerifier.create(paperMessageCall.putRequest(new PaperEngageRequest()))
                    .expectErrorMatches(throwable -> throwable instanceof ConsolidatoreException.PermanentException)
                    .verify();
    }

    @Test
    void testPutRequestTemporaryError() {
        OperationResultCodeResponse operationResultCodeResponse = new OperationResultCodeResponse()
                .resultCode("500.00") // Error code is null or empty
                .resultDescription("Generic client error");

        mockBackEnd.enqueue(buildMockResponse(operationResultCodeResponse, 500));

        StepVerifier.create(paperMessageCall.putRequest(new PaperEngageRequest()))
                .expectErrorMatches(throwable -> throwable instanceof ConsolidatoreException.TemporaryException)
                .verify();
    }



    @SneakyThrows
    private <T> MockResponse buildMockResponse(T body, int code) {
        return new MockResponse().setBody(objectMapper.writeValueAsString(body)).setResponseCode(code).addHeader("Content-Type", "application/json");
    }

    @SneakyThrows
    private <T> MockResponse buildMockResponse(T body) {
        return new MockResponse().setBody(objectMapper.writeValueAsString(body)).setResponseCode(200).addHeader("Content-Type", "application/json");
    }
}
