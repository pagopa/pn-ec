package it.pagopa.pn.ec.commons.model.pojo.cloudwatch;

import lombok.CustomLog;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import java.time.Duration;


@Getter
@CustomLog
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class CloudWatchMetricsPublisherWrapper {

    CloudWatchMetricPublisher cloudWatchMetricPublisher;
    String namespace;
    int maximumCallsPerUpload;
    Duration uploadFrequency;
    CloudWatchAsyncClient cloudWatchClient;


    /**
     * Instantiates a new CloudWatchMetricsPublisherWrapper.
     *
     * @param nameSpace             the name space
     * @param maximumCallsPerUpload the maximum calls per upload
     * @param uploadFrequency       the upload frequency
     * @param cloudWatchClient      the cloud watch client
     */
    public CloudWatchMetricsPublisherWrapper(String nameSpace, int maximumCallsPerUpload, Duration uploadFrequency, CloudWatchAsyncClient cloudWatchClient) {
        log.debug("Initializing CloudWatchMetricPublisher wrapper with args: nameSpace={}, maximumCallsPerUpload={}, uploadFrequencyMillis={}", nameSpace, maximumCallsPerUpload, uploadFrequency.toSeconds()+"s");
        this.namespace = nameSpace;
        this.maximumCallsPerUpload = maximumCallsPerUpload;
        this.uploadFrequency = uploadFrequency;
        this.cloudWatchClient = cloudWatchClient;
        this.cloudWatchMetricPublisher = CloudWatchMetricPublisher.builder()
                .cloudWatchClient(cloudWatchClient)
                .namespace(nameSpace)
                .maximumCallsPerUpload(maximumCallsPerUpload)
                .uploadFrequency(uploadFrequency)
                .build();
    }

    /**
     * Method for calling the publish method of the CloudWatchMetricPublisher.
     *
     * @param metricCollection the metric collection to publish
     */
    public void publish(MetricCollection metricCollection) {
        cloudWatchMetricPublisher.publish(metricCollection);
    }


}
