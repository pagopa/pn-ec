package it.pagopa.pn.ec.notificationtracker.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
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
    public Mono<PutEventsResponse> putEventExternal(final NotificationTrackerQueueDto notificationTrackerQueueDto) {
        return Mono.fromCallable(() -> PutEventsRequestEntry.builder()
                                                            .time(new Date().toInstant())
                                                            .source("NOTIFICATION TRACKER")
                                                            .detailType(notificationTrackerQueueDto.getProcessId())
                                                            .detail(objectMapper.writeValueAsString(notificationTrackerQueueDto))
                                                            .eventBusName(notificationTrackerEventBridgeEventName.notificationsBusName())
                                                            .build()).flatMap(putEventsRequestEntry -> {
            log.info("Publish to event bridge with PutEventsRequestEntry ↓\n{}", putEventsRequestEntry);
            return Mono.fromCompletionStage(eventBrClient.putEvents(builder -> builder.entries(putEventsRequestEntry)))
                       .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)));
        });
    }
}
