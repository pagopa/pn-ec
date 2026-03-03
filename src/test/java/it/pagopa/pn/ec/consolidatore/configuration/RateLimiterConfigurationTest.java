package it.pagopa.pn.ec.consolidatore.configuration;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiter.Metrics;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(classes = RateLimiterConfiguration.class)
@EnableConfigurationProperties(RateLimiterConfiguration.class)
@TestPropertySource(properties = {
        "pn.ec.consolidatore.max-requests=2",
        "pn.ec.consolidatore.refresh-period-seconds=5"
})
class RateLimiterConfigurationTest {

    @Autowired
    private RateLimiterConfiguration rateLimiterConfiguration;

    @SneakyThrows
    @Test
    void shouldConsumeAndBlockCorrectly() {
        RateLimiter rateLimiter = rateLimiterConfiguration.rateLimiter();
        Metrics metrics = rateLimiter.getMetrics();

        log.info("RateLimiter creato con maxRequestsPerMinute = {} e limitRefreshPeriod = {}", rateLimiter.getRateLimiterConfig().getLimitForPeriod(), rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod());
        log.info("Permessi iniziali: {}", metrics.getAvailablePermissions());

        assertTrue(acquireAndLog(rateLimiter, "Prima richiesta"));
        assertTrue(acquireAndLog(rateLimiter, "Seconda richiesta"));

        //deve fallire
        assertFalse(acquireAndLog(rateLimiter, "Terza richiesta"));

        Thread.sleep(6000);


        assertTrue(acquireAndLog(rateLimiter, "Quarta richiesta"));
        assertTrue(acquireAndLog(rateLimiter, "Quinta richiesta"));
        assertFalse(acquireAndLog(rateLimiter, "Sesta richiesta"));


        log.info("end test");
    }

    @Test
    void shouldResetAfterConfiguredPeriod() throws InterruptedException {
        log.info("Start test reset period");

        RateLimiter rateLimiter = rateLimiterConfiguration.rateLimiter();
        Metrics metrics = rateLimiter.getMetrics();
        RateLimiterConfig config = rateLimiter.getRateLimiterConfig();

        log.info("LimitForPeriod: {}", config.getLimitForPeriod());
        log.info("RefreshPeriod: {}", config.getLimitRefreshPeriod());

        assertTrue(acquireAndLog(rateLimiter, "Prima richiesta"));
        assertTrue(acquireAndLog(rateLimiter, "Seconda richiesta"));

        assertFalse(acquireAndLog(rateLimiter, "Terza richiesta"));

        log.info("Aspetto 6 secondi per il reset");
        Thread.sleep(6000);


        // Ora il contatore deve essere resettato
        int availableAfterReset = metrics.getAvailablePermissions();
        log.info("Permessi dopo reset: {}", availableAfterReset);
        assertEquals(2, availableAfterReset);

        log.info("end test reset");
    }


    private boolean acquireAndLog(RateLimiter rateLimiter, String requestName) {
        Metrics metrics = rateLimiter.getMetrics();
        boolean result = rateLimiter.acquirePermission();
        log.info("{} permessa? {}: permessi rimanenti: {}", requestName, result, metrics.getAvailablePermissions());
        return result;
    }


}