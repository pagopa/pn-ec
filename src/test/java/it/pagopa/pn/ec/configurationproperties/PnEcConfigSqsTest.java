package it.pagopa.pn.ec.configurationproperties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PnEcConfig.class)
@EnableConfigurationProperties(PnEcConfig.class)
@TestPropertySource(properties = {
        "pn.ec.sqs.retry-strategy.max-attempts=3",
        "pn.ec.sqs.retry-strategy.min-backoff=1",
        "pn.ec.sqs.timeout.percent=10",
        "pn.ec.sqs.timeout.default-seconds=86400",
        "pn.ec.sqs.timeout.managed-queues=queue1,queue2,queue3",
        "pn.ec.sqs.max-batch-subscribed-msgs=10",
        "pn.ec.sqs.max-message-size=200000"
})
class PnEcConfigSqsTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    @Test
    void shouldBindRetryStrategyProperties() {
        var retryStrategy = pnEcConfig.getSqs().getRetryStrategy();
        assertThat(retryStrategy.getMaxAttempts()).isEqualTo(3L);
        assertThat(retryStrategy.getMinBackoff()).isEqualTo(1L);
    }

    @Test
    void shouldBindTimeoutProperties() {
        var timeout = pnEcConfig.getSqs().getTimeout();
        assertThat(timeout.getPercent()).isEqualTo(10);
        assertThat(timeout.getDefaultSeconds()).isEqualTo(86400L);
        assertThat(timeout.getManagedQueues()).containsExactly("queue1", "queue2", "queue3");
    }

    @Test
    void shouldBindTopLevelProperties() {
        assertThat(pnEcConfig.getSqs().getMaxBatchSubscribedMsgs()).isEqualTo(10);
        assertThat(pnEcConfig.getSqs().getMaxMessageSize()).isEqualTo(200000L);
    }
}
