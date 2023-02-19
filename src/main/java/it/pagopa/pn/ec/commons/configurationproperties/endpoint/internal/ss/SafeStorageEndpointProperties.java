package it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ss;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-endpoint.ss")
public record SafeStorageEndpointProperties(String containerBaseUrl, String clientHeaderName, String clientHeaderValue,
                                            String apiKeyHeaderName, String apiKeyHeaderValue) {}
