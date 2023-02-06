
package it.pagopa.pn.ec.notificationtracker.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.*;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class PutEventsImpl  implements PutEvents {



    private final EventBridgeAsyncClient eventBrClient;

    public PutEventsImpl(EventBridgeAsyncClient eventBrClient) {
        this.eventBrClient = eventBrClient;
    }

    @Value("${event.Bus.Nome}")
    String evenName;

    @Override
    public Mono<Void> putEventExternal(final NotificationTrackerQueueDto eventInfo)  {
        log.info(">>> Start publish event to EventBridge  ");

        PutEventsRequestEntry reqEntry = PutEventsRequestEntry.builder()
                .time(new Date().toInstant())
                .source("NOTIFICATION TRACKER")
                .detailType(eventInfo.getProcessId().toString())
                .detail(toJson(eventInfo))
                .eventBusName(evenName)
                .build();
        log.info(">>> publish event to EventBridge {} ", reqEntry);


        PutEventsRequest eventsRequest = PutEventsRequest.builder()
                .entries(reqEntry)
                .build();
        log.info(">>> PutEventsRequestEntry to EventBridge: " + eventsRequest);


        return  Mono.fromCompletionStage(eventBrClient.putEvents(builder -> builder.entries(reqEntry))).retryWhen(Retry.backoff(3, Duration.ofSeconds(2))).then();

    }




    private static String toJson(Object obj) {
        try {
            return new ObjectMapper()
                    .registerModule(new Jdk8Module())
                    .registerModule(new JavaTimeModule())
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error converting toJson", e);
        }
        return "{}";
    }
}
