package it.pagopa.pn.ec.commons.configuration;

import it.pagopa.pn.ec.commons.configuration.normalization.NormalizationConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = NormalizationConfiguration.class)
@TestPropertySource(properties = {
        "PnECPaperPAIdToNormalize=PA1;PA2",
        "TransformationPriority=RASTERIZATION"
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

