package it.pagopa.pn.ec.notificationtracker.service.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.notificationtracker.configurationproperties.NotificationTrackerEventBridgeEventName;
import it.pagopa.pn.ec.notificationtracker.service.PutEvents;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.time.Duration;
import java.util.Date;

@Service
@Slf4j
public class PutEventsImpl implements PutEvents {

    private final EventBridgeAsyncClient eventBrClient;
    private final NotificationTrackerEventBridgeEventName notificationTrackerEventBridgeEventName;

    public PutEventsImpl(EventBridgeAsyncClient eventBrClient,
                         NotificationTrackerEventBridgeEventName notificationTrackerEventBridgeEventName) {
        this.eventBrClient = eventBrClient;
        this.notificationTrackerEventBridgeEventName = notificationTrackerEventBridgeEventName;
    }

    @Override
    public Mono<Void> putEventExternal(final NotificationTrackerQueueDto eventInfo) {
        log.info(">>> Start publish event to EventBridge  ");

        PutEventsRequestEntry reqEntry = PutEventsRequestEntry.builder()
                                                              .time(new Date().toInstant())
                                                              .source("NOTIFICATION TRACKER")
                                                              .detailType(eventInfo.getProcessId().toString())
                                                              .detail(toJson(eventInfo))
                                                              .eventBusName(notificationTrackerEventBridgeEventName.notificationsBusName())
                                                              .build();
        log.info(">>> publish event to EventBridge {} ", reqEntry);


        PutEventsRequest eventsRequest = PutEventsRequest.builder().entries(reqEntry).build();
        log.info(">>> PutEventsRequestEntry to EventBridge: " + eventsRequest);

        return Mono.fromCompletionStage(eventBrClient.putEvents(builder -> builder.entries(reqEntry)))
                   .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                   .then();
    }

    private static String toJson(Object obj) {
        try {
            return new ObjectMapper().registerModule(new Jdk8Module())
                                     .registerModule(new JavaTimeModule())
                                     .writerWithDefaultPrettyPrinter()
                                     .writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error converting toJson", e);
        }
        return "{}";
    }
}
