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
        "pn.ec.notification-tracker.sqs-queue.stato-sms-name=pn-ec_stato_sms",
        "pn.ec.notification-tracker.sqs-queue.stato-sms-errato-name=pn-ec_stato_sms_errato",
        "pn.ec.notification-tracker.sqs-queue.stato-email-name=pn-ec_stato_email",
        "pn.ec.notification-tracker.sqs-queue.stato-email-errato-name=pn-ec_stato_email_errato",
        "pn.ec.notification-tracker.sqs-queue.stato-pec-name=pn-ec_stato_pec",
        "pn.ec.notification-tracker.sqs-queue.stato-pec-errato-name=pn-ec_stato_pec_errato",
        "pn.ec.notification-tracker.sqs-queue.stato-cartaceo-name=pn-ec_stato_cartaceo",
        "pn.ec.notification-tracker.sqs-queue.stato-cartaceo-errato-name=pn-ec_stato_cartaceo_errato",
        "pn.ec.notification-tracker.sqs-queue.stato-sercq-name=pn-ec_stato_sercq",
        "pn.ec.notification-tracker.sqs-queue.stato-sercq-errato-name=pn-ec_stato_sercq_errato",
        "pn.ec.notification-tracker.sqs-queue.elapsed-time-seconds=60",
        "pn.ec.notification-tracker.event-bridge.notifications-bus-name=pn-ec_notifications_bus"
})
class PnEcConfigNotificationTrackerTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    @Test
    void shouldBindSqsQueueProperties() {
        var sqsQueue = pnEcConfig.getNotificationTracker().getSqsQueue();
        assertThat(sqsQueue.getStatoSmsName()).isEqualTo("pn-ec_stato_sms");
        assertThat(sqsQueue.getStatoSmsErratoName()).isEqualTo("pn-ec_stato_sms_errato");
        assertThat(sqsQueue.getStatoEmailName()).isEqualTo("pn-ec_stato_email");
        assertThat(sqsQueue.getStatoEmailErratoName()).isEqualTo("pn-ec_stato_email_errato");
        assertThat(sqsQueue.getStatoPecName()).isEqualTo("pn-ec_stato_pec");
        assertThat(sqsQueue.getStatoPecErratoName()).isEqualTo("pn-ec_stato_pec_errato");
        assertThat(sqsQueue.getStatoCartaceoName()).isEqualTo("pn-ec_stato_cartaceo");
        assertThat(sqsQueue.getStatoCartaceoErratoName()).isEqualTo("pn-ec_stato_cartaceo_errato");
        assertThat(sqsQueue.getStatoSercqName()).isEqualTo("pn-ec_stato_sercq");
        assertThat(sqsQueue.getStatoSercqErratoName()).isEqualTo("pn-ec_stato_sercq_errato");
        assertThat(sqsQueue.getElapsedTimeSeconds()).isEqualTo(60L);
    }

    @Test
    void shouldBindEventBridgeProperties() {
        assertThat(pnEcConfig.getNotificationTracker().getEventBridge().getNotificationsBusName())
                .isEqualTo("pn-ec_notifications_bus");
    }
}
