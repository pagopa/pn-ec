package it.pagopa.pn.ec.sqs;

import it.pagopa.pn.ec.commons.configuration.sqs.SqsTimeoutConfigurationProperties;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CustomLog
@RequiredArgsConstructor
public class SqsTimeoutProvider {

    private final SqsAsyncClient sqsAsyncClient;
    private final SqsTimeoutConfigurationProperties config;
    private final Map<String, Duration> queueTimeouts = new ConcurrentHashMap<>();

    @PostConstruct
    public Mono<Void> initQueueTimeouts() {
        var queues = config.getManagedQueues();
        if (queues == null || queues.isEmpty()) {
            log.warn("No queues configured in SqsTimeoutConfigurationProperties");
            return Mono.empty();
        }

        return Flux.fromIterable(queues)
                .flatMap(this::loadTimeoutForQueue)
                .then();
    }


    private Mono<Void> loadTimeoutForQueue(String queueName) {
        int percent= config.getPercent();
        if (percent == 0) {
            log.info("Timeout is disabled (percent = {}). Skipping queue {}.", percent, queueName);
            return Mono.empty();
        }
        return Mono.fromFuture(() ->
                sqsAsyncClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build())
        ).flatMap(queueUrlResponse -> {
            String queueUrl = queueUrlResponse.queueUrl();
            return Mono.fromFuture(() ->
                    sqsAsyncClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                            .queueUrl(queueUrl)
                            .attributeNamesWithStrings("VisibilityTimeout")
                            .build())
            );
        }).doOnNext(attributes -> {
            String timeoutStr = attributes.attributes().get(QueueAttributeName.VISIBILITY_TIMEOUT);
            int visibilityTimeout = Integer.parseInt(timeoutStr);

            long timeout = (long) visibilityTimeout * percent / 100L;

            if (timeout < 1 || visibilityTimeout < 1) {
                log.warn("Effective timeout for {} would be >= visibilityTimeout or too short — skipping",queueName);
                return;
            }

            Duration processingTimeout = Duration.ofSeconds(timeout);
            queueTimeouts.put(queueName, processingTimeout);

            log.info("Timeout configured for queue {}: {}s", queueName, processingTimeout.getSeconds());
        }).then();
    }

    public Duration getTimeoutForQueue(String queueName) {
        return queueTimeouts.getOrDefault(queueName, Duration.ofSeconds(config.getDefaultSeconds()));
    }

}