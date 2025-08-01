package it.pagopa.pn.ec.commons.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lavorazione-cartaceo")
public record LavorazioneCartaceoConfigurationProperties(
        Integer maxThreadPoolSize,
        Long maxRetryAttempts,
        Long minRetryBackoff
){}