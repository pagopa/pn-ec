package it.pagopa.pn.ec.commons.configuration.aws.cloudwatch;

import it.pagopa.pn.ec.commons.exception.cloudwatch.CloudWatchResourceNotFoundException;
import it.pagopa.pn.ec.commons.model.pojo.cloudwatch.CloudWatchMetricsPublisherWrapper;
import lombok.CustomLog;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.metrics.*;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.metrics.publishers.cloudwatch.internal.CloudWatchMetricLogger;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * A configuration class for publishing CloudWatch metrics. It prepares objects to handle asynchronous and optimized publishing of CloudWatch metrics.
 * This configuration class makes use of the AWS CloudWatchMetricPublisher system (Refer to <a href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/metrics/publishers/cloudwatch/CloudWatchMetricPublisher.html">CloudWatchMetricPublisher</a>)
 */
@Configuration
@CustomLog
public class CloudWatchMetricPublisherConfiguration {
    @Value("${library.pec.cloudwatch.namespace.aruba}")
    private String arubaPecNamespace;
    @Value("${library.pec.cloudwatch.namespace.namirial}")
    private String namirialPecNamespace;
    @Value("${cloudwatch.maximum-calls-per-upload:#{null}}")
    private int maximumCallsPerUpload;
    @Value("${cloudwatch.upload-frequency-millis:#{null}}")
    private int uploadFrequencyMillis;
    @Value("${library.pec.cloudwatch.metric.response-time.mark-message-as-read}")
    private String markMessageAsReadResponseTimeMetric;
    @Value("${library.pec.cloudwatch.metric.response-time.delete-message}")
    private String deleteMessageResponseTimeMetric;
    private final CloudWatchAsyncClient cloudWatchAsyncClient;
    private final Map<String, CloudWatchMetricsPublisherWrapper> cloudWatchMetricPublishers = new HashMap<>();
    private final Map<String, SdkMetric<?>> cloudWatchSdkMetrics = new HashMap<>();
    private final Map<String, MetricCollector> cloudWatchMetricCollectors = new HashMap<>();

    /**
     * Instantiates a new CloudWatchMetricPublisherConfiguration.
     *
     * @param cloudWatchAsyncClient the cloud watch async client
     */
    @Autowired
    public CloudWatchMetricPublisherConfiguration(CloudWatchAsyncClient cloudWatchAsyncClient) {
        this.cloudWatchAsyncClient = cloudWatchAsyncClient;
    }

    /**
     * Init method to initialize MetricPublishers, SdkMetrics and MetricCollectors
     */
    @PostConstruct
    private void init() {
        log.debug("Initializing CloudWatchMetricPublisher configurations.");
        initCloudWatchMetricPublishers();
        initCloudWatchSdkMetrics();
        initCloudWatchMetricCollectors();
   }

    /**
     * Gets metric publisher by namespace.
     *
     * @param namespace the namespace
     * @return the metric publisher by namespace
     * @throws CloudWatchResourceNotFoundException.MetricPublisherNotFoundException if there is no metric publisher for the given namespace
     */
    /*

     */
    public CloudWatchMetricsPublisherWrapper getMetricPublisherByNamespace(String namespace) {
        try {
            return cloudWatchMetricPublishers.get(namespace);
        } catch (NullPointerException e) {
            throw new CloudWatchResourceNotFoundException.MetricPublisherNotFoundException(namespace);
        }
    }

    /**
     * Gets metric collector by metric name.
     *
     * @param metricName the metric name
     * @return the metric collector by metric name
     * @throws CloudWatchResourceNotFoundException.MetricCollectorNotFoundException if there is no metric collector for the given metric name
     */
    public MetricCollector getMetricCollectorByMetricName(String metricName) {
        try {
            return cloudWatchMetricCollectors.get(metricName);
        } catch (NullPointerException e) {
            throw new CloudWatchResourceNotFoundException.MetricCollectorNotFoundException(metricName);
        }
    }

    /**
     * Gets sdk metric by metric name.
     *
     * @param metricName the metric name
     * @return the sdk metric by metric name
     * @throws CloudWatchResourceNotFoundException.SdkMetricNotFoundException if there is no sdk metric for the given metric name
     */
    public SdkMetric<?> getSdkMetricByMetricName(String metricName) {
        try {
            return cloudWatchSdkMetrics.get(metricName);
        } catch (NullPointerException e) {
            throw new CloudWatchResourceNotFoundException.SdkMetricNotFoundException(metricName);
        }
    }

    /**
     * Init method to initialize MetricPublishers.
     * If maximumCallsPerUpload and uploadFrequencyMillis fields are null, CloudWatchMetricPublisher class will use its default values.
     */
    private void initCloudWatchMetricPublishers() {
        cloudWatchMetricPublishers.put(arubaPecNamespace, new CloudWatchMetricsPublisherWrapper(arubaPecNamespace, maximumCallsPerUpload, Duration.ofMillis(uploadFrequencyMillis), cloudWatchAsyncClient));
        cloudWatchMetricPublishers.put(namirialPecNamespace, new CloudWatchMetricsPublisherWrapper(namirialPecNamespace, maximumCallsPerUpload, Duration.ofMillis(uploadFrequencyMillis), cloudWatchAsyncClient));
    }

    /**
     * Init method to initialize SdkMetrics
     */
    private void initCloudWatchSdkMetrics() {
        cloudWatchSdkMetrics.put(markMessageAsReadResponseTimeMetric, SdkMetric.create(markMessageAsReadResponseTimeMetric, Long.class, MetricLevel.INFO, MetricCategory.HTTP_CLIENT));
        cloudWatchSdkMetrics.put(deleteMessageResponseTimeMetric, SdkMetric.create(deleteMessageResponseTimeMetric, Long.class, MetricLevel.INFO, MetricCategory.HTTP_CLIENT));
    }

    /**
     * Init method to initialize MetricCollectors
     */
    private void initCloudWatchMetricCollectors() {
        cloudWatchMetricCollectors.put(markMessageAsReadResponseTimeMetric, MetricCollector.create(markMessageAsReadResponseTimeMetric));
        cloudWatchMetricCollectors.put(deleteMessageResponseTimeMetric, MetricCollector.create(deleteMessageResponseTimeMetric));
    }

}