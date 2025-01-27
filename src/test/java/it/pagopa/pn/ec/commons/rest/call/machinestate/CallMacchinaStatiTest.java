package it.pagopa.pn.ec.commons.rest.call.machinestate;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.exception.StateMachineServiceException;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiDecodeResponseDto;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

@SpringBootTestWebEnv
class CallMacchinaStatiTest {

    private static MockWebServer mockBackEnd;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String CLIENT_ID = "clientId";
    private static final String PROCESS_ID = "requestIdx";
    private static final String CURRENT_STATUS = "currentStatus";
    private static final String NEXT_STATUS = "nextStatus";
    @Autowired
    private CallMacchinaStati callMacchinaStati;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry r) {
        // Overriding of internal base url property to point to mock server
        r.add("internal-endpoint.state-machine.container-base-url", () -> "http://localhost:" + mockBackEnd.getPort());
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
    public void afterEach() {
        // Setting default dispatcher after every test
        mockBackEnd.setDispatcher(new QueueDispatcher());
    }

    @Test
    void statusValidationOk() {
        //GIVEN
        MacchinaStatiValidateStatoResponseDto response = new MacchinaStatiValidateStatoResponseDto();
        response.setAllowed(true);

        //WHEN
        mockBackEnd.enqueue(buildMockResponse(response));

        //THEN
        Mono<MacchinaStatiValidateStatoResponseDto> statusValidation = callMacchinaStati.statusValidation(CLIENT_ID, PROCESS_ID, CURRENT_STATUS, NEXT_STATUS);
        StepVerifier.create(statusValidation).expectNextMatches(MacchinaStatiValidateStatoResponseDto::isAllowed).verifyComplete();
    }

    @Test
    void statusValidationKo() {
        //GIVEN
        MacchinaStatiValidateStatoResponseDto response = new MacchinaStatiValidateStatoResponseDto();
        response.setAllowed(true);

        //WHEN
        Dispatcher mDispatcher = new Dispatcher() {
            @Override
            public @NotNull MockResponse dispatch(@NotNull RecordedRequest request) {
                return buildMockResponse().setResponseCode(500);
            }
        };
        mockBackEnd.setDispatcher(mDispatcher);

        //THEN
        Mono<MacchinaStatiValidateStatoResponseDto> statusValidation = callMacchinaStati.statusValidation(CLIENT_ID, PROCESS_ID, CURRENT_STATUS, NEXT_STATUS);
        StepVerifier.create(statusValidation).expectError(StateMachineServiceException.class).verify();
    }

    @Test
    void statusDecodeOk() {
        //GIVEN
        MacchinaStatiDecodeResponseDto response = new MacchinaStatiDecodeResponseDto();
        response.setExternalStatus(NEXT_STATUS);

        //WHEN
        mockBackEnd.enqueue(buildMockResponse(response));

        //THEN
        Mono<MacchinaStatiDecodeResponseDto> statusValidation = callMacchinaStati.statusDecode(CLIENT_ID, PROCESS_ID, CURRENT_STATUS);
        StepVerifier.create(statusValidation)
                .expectNextMatches(macchinaStatiDecodeResponseDto -> macchinaStatiDecodeResponseDto.getExternalStatus().equals(NEXT_STATUS))
                .verifyComplete();
    }

    @Test
    void statusDecodeKo() {
        //WHEN
        Dispatcher mDispatcher = new Dispatcher() {
            @Override
            public @NotNull MockResponse dispatch(@NotNull RecordedRequest request) {
                return buildMockResponse().setResponseCode(500);
            }
        };
        mockBackEnd.setDispatcher(mDispatcher);

        //THEN
        Mono<MacchinaStatiDecodeResponseDto> statusValidation = callMacchinaStati.statusDecode(CLIENT_ID, PROCESS_ID, CURRENT_STATUS);
        StepVerifier.create(statusValidation).expectError(StateMachineServiceException.class).verify();
    }

    @SneakyThrows
    private <T> MockResponse buildMockResponse(T body) {
        return new MockResponse().setBody(objectMapper.writeValueAsString(body)).addHeader("Content-Type", "application/json");
    }

    @SneakyThrows
    private MockResponse buildMockResponse() {
        return buildMockResponse("{}");
    }

}
