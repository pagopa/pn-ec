package it.pagopa.pn.ec.configurationproperties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PnEcConfigPostConstructTest {

    @Test
    void shouldLogResolvedConfigurationWithoutThrowing() {
        PnEcConfig pnEcConfig = new PnEcConfig();

        assertDoesNotThrow(pnEcConfig::logConfiguration);
    }
}
