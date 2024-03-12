package it.pagopa.pn.library.pec.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pn-pec-retry-strategy")
public record PnPecRetryStrategyProperties(String maxAttempts, String minBackoff) {

}
