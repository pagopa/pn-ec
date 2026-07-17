package it.pagopa.pn.ec.commons;


import it.pagopa.pn.ec.commons.configuration.RicezioneEsitiCartaceoConfiguration;
import it.pagopa.pn.ec.commons.constant.DuplicatesCheckMode;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Map;

@SpringBootTestWebEnv
@TestPropertySource(properties = "ricezione-esiti-cartaceo.duplicates-check=productType1:NONBLOCKING;productType2;productType3")
@CustomLog
class RicezioneEsitiCartaceoConfigurationTest {

    @Autowired
    private RicezioneEsitiCartaceoConfiguration config;

    @Test
    void checkValues(){
        String[] expectedProducts = {"productType1", "productType2", "productType3"};
        Assertions.assertEquals(true,config.isConsiderEventsWithoutStatusAsBooked());
        Assertions.assertEquals(Duration.ofMinutes(1),config.getOffsetDuration());
        Assertions.assertArrayEquals(expectedProducts,config.getProductTypesToCheck());
    }

    @Test
    void getDuplicatesCheckMode_productConfiguredAsNonblocking_returnsNonblocking(){
        Assertions.assertEquals(DuplicatesCheckMode.NONBLOCKING, config.getDuplicatesCheckMode("productType1"));
    }

    @Test
    void getDuplicatesCheckMode_productConfiguredWithoutSuffix_returnsBlocking(){
        Assertions.assertEquals(DuplicatesCheckMode.BLOCKING, config.getDuplicatesCheckMode("productType2"));
        Assertions.assertEquals(DuplicatesCheckMode.BLOCKING, config.getDuplicatesCheckMode("productType3"));
    }

    @Test
    void getDuplicatesCheckMode_productNotConfigured_returnsNotConfigured(){
        Assertions.assertEquals(DuplicatesCheckMode.NOT_CONFIGURED, config.getDuplicatesCheckMode("productTypeNeverConfigured"));
    }

    @Test
    void getDuplicatesCheckModeByProduct_returnsExpectedMap(){
        Map<String, DuplicatesCheckMode> expected = Map.of(
                "productType1", DuplicatesCheckMode.NONBLOCKING,
                "productType2", DuplicatesCheckMode.BLOCKING,
                "productType3", DuplicatesCheckMode.BLOCKING
        );
        Assertions.assertEquals(expected, config.getDuplicatesCheckModeByProduct());
    }

}
