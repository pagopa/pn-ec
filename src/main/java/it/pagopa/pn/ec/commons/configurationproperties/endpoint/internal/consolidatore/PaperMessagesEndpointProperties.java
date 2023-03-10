package it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.consolidatore;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-endpoint.consolidatore.paper-messages")
public record PaperMessagesEndpointProperties(String putRequest, String putDuplicateRequest, String getRequest,
                                              String getDuplicateRequest) {}
