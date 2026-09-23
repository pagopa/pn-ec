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
        "pn.ec.cancellazione-ricevute-pec.sqs-queue-name=pn-ec_cancellazione_ricevute_pec",
        "pn.ec.cancellazione-ricevute-pec.max-thread-pool-size=4"
})
class PnEcConfigCancellazioneRicevutePecTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    @Test
    void shouldBindProperties() {
        var cancellazioneRicevutePec = pnEcConfig.getCancellazioneRicevutePec();
        assertThat(cancellazioneRicevutePec.getSqsQueueName()).isEqualTo("pn-ec_cancellazione_ricevute_pec");
        assertThat(cancellazioneRicevutePec.getMaxThreadPoolSize()).isEqualTo(4);
    }
}
