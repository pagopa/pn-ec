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
        "pn.ec.pec.attachment-rule=NONE",
        "pn.ec.pec.max-message-size-mb=50",
        "pn.ec.pec.tipo-ricevuta-header-name=X-TipoRicevuta",
        "pn.ec.pec.tipo-ricevuta-header-value=breve",
        "pn.ec.pec.max-thread-pool-size=8",
        "pn.ec.pec.sqs-queue.batch-name=pn-ec_pec_batch",
        "pn.ec.pec.sqs-queue.interactive-name=pn-ec_pec_interactive",
        "pn.ec.pec.sqs-queue.error-name=pn-ec_pec_error",
        "pn.ec.pec.retry-strategy.max-attempts=5",
        "pn.ec.pec.retry-strategy.min-backoff=2"
})
class PnEcConfigPecTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    @Test
    void shouldBindTopLevelProperties() {
        var pec = pnEcConfig.getPec();
        assertThat(pec.getAttachmentRule()).isEqualTo("NONE");
        assertThat(pec.getMaxMessageSizeMb()).isEqualTo(50);
        assertThat(pec.getTipoRicevutaHeaderName()).isEqualTo("X-TipoRicevuta");
        assertThat(pec.getTipoRicevutaHeaderValue()).isEqualTo("breve");
        assertThat(pec.getMaxThreadPoolSize()).isEqualTo(8);
    }

    @Test
    void shouldBindSqsQueueProperties() {
        var sqsQueue = pnEcConfig.getPec().getSqsQueue();
        assertThat(sqsQueue.getBatchName()).isEqualTo("pn-ec_pec_batch");
        assertThat(sqsQueue.getInteractiveName()).isEqualTo("pn-ec_pec_interactive");
        assertThat(sqsQueue.getErrorName()).isEqualTo("pn-ec_pec_error");
    }

    @Test
    void shouldBindRetryStrategyProperties() {
        var retryStrategy = pnEcConfig.getPec().getRetryStrategy();
        assertThat(retryStrategy.getMaxAttempts()).isEqualTo("5");
        assertThat(retryStrategy.getMinBackoff()).isEqualTo("2");
    }
}
