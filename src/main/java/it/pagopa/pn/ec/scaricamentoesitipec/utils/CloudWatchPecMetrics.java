package it.pagopa.pn.ec.scaricamentoesitipec.utils;

import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.CloudWatchPecMetricsInfo;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

@Component
public class CloudWatchPecMetrics {

    private final CloudWatchAsyncClient cloudWatchAsyncClient;

    public CloudWatchPecMetrics(CloudWatchAsyncClient cloudWatchAsyncClient) {
        this.cloudWatchAsyncClient = cloudWatchAsyncClient;
    }

    private static final PutMetricDataRequest.Builder NAMESPACE = PutMetricDataRequest.builder().namespace("PEC");
    private static final MetricDatum.Builder DATUM = MetricDatum.builder()
                                                                .metricName("PAGES_VISITED")
                                                                .unit(StandardUnit.MILLISECONDS)
                                                                .dimensions(builder -> builder.name("TIME_ELAPSED_BETWEEN_STATES")
                                                                                              .value("MILLISECONDS"));

    public Mono<Void> publishCustomPecMetrics(CloudWatchPecMetricsInfo cloudWatchPecMetricsInfo) {
        return Mono.fromCompletionStage(cloudWatchAsyncClient.putMetricData(NAMESPACE.metricData()
                                                                                     .build())).thenReturn(cloudWatchPecMetricsInfo);
    }
}
