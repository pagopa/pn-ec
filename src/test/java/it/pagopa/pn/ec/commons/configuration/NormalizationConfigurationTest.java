package it.pagopa.pn.ec.commons.configuration;

import it.pagopa.pn.ec.commons.configuration.normalization.NormalizationConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = NormalizationConfiguration.class)
@TestPropertySource(properties = {
        "PN_EC_PAPER_PAIDTONORMALIZE=PA1;PA2",
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

