package it.pagopa.pn.ec.cartaceo.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lavorazione-cartaceo.raster")
public record RasterProperties(
        String documentTypeToRaster,
        String documentTypeForRasterized,
        String documentTypeForNormalized,
        String paIdToRaster,
        String paIdOverride
){}
