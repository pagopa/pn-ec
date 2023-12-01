package it.pagopa.pn.library.pec.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aruba-call-retry-strategy")
public record ArubaServiceProperties(String maxAttempts, String minBackoff) {

}
