package it.pagopa.pn.ec.commons.rest.call.aruba;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aruba-call-retry-strategy")
public record ArubaCallProperties(String maxAttempts, String minBackoff) {

}
