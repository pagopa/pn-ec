package it.pagopa.pn.ec.scaricamentoesitipec.utils;

import it.pagopa.pn.ec.commons.configuration.aws.cloudwatch.CloudWatchMetricPublisherConfiguration;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.CloudWatchTransitionElapsedTimeMetricsInfo;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.metrics.*;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

/**
 * A service class to publish CloudWatch metrics. It can directly publish metrics to CloudWatch or make use of the CloudWatchMetricPublisherConfiguration class.
 *
 * @see it.pagopa.pn.ec.commons.configuration.aws.cloudwatch.CloudWatchMetricPublisherConfiguration
 */
@Component
@CustomLog
public class CloudWatchPecMetrics {

    private final CloudWatchAsyncClient cloudWatchAsyncClient;
    private final CloudWatchMetricPublisherConfiguration cloudWatchMetricPublisherConfiguration;
    private static final PutMetricDataRequest.Builder NAMESPACE = PutMetricDataRequest.builder().namespace("PEC");
    private static final Dimension DIMENSION = Dimension.builder().name("Event").value("StatusChange").build();
    private static final MetricDatum.Builder DATUM = MetricDatum.builder().unit(StandardUnit.SECONDS).dimensions(DIMENSION);
    private static final String MESSAGE_COUNT_METRIC_NAME = "InboxMessageCount";
    private static final String TRANSACTION_SENT_AND_ACCEPTED = "TimeElapsedBetweenSentAndAccepted";
    private static final String TRANSACTION_ACCEPTED_AND_DELIVERED = "TimeElapsedBetweenAcceptedAndDelivered";
    private static final String TRANSACTION_ACCEPTED_AND_NOT_DELIVERED = "TimeElapsedBetweenAcceptedAndNotDelivered";

    /**
     * Instantiates a new Cloud watch pec metrics.
     *
     * @param cloudWatchAsyncClient                  the cloud watch async client
     * @param cloudWatchMetricPublisherConfiguration the cloud watch metric publisher configuration
     */
    public CloudWatchPecMetrics(CloudWatchAsyncClient cloudWatchAsyncClient, CloudWatchMetricPublisherConfiguration cloudWatchMetricPublisherConfiguration) {
        this.cloudWatchAsyncClient = cloudWatchAsyncClient;
        this.cloudWatchMetricPublisherConfiguration = cloudWatchMetricPublisherConfiguration;
    }

    /**
     * Method for publishing a metric related to the time between receipt arrivals.
     * In order not to block the chain with an error, the method emits onComplete() even if an error occurred while publishing the metric.
     *
     * @param cloudWatchTransitionElapsedTimeMetricsInfo the cloud watch transition elapsed time metrics related info
     * @return a void Mono
     */
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

    /**
     * Method to publish the count of messages in a PEC folder.
     *In order not to block the chain with an error, the method emits onComplete() even if an error occurred while publishing the metric.
     * @param count     the count of messages
     * @param namespace the metric namespace
     * @return the mono
     */
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

    /**
     * Method to execute a Mono and publish its execution time to CloudWatch
     * It handles both valued and void monos.
     *
     * @param mono       the mono to execute
     * @param namespace  the metric namespace
     * @param metricName the metric name
     * @return the mono with the result
     */
    public <T> Mono<T> executeAndPublishResponseTime(Mono<T> mono, String namespace, String metricName, Dimension... dimensions) {
        return Mono.defer(() -> {
            Instant start = Instant.now();
            return mono
                    //If mono emits a value.
                    .flatMap(result -> {
                        long elapsed = Duration.between(start, Instant.now()).toMillis();
                        return publishResponseTime(namespace, metricName, elapsed, dimensions).thenReturn(result);
                    })
                    //If mono doesn't emit any value (Mono<Void>)
                    .switchIfEmpty(Mono.defer(() -> {
                        long elapsed = Duration.between(start, Instant.now()).toMillis();
                        return publishResponseTime(namespace, metricName, elapsed, dimensions).then(Mono.empty());
                    }));
        });
    }

    /**
     * Method to publish a response time related CloudWatch metric with its dimensions, using the CloudWatchMetricPublisherConfiguration class.
     * In order not to block the chain with an error, the method emits onComplete() even if an error occurred while publishing the metric.
     *
     * @param namespace   the metric namespace
     * @param metricName  the metric name
     * @param elapsedTime the response time
     * @param dimensions  the metric dimensions
     * @return a void Mono
     */
    public Mono<Void> publishResponseTime(String namespace, String metricName, long elapsedTime, Dimension... dimensions) {
        return Mono.fromRunnable(() -> {
                    log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, PUBLISH_RESPONSE_TIME, Stream.of(namespace, metricName, elapsedTime).toList());
                    MetricCollector metricCollector = MetricCollector.create(metricName);

                    //Report metric.
                    SdkMetric<Long> responseTimeMetric = (SdkMetric<Long>) cloudWatchMetricPublisherConfiguration.getSdkMetricByMetricName(metricName);
                    metricCollector.reportMetric(responseTimeMetric, elapsedTime);

                    //Report metric dimensions.
                    for (Dimension dimension : dimensions) {
                        SdkMetric<Long> dimensionMetric = (SdkMetric<Long>) cloudWatchMetricPublisherConfiguration.getSdkMetricByMetricName(dimension.name());
                        metricCollector.reportMetric(dimensionMetric, Long.valueOf(dimension.value()));
                    }

                    MetricCollection metricCollection = metricCollector.collect();
                    cloudWatchMetricPublisherConfiguration.getMetricPublisherByNamespace(namespace).publish(metricCollection);
                })
                .onErrorResume(throwable -> {
                    log.error(EXCEPTION_IN_PROCESS, PUBLISH_RESPONSE_TIME, throwable, throwable.getMessage());
                    return Mono.empty();
                }).then();
    }
}
