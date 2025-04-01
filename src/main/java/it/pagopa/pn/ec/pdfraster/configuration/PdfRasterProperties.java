package it.pagopa.pn.ec.pdfraster.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pdf-raster")
public record PdfRasterProperties(Long maxRetryAttempts, Long minRetryBackoff, Integer pdfConversionExpirationOffsetInDays) {
}
