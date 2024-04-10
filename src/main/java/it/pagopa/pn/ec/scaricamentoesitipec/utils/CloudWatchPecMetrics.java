package it.pagopa.pn.ec.scaricamentoesitipec.utils;

import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.CloudWatchTransitionElapsedTimeMetricsInfo;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@Component
@CustomLog
public class CloudWatchPecMetrics {

    private final CloudWatchAsyncClient cloudWatchAsyncClient;

    public CloudWatchPecMetrics(CloudWatchAsyncClient cloudWatchAsyncClient) {
        this.cloudWatchAsyncClient = cloudWatchAsyncClient;
    }

    private static final PutMetricDataRequest.Builder NAMESPACE = PutMetricDataRequest.builder().namespace("PEC");
    private static final Dimension DIMENSION = Dimension.builder().name("Event").value("StatusChange").build();
    private static final MetricDatum.Builder DATUM = MetricDatum.builder().unit(StandardUnit.SECONDS).dimensions(DIMENSION);
    private static final String MESSAGE_COUNT_METRIC_NAME = "InboxMessageCount";
    private static final String TRANSACTION_SENT_AND_ACCEPTED = "TimeElapsedBetweenSentAndAccepted";
    private static final String TRANSACTION_ACCEPTED_AND_DELIVERED = "TimeElapsedBetweenAcceptedAndDelivered";
    private static final String TRANSACTION_ACCEPTED_AND_NOT_DELIVERED = "TimeElapsedBetweenAcceptedAndNotDelivered";

    /**
     * In order not to block the chain with an error, the method emits onComplete() even if an error occurred while publishing the metric
     */

    //pubblica il tempo necessario tra le ericevute elapsed time , refacto method andclass
    public Mono<Void> publishTransitionElapsedTimeMetrics(CloudWatchTransitionElapsedTimeMetricsInfo cloudWatchTransitionElapsedTimeMetricsInfo) {
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, PUBLISH_CUSTOM_PEC_METRICS, cloudWatchTransitionElapsedTimeMetricsInfo);
        return Mono.fromCallable(() -> {
            var previousStatus = cloudWatchTransitionElapsedTimeMetricsInfo.getPreviousStatus();
            var nextStatus = cloudWatchTransitionElapsedTimeMetricsInfo.getNextStatus();
            if (previousStatus.equals(SENT.getStatusTransactionTableCompliant()) &&
                nextStatus.equals(ACCEPTED.getStatusTransactionTableCompliant())) {
                return TRANSACTION_SENT_AND_ACCEPTED;
            } else if (previousStatus.equals(ACCEPTED.getStatusTransactionTableCompliant()) &&
                       nextStatus.equals(DELIVERED.getStatusTransactionTableCompliant())) {
                return TRANSACTION_ACCEPTED_AND_DELIVERED;
            } else if (previousStatus.equals(ACCEPTED.getStatusTransactionTableCompliant()) &&
                       nextStatus.equals(NOT_DELIVERED.getStatusTransactionTableCompliant())) {
                return TRANSACTION_ACCEPTED_AND_NOT_DELIVERED;
            } else {
                return null;
            }
        }).flatMap(metricName -> {
            var time = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            var instant = Instant.parse(time);

            var deltaElapsed =
                    Duration.between(cloudWatchTransitionElapsedTimeMetricsInfo.getPreviousEventTimestamp(), cloudWatchTransitionElapsedTimeMetricsInfo.getNextEventTimestamp())
                            .toSeconds();

            return Mono.fromCompletionStage(cloudWatchAsyncClient.putMetricData(NAMESPACE.metricData(DATUM.metricName(metricName)
                                                                                                          .value((double) deltaElapsed)
                                                                                                          .timestamp(instant)
                                                                                                          .build()).build()));
        }).onErrorResume(throwable -> {
            log.error(EXCEPTION_IN_PROCESS, PUBLISH_CUSTOM_PEC_METRICS, throwable, throwable.getMessage());
            return Mono.empty();
        }).then();
    }

    public Mono<Void> publishMessageCount(Long count, String namespace) {
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, PUBLISH_PEC_MESSAGE_COUNT, count);
        return Mono.fromCompletionStage(cloudWatchAsyncClient.putMetricData(PutMetricDataRequest.builder()
                        .namespace(namespace)
                        .metricData(MetricDatum.builder()
                        .unit(StandardUnit.COUNT)
                        .metricName(MESSAGE_COUNT_METRIC_NAME)
                        .value(Double.valueOf(count))
                        .timestamp(Instant.now())
                        .build()).build()))
                .onErrorResume(throwable -> {
                    log.error(EXCEPTION_IN_PROCESS, PUBLISH_PEC_MESSAGE_COUNT, throwable, throwable.getMessage());
                    return Mono.empty();
                }).then();
    }

    //Method to execute a Mono<Void> and publish its response time to CloudWatch.
    public Mono<Void> executeAndPublishResponseTime(Mono<Void> mono, String namespace, String metricName) {
        return mono.thenReturn(true)
                .elapsed()
                .flatMap(tuple -> publishResponseTime(namespace, metricName, tuple.getT1()));
    }

    //Method to publish a response time related CloudWatch metric.
    public Mono<Void> publishResponseTime(String namespace, String metricName, long elapsedTime) {
        return Mono.fromCompletionStage(() -> {
                    log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, PUBLISH_RESPONSE_TIME, Stream.of(namespace, metricName, elapsedTime).toList());
                    return cloudWatchAsyncClient.putMetricData(PutMetricDataRequest.builder()
                            .namespace(namespace)
                            .metricData(MetricDatum.builder()
                                    .unit(StandardUnit.MILLISECONDS)
                                    .metricName(metricName)
                                    .value((double) elapsedTime)
                                    .timestamp(Instant.now())
                                    .build()).build());
                })
                .onErrorResume(throwable -> {
                    log.error(EXCEPTION_IN_PROCESS, PUBLISH_RESPONSE_TIME, throwable, throwable.getMessage());
                    return Mono.empty();
                }).then();
    }
}
