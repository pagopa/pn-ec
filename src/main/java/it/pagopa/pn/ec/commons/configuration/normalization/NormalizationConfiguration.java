package it.pagopa.pn.ec.commons.configuration.normalization;

import it.pagopa.pn.ec.cartaceo.configurationproperties.TransformationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class NormalizationConfiguration {

    TransformationProperties properties;

    public NormalizationConfiguration(TransformationProperties properties) {
        this.properties = properties;
    }


    /** true se la normalizzazione è abilitata per la PA indicata */
    public boolean isNormalizationEnabled(String paId) {
        String paIdToNormalize = properties.paIdToNormalize();
        String cfg = paIdToNormalize == null || paIdToNormalize.isBlank()
                ? "NOTHING" : paIdToNormalize;

        return switch (cfg) {
            case "ALL"     -> true;
            case "NOTHING" -> false;
            default        -> Arrays.stream(cfg.split(";")).anyMatch(paId::equals);
        };
    }
}
