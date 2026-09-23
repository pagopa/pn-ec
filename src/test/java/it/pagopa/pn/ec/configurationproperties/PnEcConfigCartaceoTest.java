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
        "pn.ec.cartaceo.sqs-queue.batch-name=pn-ec_cartaceo_batch",
        "pn.ec.cartaceo.sqs-queue.error-name=pn-ec_cartaceo_error",
        "pn.ec.cartaceo.sqs-queue.dlq-error-name=pn-ec_cartaceo_error_dlq",
        "pn.ec.cartaceo.lavorazione.max-thread-pool-size=10",
        "pn.ec.cartaceo.lavorazione.max-retry-attempts=3",
        "pn.ec.cartaceo.lavorazione.min-retry-backoff=1",
        "pn.ec.cartaceo.paper.document-type-to-transform=PDF",
        "pn.ec.cartaceo.paper.document-type-for-rasterized=PDF_RASTER",
        "pn.ec.cartaceo.paper.document-type-for-normalized=PDF_NORMALIZED",
        "pn.ec.cartaceo.paper.pa-id-to-raster=paId1",
        "pn.ec.cartaceo.paper.pa-id-to-normalize=paId2",
        "pn.ec.cartaceo.paper.pa-id-override=paId3",
        "pn.ec.cartaceo.paper.transformation-priority=RASTER"
})
class PnEcConfigCartaceoTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    @Test
    void shouldBindSqsQueueProperties() {
        var sqsQueue = pnEcConfig.getCartaceo().getSqsQueue();
        assertThat(sqsQueue.getBatchName()).isEqualTo("pn-ec_cartaceo_batch");
        assertThat(sqsQueue.getErrorName()).isEqualTo("pn-ec_cartaceo_error");
        assertThat(sqsQueue.getDlqErrorName()).isEqualTo("pn-ec_cartaceo_error_dlq");
    }

    @Test
    void shouldBindLavorazioneProperties() {
        var lavorazione = pnEcConfig.getCartaceo().getLavorazione();
        assertThat(lavorazione.getMaxThreadPoolSize()).isEqualTo(10);
        assertThat(lavorazione.getMaxRetryAttempts()).isEqualTo(3L);
        assertThat(lavorazione.getMinRetryBackoff()).isEqualTo(1L);
    }

    @Test
    void shouldBindPaperProperties() {
        var paper = pnEcConfig.getCartaceo().getPaper();
        assertThat(paper.getDocumentTypeToTransform()).isEqualTo("PDF");
        assertThat(paper.getDocumentTypeForRasterized()).isEqualTo("PDF_RASTER");
        assertThat(paper.getDocumentTypeForNormalized()).isEqualTo("PDF_NORMALIZED");
        assertThat(paper.getPaIdToRaster()).isEqualTo("paId1");
        assertThat(paper.getPaIdToNormalize()).isEqualTo("paId2");
        assertThat(paper.getPaIdOverride()).isEqualTo("paId3");
        assertThat(paper.getTransformationPriority()).isEqualTo("RASTER");
    }
}
