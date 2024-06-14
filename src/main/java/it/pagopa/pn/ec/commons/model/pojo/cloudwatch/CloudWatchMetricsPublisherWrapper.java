package it.pagopa.pn.ec.commons.model.pojo.cloudwatch;

import lombok.CustomLog;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import java.time.Duration;
import java.util.List;


@Getter
@CustomLog
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class CloudWatchMetricsPublisherWrapper {

    CloudWatchMetricPublisher cloudWatchMetricPublisher;
    String namespace;
    int maximumCallsPerUpload;
    Duration uploadFrequency;
    CloudWatchAsyncClient cloudWatchClient;
    List<SdkMetric<String>> dimensions;


    /**
     * Instantiates a new CloudWatchMetricsPublisherWrapper.
     *
     * @param nameSpace             the name space
     * @param maximumCallsPerUpload the maximum calls per upload
     * @param uploadFrequency       the upload frequency
     * @param cloudWatchClient      the cloud watch client
     * @param dimensions            the dimensions
     */
    public CloudWatchMetricsPublisherWrapper(String nameSpace, int maximumCallsPerUpload, Duration uploadFrequency, CloudWatchAsyncClient cloudWatchClient, List<SdkMetric<String>> dimensions) {
        log.debug("Initializing CloudWatchMetricPublisher wrapper with args: nameSpace={}, maximumCallsPerUpload={}, uploadFrequencyMillis={}", nameSpace, maximumCallsPerUpload, uploadFrequency.toSeconds()+"s");
        this.namespace = nameSpace;
        this.maximumCallsPerUpload = maximumCallsPerUpload;
        this.uploadFrequency = uploadFrequency;
        this.cloudWatchClient = cloudWatchClient;
        this.dimensions = dimensions;
        this.cloudWatchMetricPublisher = CloudWatchMetricPublisher.builder()
                .cloudWatchClient(cloudWatchClient)
                .namespace(nameSpace)
                .maximumCallsPerUpload(maximumCallsPerUpload)
                .uploadFrequency(uploadFrequency)
                .dimensions(dimensions)
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
