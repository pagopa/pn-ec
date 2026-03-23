package it.pagopa.pn.ec.testutils.configuration;

import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
/**
 * Configuration class for integration test that do not need message listening capabilities.
 */
@TestConfiguration
public class MockMessageListenerConfiguration {

    @MockitoBean
    private MessageListenerContainer messageListenerContainer;
}
