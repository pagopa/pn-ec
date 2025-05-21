package it.pagopa.pn.ec.commons.configuration.sqs;

import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTestWebEnv
class SqsTimeoutConfigurationPropertiesTest {

    @Autowired
    private SqsTimeoutConfigurationProperties properties;

    @Test
    void shouldLoadPropertiesTest() {
        assertThat(properties.getPercent()).isEqualTo(10);
        assertThat(properties.getManagedQueues()).containsExactly("queue1", "queue2", "queue3");
        assertThat(properties.getDefaultSeconds()).isEqualTo(86400);
    }
}
