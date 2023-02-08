package it.pagopa.pn.ec.testutils.configuration;

import io.awspring.cloud.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Configuration class for integration test that do not need message listening capabilities.
 */
@TestConfiguration
public class MockMessageListenerConfiguration {

    @MockBean
    private SimpleMessageListenerContainer messageListenerContainer;
}
