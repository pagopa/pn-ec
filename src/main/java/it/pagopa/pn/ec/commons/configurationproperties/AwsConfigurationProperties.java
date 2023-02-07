package it.pagopa.pn.ec.commons.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public record AwsConfigurationProperties(String regionCode) {}
