package it.pagopa.pn.ec.cartaceo.configuration;

import it.pagopa.pn.ec.cartaceo.configurationproperties.TransformationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class PdfTransformationConfiguration {

    private final TransformationProperties transformationProperties;

    public PdfTransformationConfiguration(TransformationProperties transformationProperties) {

        this.transformationProperties = transformationProperties;
    }

    public List<String> getDocumentTypesToRaster() {
        return Arrays.stream(transformationProperties.documentTypeToRaster().split(";")).toList();
    }

    public String getPaIdToRaster() {
        return transformationProperties.paIdToRaster();
    }

    public String getPaIdOverride() {
        return transformationProperties.paIdOverride();
    }

    public String getDocumentTypeForRasterized() {
        return transformationProperties.documentTypeForRasterized();
    }

    public String getDocumentTypeForNormalized() {
        return transformationProperties.documentTypeForNormalized();
    }

    public List<String> getValidTransformationDocumentTypes() {
        return List.of(
                getDocumentTypeForRasterized(),
                getDocumentTypeForNormalized()
        );
    }
}
