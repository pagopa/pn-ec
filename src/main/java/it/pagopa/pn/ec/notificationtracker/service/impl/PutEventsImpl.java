package it.pagopa.pn.ec.notificationtracker.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import it.pagopa.pn.ec.notificationtracker.service.PutEvents;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.time.Duration;
import java.util.Date;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@Service
@CustomLog
public class PutEventsImpl implements PutEvents {

    private final EventBridgeAsyncClient eventBrClient;
    private final ObjectMapper objectMapper;
    private final String notificationsBusName;

    public PutEventsImpl(EventBridgeAsyncClient eventBrClient, ObjectMapper objectMapper,
                         PnEcConfig pnEcConfig) {
        this.eventBrClient = eventBrClient;
        this.objectMapper = objectMapper;
        this.notificationsBusName = pnEcConfig.getNotificationTracker().getEventBridge().getNotificationsBusName();
    }

    @Override
    public <T>Mono<PutEventsResponse> putEventExternal(final T objectToNotify, String processId, String detailType) {
        log.info(CLIENT_METHOD_INVOCATION_WITH_ARGS, EVENT_BRIDGE_PUT_EVENT_EXTERNAL, objectToNotify);
        return Mono.fromCallable(() -> PutEventsRequestEntry.builder()
                                                            .time(new Date().toInstant())
                                                            .source("NOTIFICATION TRACKER")
                                                            .detailType(detailType)
                                                            .detail(objectMapper.writeValueAsString(objectToNotify))
                                                            .eventBusName(notificationsBusName)
                                                            .build()).flatMap(putEventsRequestEntry -> {
            log.debug("Publish to event bridge with PutEventsRequestEntry ↓\n{}", putEventsRequestEntry);
            return Mono.fromCompletionStage(eventBrClient.putEvents(builder -> builder.entries(putEventsRequestEntry)))
                       .doOnError(throwable -> log.error( "EventBridgeClient error ---> {}", throwable.getMessage(), throwable.getCause()))
                       .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)));

        }).doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, EVENT_BRIDGE_PUT_EVENT_EXTERNAL, result));
    }
}
