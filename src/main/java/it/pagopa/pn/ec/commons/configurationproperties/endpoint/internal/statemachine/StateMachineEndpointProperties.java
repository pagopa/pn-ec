package it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.statemachine;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-endpoint.state-machine")
public record StateMachineEndpointProperties(String containerBaseUrl, String validate) {}
