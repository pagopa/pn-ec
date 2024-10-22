package it.pagopa.pn.ec.commons.configurationproperties.sqs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pn.ec.sqs.retry.strategy")
public record SqsRetryStrategyProperties(Long maxAttempts, Long minBackoff) {
}
