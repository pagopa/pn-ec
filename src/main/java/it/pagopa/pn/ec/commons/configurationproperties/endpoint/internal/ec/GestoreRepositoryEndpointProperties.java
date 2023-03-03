package it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ec;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-endpoint.ec.gestore-repository")
public record GestoreRepositoryEndpointProperties(

//      <-- CLIENT CONFIGURATION -->
        String getClientConfiguration, String postClientConfiguration, String putClientConfiguration, String deleteClientConfiguration,

//      <-- REQUEST -->
        String getRequest, String postRequest, String patchRequest, String deleteRequest, String getRequestByMessageId,
        String setMessageIdInRequestMetadata) {}

