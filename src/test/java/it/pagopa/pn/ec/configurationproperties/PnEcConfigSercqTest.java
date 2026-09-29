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
        "pn.ec.sercq.receiver-digital-address=sercq@pn.pagopa.it"
})
class PnEcConfigSercqTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    @Test
    void shouldBindReceiverDigitalAddress() {
        assertThat(pnEcConfig.getSercq().getReceiverDigitalAddress()).isEqualTo("sercq@pn.pagopa.it");
    }
}
