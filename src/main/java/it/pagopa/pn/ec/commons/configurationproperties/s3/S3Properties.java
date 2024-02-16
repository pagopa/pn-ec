package it.pagopa.pn.ec.commons.configurationproperties.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pn.ec.s3.retry.strategy")
public record S3Properties(Long maxAttempts, Long minBackoff) {
}

