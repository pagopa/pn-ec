package it.pagopa.pn.ec.scaricamentoesitipec.utils;

import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.CloudWatchPecMetricsInfo;
import lombok.extern.slf4j.Slf4j;
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

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@Component
@Slf4j
public class CloudWatchPecMetrics {

    private final CloudWatchAsyncClient cloudWatchAsyncClient;

    public CloudWatchPecMetrics(CloudWatchAsyncClient cloudWatchAsyncClient) {
        this.cloudWatchAsyncClient = cloudWatchAsyncClient;
    }

    private static final PutMetricDataRequest.Builder NAMESPACE = PutMetricDataRequest.builder().namespace("PEC");
    private static final Dimension DIMENSION = Dimension.builder().name("Event").value("StatusChange").build();
    private static final MetricDatum.Builder DATUM = MetricDatum.builder().unit(StandardUnit.SECONDS).dimensions(DIMENSION);

    /**
     * In order not to block the chain with an error, the method emits onComplete() even if an error occurred while publishing the metric
     */
    public Mono<Void> publishCustomPecMetrics(CloudWatchPecMetricsInfo cloudWatchPecMetricsInfo) {
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, PUBLISH_CUSTOM_PEC_METRICS, cloudWatchPecMetricsInfo);
        return Mono.fromCallable(() -> {
            var previousStatus = cloudWatchPecMetricsInfo.getPreviousStatus();
            var nextStatus = cloudWatchPecMetricsInfo.getNextStatus();
            if (previousStatus.equals(SENT.getStatusTransactionTableCompliant()) &&
                nextStatus.equals(ACCEPTED.getStatusTransactionTableCompliant())) {
                return "TimeElapsedBetweenSentAndAccepted";
            } else if (previousStatus.equals(ACCEPTED.getStatusTransactionTableCompliant()) &&
                       nextStatus.equals(DELIVERED.getStatusTransactionTableCompliant())) {
                return "TimeElapsedBetweenAcceptedAndDelivered";
            } else if (previousStatus.equals(ACCEPTED.getStatusTransactionTableCompliant()) &&
                       nextStatus.equals(NOT_DELIVERED.getStatusTransactionTableCompliant())) {
                return "TimeElapsedBetweenAcceptedAndNotDelivered";
            } else {
                return null;
            }
        }).flatMap(metricName -> {
            var time = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            var instant = Instant.parse(time);

            var deltaElapsed =
                    Duration.between(cloudWatchPecMetricsInfo.getPreviousEventTimestamp(), cloudWatchPecMetricsInfo.getNextEventTimestamp())
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
}
