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
        "pn.ec.email.default-sender-address=send@pn.pagopa.it",
        "pn.ec.email.max-thread-pool-size=15",
        "pn.ec.email.sqs-queue.batch-name=pn-ec_email_batch",
        "pn.ec.email.sqs-queue.interactive-name=pn-ec_email_interactive",
        "pn.ec.email.sqs-queue.error-name=pn-ec_email_error",
        "pn.ec.email.sqs-queue.ses-events-name=pn-ec_email_ses_events",
        "pn.ec.email.ses.events-list-default=Bounce,Complaint"
})
class PnEcConfigEmailTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    @Test
    void shouldBindTopLevelProperties() {
        var email = pnEcConfig.getEmail();
        assertThat(email.getDefaultSenderAddress()).isEqualTo("send@pn.pagopa.it");
        assertThat(email.getMaxThreadPoolSize()).isEqualTo(15);
    }

    @Test
    void shouldBindSqsQueueProperties() {
        var sqsQueue = pnEcConfig.getEmail().getSqsQueue();
        assertThat(sqsQueue.getBatchName()).isEqualTo("pn-ec_email_batch");
        assertThat(sqsQueue.getInteractiveName()).isEqualTo("pn-ec_email_interactive");
        assertThat(sqsQueue.getErrorName()).isEqualTo("pn-ec_email_error");
        assertThat(sqsQueue.getSesEventsName()).isEqualTo("pn-ec_email_ses_events");
    }

    @Test
    void shouldBindSesProperties() {
        assertThat(pnEcConfig.getEmail().getSes().getEventsListDefault()).isEqualTo("Bounce,Complaint");
    }
}
