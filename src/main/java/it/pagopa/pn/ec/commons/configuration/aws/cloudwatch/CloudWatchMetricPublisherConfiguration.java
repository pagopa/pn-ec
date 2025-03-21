package it.pagopa.pn.ec.commons.configuration.aws.cloudwatch;

import it.pagopa.pn.ec.commons.exception.cloudwatch.CloudWatchResourceNotFoundException;
import it.pagopa.pn.ec.commons.model.pojo.cloudwatch.CloudWatchMetricsPublisherWrapper;
import it.pagopa.pn.library.pec.configurationproperties.PnPecMetricNames;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.metrics.*;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
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
    private final CloudWatchAsyncClient cloudWatchAsyncClient;
    private final Map<String, CloudWatchMetricsPublisherWrapper> cloudWatchMetricPublishers = new HashMap<>();
    private final Map<String, SdkMetric<?>> cloudWatchSdkMetrics = new HashMap<>();
    private final PnPecMetricNames pnPecMetricNames;

    /**
     * Instantiates a new CloudWatchMetricPublisherConfiguration.
     *
     * @param cloudWatchAsyncClient the cloud watch async client
     * @param pnPecMetricNames the names of the CloudWatch metrics
     */
    @Autowired
    public CloudWatchMetricPublisherConfiguration(CloudWatchAsyncClient cloudWatchAsyncClient, PnPecMetricNames pnPecMetricNames) {
        this.cloudWatchAsyncClient = cloudWatchAsyncClient;
        this.pnPecMetricNames = pnPecMetricNames;
    }

    /**
     * Init method to initialize MetricPublishers, SdkMetrics and MetricCollectors
     */
    @PostConstruct
    private void init() {
        log.debug("Initializing CloudWatchMetricPublisher configurations.");
        initCloudWatchSdkMetrics();
        initCloudWatchMetricPublishers();
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
        SdkMetric<String> payloadSizeRangeDimension = (SdkMetric<String>) cloudWatchSdkMetrics.get(pnPecMetricNames.getPayloadSizeRange());
        SdkMetric<String> messageCountRangeDimension = (SdkMetric<String>) cloudWatchSdkMetrics.get(pnPecMetricNames.getMessageCountRange());
        List<SdkMetric<String>> dimensions = List.of(payloadSizeRangeDimension, messageCountRangeDimension);
        cloudWatchMetricPublishers.put(arubaPecNamespace, new CloudWatchMetricsPublisherWrapper(arubaPecNamespace, maximumCallsPerUpload, Duration.ofMillis(uploadFrequencyMillis), cloudWatchAsyncClient, dimensions));
        cloudWatchMetricPublishers.put(namirialPecNamespace, new CloudWatchMetricsPublisherWrapper(namirialPecNamespace, maximumCallsPerUpload, Duration.ofMillis(uploadFrequencyMillis), cloudWatchAsyncClient, dimensions));
    }

    /**
     * Init method to initialize SdkMetrics
     */
    private void initCloudWatchSdkMetrics() {
        pnPecMetricNames.getAllMetrics().forEach(metricName -> cloudWatchSdkMetrics.put(metricName, SdkMetric.create(metricName, Long.class, MetricLevel.INFO, MetricCategory.HTTP_CLIENT)));
    }

}