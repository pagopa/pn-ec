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
        "pn.ec.pdfraster.max-retry-attempts=3",
        "pn.ec.pdfraster.min-retry-backoff=1",
        "pn.ec.pdfraster.pdf-conversion-expiration-offset-in-days=7",
        "pn.ec.pdfraster.endpoint.base-url=http://pn-ec-pdfraster:8080",
        "pn.ec.pdfraster.endpoint.client-header-value=pn-ec",
        "pn.ec.pdfraster.endpoint.client-header-api-key=api-key"
})
class PnEcConfigPdfRasterTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    @Test
    void shouldBindTopLevelProperties() {
        var pdfRaster = pnEcConfig.getPdfRaster();
        assertThat(pdfRaster.getMaxRetryAttempts()).isEqualTo(3L);
        assertThat(pdfRaster.getMinRetryBackoff()).isEqualTo(1L);
        assertThat(pdfRaster.getPdfConversionExpirationOffsetInDays()).isEqualTo(7);
    }

    @Test
    void shouldBindEndpointProperties() {
        var endpoint = pnEcConfig.getPdfRaster().getEndpoint();
        assertThat(endpoint.getBaseUrl()).isEqualTo("http://pn-ec-pdfraster:8080");
        assertThat(endpoint.getClientHeaderValue()).isEqualTo("pn-ec");
        assertThat(endpoint.getClientHeaderApiKey()).isEqualTo("api-key");
    }
}
