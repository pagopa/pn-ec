package it.pagopa.pn.ec.scaricamentoesitipec.utils;

import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;
import software.amazon.awssdk.services.cloudwatch.model.MetricStat;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.concurrent.ExecutionException;

@SpringBootTestWebEnv
@Slf4j
public class CloudWatchPecMetricsTest {

    @Autowired
    private CloudWatchPecMetrics cloudWatchPecMetrics;

    @Autowired
    private CloudWatchAsyncClient cloudWatchAsyncClient;

    private static final Long MESSAGE_COUNT = 100L;

    @Test
    void publishMessageCountOk() throws ExecutionException, InterruptedException {

        var testMono = cloudWatchPecMetrics.publishMessageCount(MESSAGE_COUNT);
        StepVerifier.create(testMono).verifyComplete();

//        var listMetrics = cloudWatchAsyncClient.listMetrics().get();
//        Metric messageCount = listMetrics.metrics().stream().filter(metric -> metric.metricName().equals("PECInboxMessageCount")).findFirst().get();
//
//        log.info("Metrics : {}", listMetrics);
//
//        var messageCountMetric = cloudWatchAsyncClient.getMetricData(builder -> builder
//                        .startTime(Instant.now().minus(Duration.ofMinutes(1)))
//                        .endTime(Instant.now())
//                        .metricDataQueries(MetricDataQuery.builder()
//                                .id("count")
//                                .metricStat(MetricStat.builder()
//                                        .metric(messageCount)
//                                        .stat("")
//                                        .period(60).build()).build())).get();
//
//        log.info("Message count metric : {}", messageCountMetric);

    }

}