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
        "pn.ec.sms.max-thread-pool-size=12",
        "pn.ec.sms.stress-test.mode=true",
        "pn.ec.sms.stress-test.topic-arn=arn:aws:sns:eu-south-1:123456789012:stress-test",
        "pn.ec.sms.sqs-queue.batch-name=pn-ec_sms_batch",
        "pn.ec.sms.sqs-queue.interactive-name=pn-ec_sms_interactive",
        "pn.ec.sms.sqs-queue.error-name=pn-ec_sms_error",
        "pn.ec.sms.sns-topic.default-sender-id-key=AWS.SNS.SMS.SenderID",
        "pn.ec.sms.sns-topic.default-sender-id-value=PagoPA",
        "pn.ec.sms.sns-topic.default-sender-id-type=String"
})
class PnEcConfigSmsTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    @Test
    void shouldBindTopLevelProperties() {
        var sms = pnEcConfig.getSms();
        assertThat(sms.getMaxThreadPoolSize()).isEqualTo(12);
        assertThat(sms.getStressTestMode()).isTrue();
        assertThat(sms.getStressTestTopicArn()).isEqualTo("arn:aws:sns:eu-south-1:123456789012:stress-test");
    }

    @Test
    void shouldBindSqsQueueProperties() {
        var sqsQueue = pnEcConfig.getSms().getSqsQueue();
        assertThat(sqsQueue.getBatchName()).isEqualTo("pn-ec_sms_batch");
        assertThat(sqsQueue.getInteractiveName()).isEqualTo("pn-ec_sms_interactive");
        assertThat(sqsQueue.getErrorName()).isEqualTo("pn-ec_sms_error");
    }

    @Test
    void shouldBindSnsTopicProperties() {
        var snsTopic = pnEcConfig.getSms().getSnsTopic();
        assertThat(snsTopic.getDefaultSenderIdKey()).isEqualTo("AWS.SNS.SMS.SenderID");
        assertThat(snsTopic.getDefaultSenderIdValue()).isEqualTo("PagoPA");
        assertThat(snsTopic.getDefaultSenderIdType()).isEqualTo("String");
    }
}
