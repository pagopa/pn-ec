
package it.pagopa.pn.ec.notificationtracker.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.ec.notificationtracker.model.NotificationRequestModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.*;

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
    public Mono<Void> putEventExternal(NotificationRequestModel eventInfo)  {
        log.info(">>> Start publish event to EventBridge  ");
        try {
            NotificationRequestModel request= NotificationRequestModel.builder()
                    .processId(eventInfo.getProcessId())
                    .xpagopaExtchCxId(eventInfo.getXpagopaExtchCxId())
                    .nextStatus(eventInfo.getNextStatus())
                    .build();

            PutEventsRequestEntry reqEntry = PutEventsRequestEntry.builder()
                    .time(new Date().toInstant())
                    .source("NOTIFICATION TRACKER")
                    .detailType(eventInfo.getProcessId())
                    .detail(toJson(request))
                    .eventBusName(evenName)
                    .build();
            log.info(">>> publish event to EventBridge {} ", reqEntry);

            PutEventsRequest eventsRequest = PutEventsRequest.builder()
                    .entries(reqEntry)
                    .build();
            log.info(">>> PutEventsRequestEntry to EventBridge: " + eventsRequest);

            CompletableFuture<PutEventsResponse> result = eventBrClient.putEvents(eventsRequest);
            for (PutEventsResultEntry resultEntry : result.get().entries()) {
                if (resultEntry.eventId() != null) {
                    log.info(">>> Event Id:: " +resultEntry.eventId());
                } else {
                    log.info("Injection failed with Error Code: " + resultEntry.errorCode());
                }
            }

        } catch (EventBridgeException | InterruptedException | ExecutionException e) {
//            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }

        return Mono.empty();
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
