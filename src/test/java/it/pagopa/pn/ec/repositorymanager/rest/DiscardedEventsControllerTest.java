package it.pagopa.pn.ec.repositorymanager.rest;


import it.pagopa.pn.ec.repositorymanager.model.entity.DiscardedEvent;
import it.pagopa.pn.ec.repositorymanager.service.DiscardedEventsService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@AutoConfigureWebTestClient
@SpringBootTestWebEnv
@DirtiesContext
public class DiscardedEventsControllerTest {

    @Autowired
    DiscardedEventsController discardedEventsController;

    @Autowired
    WebTestClient webTestClient;

    @SpyBean
    DiscardedEventsService discardedEventsService;

    @Autowired
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;


    private static final String DISCARDED_EVENTS_ENDPOINT = "/external-channel/gestoreRepository/discarded-events";

    private static final String SCARTI_CONSOLIDATORE_TABLE_NAME = "pn-EcScartiConsolidatore";

    private DiscardedEventDto generateDiscardedEventDto(String requestId, String timestampRicezione, String codiceScarto, String dataRicezione, String details, String jsonRicevuto, String payloadHash) {
        DiscardedEventDto discardedEvent = new DiscardedEventDto();
        discardedEvent.setRequestId(requestId);
        discardedEvent.setTimestampRicezione(timestampRicezione);
        discardedEvent.setCodiceScarto(codiceScarto);
        discardedEvent.setDataRicezione(dataRicezione);
        discardedEvent.setDetails(details);
        discardedEvent.setJsonRicevuto(jsonRicevuto);
        discardedEvent.setPayloadHash(payloadHash);


        return discardedEvent;
    }

    private Flux<DiscardedEventDto> generateDiscardedEventDtoFlux(boolean isValid) {
        return Flux.just(
                generateDiscardedEventDto("requestId1", "timeStampRicezione1", "codiceScarto1", "dataRicezione1", "details1", "jsonRicevuto1", "payloadHash1"),
                generateDiscardedEventDto(isValid ? "requestId2" : null, "timeStampRicezione2", "codiceScarto2", "dataRicezione2", "details2", "jsonRicevuto2", "payloadHash2"),
                generateDiscardedEventDto("requestId3", "timeStampRicezione3", "codiceScarto3", "dataRicezione3", "details3", "jsonRicevuto3", "payloadHash3")
        );
    }

    private DiscardedEvent getDiscardedEventFromDynamo(String requestId, String timestampRicezione) {
        Key key = Key.builder().partitionValue(requestId).sortValue(timestampRicezione).build();
        GetItemEnhancedRequest request = GetItemEnhancedRequest.builder().key(key).build();
        return dynamoDbEnhancedAsyncClient.table(SCARTI_CONSOLIDATORE_TABLE_NAME, TableSchema.fromBean(DiscardedEvent.class)).getItem(request).join();
    }

    private boolean areDiscardedEventsEquals(DiscardedEvent discardedEvent, DiscardedEventDto discardedEventDto) {
        return discardedEvent.getRequestId().equals(discardedEventDto.getRequestId()) &&
                discardedEvent.getTimestampRicezione().equals(discardedEventDto.getTimestampRicezione()) &&
                discardedEvent.getCodiceScarto().equals(discardedEventDto.getCodiceScarto()) &&
                discardedEvent.getDataRicezione().equals(discardedEventDto.getDataRicezione()) &&
                discardedEvent.getDetails().equals(discardedEventDto.getDetails()) &&
                discardedEvent.getJsonRicevuto().equals(discardedEventDto.getJsonRicevuto()) &&
                discardedEvent.getPayloadHash().equals(discardedEventDto.getPayloadHash());
    }

    @Test
    void insertDiscardedEventsWebClientOkTest() {
        Flux<DiscardedEventDto> events = generateDiscardedEventDtoFlux(true);

        webTestClient.post()
                .uri(DISCARDED_EVENTS_ENDPOINT)
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .body(BodyInserters.fromPublisher(events, DiscardedEventDto.class))
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .returnResult(DiscardedEventDto.class).getResponseBody().map(discardedEventDto -> {
                    DiscardedEvent discardedEvent = getDiscardedEventFromDynamo(discardedEventDto.getRequestId(), discardedEventDto.getTimestampRicezione());
                    Assertions.assertTrue(areDiscardedEventsEquals(discardedEvent, discardedEventDto));
                    return discardedEventDto;
                }).subscribe();


        verify(discardedEventsService, times(3)).insertDiscardedEvent(any());
    }

    @Test
    void insertDiscardedEventMissingRequiredPropertyKoTest() {
        Flux<DiscardedEventDto> events = generateDiscardedEventDtoFlux(false);

        webTestClient.post()
                .uri(DISCARDED_EVENTS_ENDPOINT)
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .body(BodyInserters.fromPublisher(events, DiscardedEventDto.class))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .returnResult(DiscardedEventDto.class).getResponseBody().map(discardedEventDto -> {
                    DiscardedEvent discardedEvent = getDiscardedEventFromDynamo(discardedEventDto.getRequestId(), discardedEventDto.getTimestampRicezione());
                    Assertions.assertNull(discardedEvent);
                    return discardedEventDto;
                }).subscribe();

        verify(discardedEventsService, times(1)).insertDiscardedEvent(any());
    }
}
