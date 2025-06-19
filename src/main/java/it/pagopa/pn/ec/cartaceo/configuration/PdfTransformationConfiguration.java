package it.pagopa.pn.ec.cartaceo.configuration;

import it.pagopa.pn.ec.cartaceo.configurationproperties.RasterProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class PdfTransformationConfiguration {

    private final RasterProperties rasterProperties;

    public PdfTransformationConfiguration(RasterProperties rasterProperties) {

        this.rasterProperties = rasterProperties;
    }

    public List<String> getDocumentTypesToRaster() {
        return Arrays.stream(rasterProperties.documentTypeToRaster().split(";")).toList();
    }

    public String getPaIdToRaster() {
        return rasterProperties.paIdToRaster();
    }

    public String getPaIdOverride() {
        return rasterProperties.paIdOverride();
    }

    public String getDocumentTypeForRasterized() {
        return rasterProperties.documentTypeForRasterized();
    }

}
