package it.pagopa.pn.ec.commons;


import it.pagopa.pn.ec.commons.configuration.RicezioneEsitiCartaceoConfiguration;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

@SpringBootTestWebEnv
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



}
