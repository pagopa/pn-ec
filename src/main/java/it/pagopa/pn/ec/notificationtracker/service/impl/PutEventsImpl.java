package it.pagopa.pn.ec.notificationtracker.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.notificationtracker.configurationproperties.NotificationTrackerEventBridgeEventName;
import it.pagopa.pn.ec.notificationtracker.service.PutEvents;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.util.Date;

import static it.pagopa.pn.ec.commons.configuration.retry.RetryStrategy.DEFAULT_BACKOFF_RETRY_STRATEGY;

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
                                                            .detailType(processId)
                                                            .detail(objectMapper.writeValueAsString(objectToNotify))
                                                            .eventBusName(notificationTrackerEventBridgeEventName.notificationsBusName())
                                                            .build()).flatMap(putEventsRequestEntry -> {
            log.info("Publish to event bridge with PutEventsRequestEntry ↓\n{}", putEventsRequestEntry);
            return Mono.fromCompletionStage(eventBrClient.putEvents(builder -> builder.entries(putEventsRequestEntry)))
                       .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)));
        });
    }
}
