package it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.pdfraster;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-endpoint.pdfraster")
public record PdfRasterEndpointProperties(String convertPdf) {


}
