package it.pagopa.pn.ec.commons.configuration;

import it.pagopa.pn.ec.commons.configuration.normalization.NormalizationConfiguration;
import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = NormalizationConfiguration.class)
@EnableConfigurationProperties(PnEcConfig.class)
@TestPropertySource(properties = {
        "pn.ec.cartaceo.paper.pa-id-to-normalize=PA1;PA2",
        "pn.ec.cartaceo.paper.transformation-priority=RASTERIZATION"
})
class NormalizationConfigurationTest {

    @Autowired
    NormalizationConfiguration cfg;

    @Test
    void shouldParsePaListCorrectly() {
        assertTrue(cfg.isNormalizationEnabled("PA2"));
        assertFalse(cfg.isNormalizationEnabled("PA3"));
    }
}
