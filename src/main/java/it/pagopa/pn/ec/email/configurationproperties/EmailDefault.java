package it.pagopa.pn.ec.email.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "email")
public record EmailDefault(String defaultSenderAddress) {
}
