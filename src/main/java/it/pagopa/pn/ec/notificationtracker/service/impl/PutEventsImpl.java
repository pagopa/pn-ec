package it.pagopa.pn.ec.notificationtracker.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.notificationtracker.configurationproperties.NotificationTrackerEventBridgeEventName;
import it.pagopa.pn.ec.notificationtracker.service.PutEvents;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.time.Duration;
import java.util.Date;

@Service
@Slf4j
public class PutEventsImpl implements PutEvents {

    private final EventBridgeAsyncClient eventBrClient;
    private final ObjectMapper objectMapper;
    private final NotificationTrackerEventBridgeEventName notificationTrackerEventBridgeEventName;

    public PutEventsImpl(EventBridgeAsyncClient eventBrClient, ObjectMapper objectMapper,
                         NotificationTrackerEventBridgeEventName notificationTrackerEventBridgeEventName) {
        this.eventBrClient = eventBrClient;
        this.objectMapper = objectMapper;
        this.notificationTrackerEventBridgeEventName = notificationTrackerEventBridgeEventName;
    }

    @Override
    public <T>Mono<PutEventsResponse> putEventExternal(final T objectToNotify, String processId) {
        return Mono.fromCallable(() -> PutEventsRequestEntry.builder()
                                                            .time(new Date().toInstant())
                                                            .source("NOTIFICATION TRACKER")
                                                            .detailType("ExternalChannelOutcomeEvent")
                                                            .detail(objectMapper.writeValueAsString(objectToNotify))
                                                            .eventBusName(notificationTrackerEventBridgeEventName.notificationsBusName())
                                                            .build()).flatMap(putEventsRequestEntry -> {
            log.debug("Publish to event bridge with PutEventsRequestEntry ↓\n{}", putEventsRequestEntry);
            return Mono.fromCompletionStage(eventBrClient.putEvents(builder -> builder.entries(putEventsRequestEntry)))
                       .doOnError(throwable -> log.error( "EventBridgeClient error ---> {}", throwable.getMessage(), throwable.getCause()))
                       .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)));

        });
    }
}
