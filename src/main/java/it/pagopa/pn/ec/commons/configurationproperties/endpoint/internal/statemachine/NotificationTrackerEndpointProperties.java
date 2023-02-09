package it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.statemachine;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-endpoint.nt")
public record NotificationTrackerEndpointProperties(String containerBasePath) {}
