package it.pagopa.pn.ec.commons.configuration.normalization;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class NormalizationConfiguration {

    /** (NOTHING | ALL | lista ';') */
    @Value("${PN_EC_PAPER_PAIDTONORMALIZE:NOTHING}")
    private String paIdToNormalize;

    /** true se la normalizzazione è abilitata per la PA indicata */
    public boolean isNormalizationEnabled(String paId) {
        String cfg = paIdToNormalize == null || paIdToNormalize.isBlank()
                ? "NOTHING" : paIdToNormalize;

        return switch (cfg) {
            case "ALL"     -> true;
            case "NOTHING" -> false;
            default        -> Arrays.stream(cfg.split(";")).anyMatch(paId::equals);
        };
    }
}
