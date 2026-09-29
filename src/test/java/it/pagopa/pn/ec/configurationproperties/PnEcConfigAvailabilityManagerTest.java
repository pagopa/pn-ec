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
        "pn.ec.availability-manager.queue-name=pn-ec-availabilitymanager-queue"
})
class PnEcConfigAvailabilityManagerTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    @Test
    void shouldBindQueueName() {
        assertThat(pnEcConfig.getAvailabilityManager().getQueueName()).isEqualTo("pn-ec-availabilitymanager-queue");
    }
}
