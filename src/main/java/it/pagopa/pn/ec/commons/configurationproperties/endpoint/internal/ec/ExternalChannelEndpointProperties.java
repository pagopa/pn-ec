package it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ec;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-endpoint.ec")
public record ExternalChannelEndpointProperties(String containerBasePath) {}
