package it.pagopa.pn.ec.repositorymanager.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.repositorymanager.model.entity.DiscardedEvent;
import it.pagopa.pn.ec.repositorymanager.service.DiscardedEventsService;
import it.pagopa.pn.ec.rest.v1.api.DiscardedEventsApi;
import it.pagopa.pn.ec.rest.v1.dto.DiscardedEventDto;
import lombok.CustomLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@CustomLog
public class DiscardedEventController implements DiscardedEventsApi {

    private final DiscardedEventsService discardedEventsService;
    private final ObjectMapper objectMapper;

    public DiscardedEventController(DiscardedEventsService discardedEventsService, ObjectMapper objectMapper) {
        this.discardedEventsService = discardedEventsService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<ResponseEntity<Flux<DiscardedEventDto>>> insertDiscardedEvents(Flux<DiscardedEventDto> discardedEventsList, ServerWebExchange exchange) {
        Flux<DiscardedEventDto> discardedEventDtoFlux = discardedEventsList.map(discardedEventDto -> {
                    discardedEventDto.setTimestampRicezione(discardedEventDto.getTimestampRicezione() + "~" + UUID.randomUUID());
                    return discardedEventDto;
                })
                .map(discardedEventDto -> objectMapper.convertValue(discardedEventDto, DiscardedEvent.class))
                .flatMap(discardedEventsService::insertDiscardedEvent)
                .map(discardedEvent -> objectMapper.convertValue(discardedEvent, DiscardedEventDto.class));

        return Mono.just(ResponseEntity.ok().body(discardedEventDtoFlux));

    }
}
