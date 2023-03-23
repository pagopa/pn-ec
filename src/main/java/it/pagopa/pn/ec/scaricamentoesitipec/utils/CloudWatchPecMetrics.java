package it.pagopa.pn.ec.scaricamentoesitipec.utils;

import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.CloudWatchPecMetricsInfo;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

@Component
public class CloudWatchPecMetrics {

    private final CloudWatchAsyncClient cloudWatchAsyncClient;

    public CloudWatchPecMetrics(CloudWatchAsyncClient cloudWatchAsyncClient) {
        this.cloudWatchAsyncClient = cloudWatchAsyncClient;
    }

    public Mono<Status> publishCustomPecMetricsAndReturnNextStatus(CloudWatchPecMetricsInfo cloudWatchPecMetricsInfo) {
        return Mono.fromCompletionStage(cloudWatchAsyncClient.putMetricData(builder ->))
    }
}
