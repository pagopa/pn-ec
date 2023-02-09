package it.pagopa.pn.ec.pec.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sqs.queue.pec")
public record PecSqsQueueName(String batchName, String interactiveName, String errorName) {}
