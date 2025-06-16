package it.pagopa.pn.ec.commons.configuration.normalization;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@Getter
public class NormalizationConfiguration {

    /** PnECPaperPAIdToNormalize (NOTHING | ALL | lista ';') */
    @Value("${PnECPaperPAIdToNormalize:NOTHING}")
    private String paIdToNormalize;

    /** TransformationPriority (NORMALIZATION | RASTERIZATION | lista ',') */
    @Value("${TransformationPriority:NORMALIZATION}")
    private String transformationPriority;

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
