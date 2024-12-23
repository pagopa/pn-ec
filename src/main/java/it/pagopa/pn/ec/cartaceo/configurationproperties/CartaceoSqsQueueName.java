package it.pagopa.pn.ec.cartaceo.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sqs.queue.cartaceo")
public record CartaceoSqsQueueName(String batchName, String errorName, String dlqErrorName) {}
//    String interactiveName,