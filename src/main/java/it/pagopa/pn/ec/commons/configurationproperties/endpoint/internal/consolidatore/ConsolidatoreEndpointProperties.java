package it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.consolidatore;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-endpoint.consolidatore")
public record ConsolidatoreEndpointProperties(boolean trustAll, String baseUrl, String basePath,
                                              String clientHeaderName, String clientHeaderValue,
                                              String apiKeyHeaderName, String apiKeyHeaderValue) {}
