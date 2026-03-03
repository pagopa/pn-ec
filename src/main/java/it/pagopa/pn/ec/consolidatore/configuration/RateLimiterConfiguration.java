package it.pagopa.pn.ec.consolidatore.configuration;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "pn.ec.consolidatore")
@Slf4j
public class RateLimiterConfiguration {

    private int maxRequests;
    private int refreshPeriodSeconds;


    @Bean
    public RateLimiter rateLimiter() {

        RateLimiterConfig config = RateLimiterConfig
                .custom()
                .limitForPeriod(maxRequests) //richieste consentite
                .limitRefreshPeriod(Duration.ofSeconds(refreshPeriodSeconds)) //velocità delle richieste (60s=1m)
                .timeoutDuration(Duration.ZERO) // non obbligatorio, quanto tempo deve aspettare una richiesta se il limite è stato superato, con zero non aspetta niente
                .build();

        return RateLimiter.of("consolidatore", config);
    }
}
