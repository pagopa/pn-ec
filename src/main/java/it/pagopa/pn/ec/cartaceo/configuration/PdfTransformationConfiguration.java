package it.pagopa.pn.ec.cartaceo.configuration;

import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class PdfTransformationConfiguration {


    private final PnEcConfig.Cartaceo.Paper transformationProperties;

    public PdfTransformationConfiguration(PnEcConfig pnEcConfig) {

        this.transformationProperties = pnEcConfig.getCartaceo().getPaper();
    }

    public List<String> getDocumentTypesToRaster() {
        return Arrays.stream(transformationProperties.getDocumentTypeToTransform().split(";")).toList();
    }

    public String getPaIdToRaster() {
        return transformationProperties.getPaIdToRaster();
    }

    public String getPaIdOverride() {
        return transformationProperties.getPaIdOverride();
    }

    public String getDocumentTypeForRasterized() {
        return transformationProperties.getDocumentTypeForRasterized();
    }

    public String getDocumentTypeForNormalized() {
        return transformationProperties.getDocumentTypeForNormalized();
    }

    public List<String> getValidTransformationDocumentTypes() {
        return List.of(
                getDocumentTypeForRasterized(),
                getDocumentTypeForNormalized()
        );
    }

    public List<String> getTransformationPriorityList() {
        return Arrays.asList(Arrays.stream(transformationProperties.getTransformationPriority().split("\\|"))
                .map(String::strip)
                .toArray(String[]::new)
        );
    }

    public String getTransformationDocumentTypeByPriority() {
        String priority = getTransformationPriorityList().get(0);
        switch (priority) {
            case "NORMALIZATION":
                return getDocumentTypeForNormalized();
            case "RASTERIZATION":
                return getDocumentTypeForRasterized();
            default:
                throw new IllegalArgumentException("Unsupported priority type: " + priority);
        }
    }
}
