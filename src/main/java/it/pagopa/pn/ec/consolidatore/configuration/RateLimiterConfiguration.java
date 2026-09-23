package it.pagopa.pn.ec.consolidatore.configuration;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(
        name = "pn.ec.feature.flag.cartaceo.consolidatore",
        havingValue = "true",
        matchIfMissing = false
)
@Slf4j
public class RateLimiterConfiguration {

    private final int maxRequests;
    private final int refreshPeriodSeconds;

    public RateLimiterConfiguration(PnEcConfig pnEcConfig) {
        var rateLimiterProperties = pnEcConfig.getCommons().getConsolidatore().getRateLimiter();
        this.maxRequests = rateLimiterProperties.getMaxRequests();
        this.refreshPeriodSeconds = rateLimiterProperties.getRefreshPeriodSeconds();
    }

    @Bean(name = "rateLimiterConsolidatore")
    public RateLimiter rateLimiter() {

        RateLimiterConfig config = RateLimiterConfig
                .custom()
                .limitForPeriod(maxRequests) //richieste consentite
                .limitRefreshPeriod(Duration.ofSeconds(refreshPeriodSeconds)) //velocità delle richieste (60s=1m)
                .timeoutDuration(Duration.ZERO) // quanto tempo deve aspettare una richiesta se il limite è stato superato, con zero non aspetta niente
                .build();

        return RateLimiter.of("consolidatore", config);
    }
}
