package it.pagopa.pn.ec.sms.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sqs.queue.sms")
public record SmsSqsQueueName(String batchName, String interactiveName, String errorName) {}
