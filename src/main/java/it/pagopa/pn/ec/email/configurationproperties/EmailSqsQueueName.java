package it.pagopa.pn.ec.email.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sqs.queue.email")
public record EmailSqsQueueName(String batchName, String interactiveName, String errorName, String dlqErrorName) {}
