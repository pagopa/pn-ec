package it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;

import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;

@SpringBootTestWebEnv
class GestoreRepositoryCallTest {

    private static MockWebServer mockBackEnd;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String CLIENT_ID = "clientId";
    private static final String REQUEST_IDX = "requestIdx";
    private static final String MESSAGE_ID = "messageId";
    @Autowired
    private GestoreRepositoryCall gestoreRepositoryCall;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        System.setProperty("internal-endpoint.ec.container-base-url", String.format("http://localhost:%s", mockBackEnd.getPort()));
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void getRichiestaOk() {
        //GIVEN
        RequestDto requestDto = new RequestDto();

        //WHEN
        mockBackEnd.enqueue(buildMockResponse(requestDto));

        //THEN
        Mono<RequestDto> statusValidation = gestoreRepositoryCall.getRichiesta(CLIENT_ID, REQUEST_IDX);
        StepVerifier.create(statusValidation).expectNext(requestDto).verifyComplete();
    }

    @Test
    void getRichiestaNotFound() {
        //WHEN
        mockBackEnd.enqueue(buildMockResponse().setResponseCode(404));

        //THEN
        Mono<RequestDto> statusValidation = gestoreRepositoryCall.getRichiesta(CLIENT_ID, REQUEST_IDX);
        StepVerifier.create(statusValidation).expectError(RestCallException.ResourceNotFoundException.class).verify();
    }

    @Test
    void insertRichiestaOk() {
        //GIVEN
        RequestDto requestDto = new RequestDto();

        //WHEN
        mockBackEnd.enqueue(buildMockResponse(requestDto));

        //THEN
        Mono<RequestDto> statusValidation = gestoreRepositoryCall.insertRichiesta(requestDto);
        StepVerifier.create(statusValidation).expectNext(requestDto).verifyComplete();
    }

    @Test
    void insertRichiestaBadRequest() {
        //WHEN
        mockBackEnd.enqueue(buildMockResponse().setResponseCode(400));

        //THEN
        Mono<RequestDto> statusValidation = gestoreRepositoryCall.insertRichiesta(new RequestDto());
        StepVerifier.create(statusValidation).expectError(RepositoryManagerException.RequestMalformedException.class).verify();
    }

    @Test
    void insertRichiestaConflict() {
        //GIVEN
        Problem problem = new Problem().detail("test detail");

        //WHEN
        mockBackEnd.enqueue(buildMockResponse(problem).setResponseCode(409));

        //THEN
        Mono<RequestDto> statusValidation = gestoreRepositoryCall.insertRichiesta(new RequestDto());
        StepVerifier.create(statusValidation).expectError(RestCallException.ResourceAlreadyExistsException.class).verify();
    }

    @Test
    void insertRichiestaNoContent() {
        //WHEN
        mockBackEnd.enqueue(buildMockResponse().setResponseCode(204));

        //THEN
        Mono<RequestDto> statusValidation = gestoreRepositoryCall.insertRichiesta(new RequestDto());
        StepVerifier.create(statusValidation).verifyComplete();
    }

    @Test
    void patchRichiestaOk() {
        //GIVEN
        RequestDto requestDto = new RequestDto();

        //WHEN
        mockBackEnd.enqueue(buildMockResponse(requestDto));

        //THEN
        Mono<RequestDto> statusValidation = gestoreRepositoryCall.patchRichiesta(CLIENT_ID, REQUEST_IDX, new PatchDto());
        StepVerifier.create(statusValidation).expectNext(requestDto).verifyComplete();
    }

    @Test
    void patchRichiestaNotFound() {
        //WHEN
        mockBackEnd.enqueue(buildMockResponse().setResponseCode(404));

        //THEN
        Mono<RequestDto> statusValidation = gestoreRepositoryCall.patchRichiesta(CLIENT_ID, REQUEST_IDX, new PatchDto());
        StepVerifier.create(statusValidation).expectError(RestCallException.ResourceNotFoundException.class).verify();
    }

    @Test
    void patchRichiestaBadRequest() {
        //WHEN
        mockBackEnd.enqueue(buildMockResponse().setResponseCode(400));

        //THEN
        Mono<RequestDto> statusValidation = gestoreRepositoryCall.patchRichiesta(CLIENT_ID, REQUEST_IDX, new PatchDto());
        StepVerifier.create(statusValidation).expectError(RepositoryManagerException.RequestMalformedException.class).verify();
    }

    @Test
    void getRequestByMessageIdOk() {
        //GIVEN
        RequestDto requestDto = new RequestDto();

        //WHEN
        mockBackEnd.enqueue(buildMockResponse(requestDto));

        //THEN
        Mono<RequestDto> statusValidation = gestoreRepositoryCall.getRequestByMessageId(MESSAGE_ID);
        StepVerifier.create(statusValidation).expectNext(requestDto).verifyComplete();
    }

    @Test
    void getRequestByMessageIdNotFound() {
        //WHEN
        mockBackEnd.enqueue(buildMockResponse().setResponseCode(404));

        //THEN
        Mono<RequestDto> statusValidation = gestoreRepositoryCall.getRequestByMessageId(MESSAGE_ID);
        StepVerifier.create(statusValidation).expectError(RestCallException.ResourceNotFoundException.class).verify();
    }

    @Test
    void getRequestByMessageIdBadRequest() {
        //WHEN
        mockBackEnd.enqueue(buildMockResponse().setResponseCode(400));

        //THEN
        Mono<RequestDto> statusValidation = gestoreRepositoryCall.getRequestByMessageId(MESSAGE_ID);
        StepVerifier.create(statusValidation).expectError(RestCallException.class).verify();
    }

    @Test
    void insertDiscardedEventsOk() {
        //GIVEN
        DiscardedEventDto discardedEventDto = new DiscardedEventDto();
        List<DiscardedEventDto> discardedEventDtoList = List.of(discardedEventDto);

        //WHEN
        mockBackEnd.enqueue(buildMockResponse(discardedEventDtoList));

        //THEN
        Flux<DiscardedEventDto> statusValidation = gestoreRepositoryCall.insertDiscardedEvents(Flux.fromIterable(discardedEventDtoList));
        StepVerifier.create(statusValidation).expectNext(discardedEventDto).verifyComplete();
    }

    @Test
    void insertDiscardedEventsEmptyFluxOk() {
        //WHEN
        mockBackEnd.enqueue(buildMockResponse(List.of()));

        //THEN
        Flux<DiscardedEventDto> statusValidation = gestoreRepositoryCall.insertDiscardedEvents(Flux.just());
        StepVerifier.create(statusValidation).verifyComplete();
    }

    @Test
    void insertDiscardedEventsBadRequest() {
        //GIVEN
        DiscardedEventDto discardedEventDto = new DiscardedEventDto();
        List<DiscardedEventDto> discardedEventDtoList = List.of(discardedEventDto);

        //WHEN
        mockBackEnd.enqueue(buildMockResponse().setResponseCode(400));

        //THEN
        Flux<DiscardedEventDto> statusValidation = gestoreRepositoryCall.insertDiscardedEvents(Flux.fromIterable(discardedEventDtoList));
        StepVerifier.create(statusValidation).expectError(RepositoryManagerException.RequestMalformedException.class).verify();
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
