package it.pagopa.pn.ec.commons.configuration;

import it.pagopa.pn.ec.commons.constant.DuplicatesCheckMode;
import lombok.CustomLog;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@CustomLog
@Configuration
@Getter
public class RicezioneEsitiCartaceoConfiguration {
    @Getter
    @Value("${ricezione-esiti-cartaceo.consider-event-without-sent-status-as-booked}")
    private boolean considerEventsWithoutStatusAsBooked;
    @Value("${ricezione-esiti-cartaceo.duplicates-check:}")
    private String duplicatesCheck;
    @Getter
    @Value("${ricezione-esiti-cartaceo.allowed-future-offset-duration}")
    private Duration offsetDuration;

    @Value("${ricezione-esiti-cartaceo.duplicated-event-error-code}")
    private String duplicatedEventErrorCode;

    @Getter
    private String[] productTypesToCheck;

    @Getter
    private Map<String, DuplicatesCheckMode> duplicatesCheckModeByProduct;

    @PostConstruct
    public void init() {
        this.productTypesToCheck = Arrays.stream(this.duplicatesCheck.split(";"))
                .map(token -> token.split(":", 2)[0].trim())
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
        this.duplicatesCheckModeByProduct = parseDuplicatesCheckModeByProduct(this.duplicatesCheck);
        this.offsetDuration = this.offsetDuration == null ? Duration.ofSeconds(-1) : this.offsetDuration;
        log.info("PostConstruct - RicezioneEsitiCartaceoConfiguration duplicatedEventErrorCode: {}",this.duplicatedEventErrorCode);
    }

    private static Map<String, DuplicatesCheckMode> parseDuplicatesCheckModeByProduct(String duplicatesCheck) {
        Map<String, DuplicatesCheckMode> result = new HashMap<>();
        if (duplicatesCheck == null || duplicatesCheck.isBlank()) {
            return result;
        }
        for (String token : duplicatesCheck.split(";")) {
            if (token == null || token.isBlank()) {
                continue;
            }
            String[] parts = token.split(":", 2);
            String productType = parts[0].trim();
            if (productType.isBlank()) {
                continue;
            }
            DuplicatesCheckMode mode = parts.length > 1 && "NONBLOCKING".equalsIgnoreCase(parts[1].trim())
                    ? DuplicatesCheckMode.NONBLOCKING
                    : DuplicatesCheckMode.BLOCKING;
            result.put(productType, mode);
        }
        return result;
    }

    public DuplicatesCheckMode getDuplicatesCheckMode(String productType) {
        return this.duplicatesCheckModeByProduct.getOrDefault(productType, DuplicatesCheckMode.NOT_CONFIGURED);
    }

}
