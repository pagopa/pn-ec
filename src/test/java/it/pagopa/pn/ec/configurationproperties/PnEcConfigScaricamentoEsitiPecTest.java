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
        "pn.ec.scaricamento-esiti-pec.get-messages-limit=100",
        "pn.ec.scaricamento-esiti-pec.sqs-queue-name=pn-ec_scaricamento_esiti_pec",
        "pn.ec.scaricamento-esiti-pec.client-header-value=pn-ec",
        "pn.ec.scaricamento-esiti-pec.api-key-header-value=api-key",
        "pn.ec.scaricamento-esiti-pec.limit-rate=10",
        "pn.ec.scaricamento-esiti-pec.dump-email=dump@pn.pagopa.it",
        "pn.ec.scaricamento-esiti-pec.lavorazione.max-thread-pool-size=6"
})
class PnEcConfigScaricamentoEsitiPecTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    @Test
    void shouldBindTopLevelProperties() {
        var scaricamentoEsitiPec = pnEcConfig.getScaricamentoEsitiPec();
        assertThat(scaricamentoEsitiPec.getGetMessagesLimit()).isEqualTo("100");
        assertThat(scaricamentoEsitiPec.getSqsQueueName()).isEqualTo("pn-ec_scaricamento_esiti_pec");
        assertThat(scaricamentoEsitiPec.getClientHeaderValue()).isEqualTo("pn-ec");
        assertThat(scaricamentoEsitiPec.getApiKeyHeaderValue()).isEqualTo("api-key");
        assertThat(scaricamentoEsitiPec.getLimitRate()).isEqualTo(10);
        assertThat(scaricamentoEsitiPec.getDumpEmail()).isEqualTo("dump@pn.pagopa.it");
    }

    @Test
    void shouldBindLavorazioneProperties() {
        assertThat(pnEcConfig.getScaricamentoEsitiPec().getLavorazione().getMaxThreadPoolSize()).isEqualTo(6);
    }
}
