package it.pagopa.pn.ec.commons.configuration.normalization;

import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class NormalizationConfiguration {

    private final PnEcConfig.Cartaceo.Paper transformationProperties;

    public NormalizationConfiguration(PnEcConfig pnEcConfig) {
        this.transformationProperties = pnEcConfig.getCartaceo().getPaper();
    }


    /** true se la normalizzazione è abilitata per la PA indicata */
    public boolean isNormalizationEnabled(String paId) {
        String paIdToNormalize = transformationProperties.getPaIdToNormalize();
        String cfg = paIdToNormalize == null || paIdToNormalize.isBlank()
                ? "NOTHING" : paIdToNormalize;

        return switch (cfg) {
            case "ALL"     -> true;
            case "NOTHING" -> false;
            default        -> Arrays.stream(cfg.split(";")).anyMatch(paId::equals);
        };
    }
}
