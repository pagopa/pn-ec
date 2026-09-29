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
        "pn.ec.storage.s3.retry-strategy.max-attempts=4",
        "pn.ec.storage.s3.retry-strategy.min-backoff=2",
        "pn.ec.storage.staging-bucket=pn-ec-staging-bucket"
})
class PnEcConfigStorageTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    @Test
    void shouldBindS3RetryStrategyProperties() {
        var retryStrategy = pnEcConfig.getStorage().getS3().getRetryStrategy();
        assertThat(retryStrategy.getMaxAttempts()).isEqualTo(4L);
        assertThat(retryStrategy.getMinBackoff()).isEqualTo(2L);
    }

    @Test
    void shouldBindStagingBucketProperty() {
        assertThat(pnEcConfig.getStorage().getStagingBucket()).isEqualTo("pn-ec-staging-bucket");
    }
}
