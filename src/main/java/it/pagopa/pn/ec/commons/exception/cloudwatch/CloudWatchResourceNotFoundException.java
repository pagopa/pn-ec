package it.pagopa.pn.ec.commons.exception.cloudwatch;

public class CloudWatchResourceNotFoundException extends RuntimeException {

    public CloudWatchResourceNotFoundException() {
        super("CloudWatch resource not found.");
    }

    public CloudWatchResourceNotFoundException(String message) {
        super(message);
    }

    public static class MetricPublisherNotFoundException extends CloudWatchResourceNotFoundException {
        public MetricPublisherNotFoundException(String namespace) {
            super(String.format("MetricPublisher with namespace '%s' has not been found.", namespace));
        }
    }

    public static class MetricCollectorNotFoundException extends CloudWatchResourceNotFoundException {
        public MetricCollectorNotFoundException(String metricName) {
            super(String.format("MetricCollector with name '%s' has not been found.", metricName));
        }
    }

    public static class SdkMetricNotFoundException extends CloudWatchResourceNotFoundException {
        public SdkMetricNotFoundException(String metricName) {
            super(String.format("SdkMetric with name '%s' has not been found.", metricName));
        }
    }
}
