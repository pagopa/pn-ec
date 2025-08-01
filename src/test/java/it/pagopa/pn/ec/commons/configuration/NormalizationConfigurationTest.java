package it.pagopa.pn.ec.commons.configuration;

import it.pagopa.pn.ec.cartaceo.configurationproperties.TransformationProperties;
import it.pagopa.pn.ec.commons.configuration.normalization.NormalizationConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = NormalizationConfiguration.class)
@EnableConfigurationProperties(TransformationProperties.class)
@TestPropertySource(properties = {
        "PN_EC_PAPER_PA_ID_TO_NORMALIZE=PA1;PA2",
        "TRANSFORMATION_PRIORITY=RASTERIZATION"
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

