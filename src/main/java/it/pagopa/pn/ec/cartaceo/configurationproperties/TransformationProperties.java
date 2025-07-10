package it.pagopa.pn.ec.cartaceo.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pn.ec.paper")
public record TransformationProperties(
        String documentTypeToTransform,
        String documentTypeForRasterized,
        String documentTypeForNormalized,
        String paIdToRaster,
        String paIdOverride,
        String transformationPriority
){}
