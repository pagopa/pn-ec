package it.pagopa.pn.ec.testutils.configuration;

import io.awspring.cloud.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Configuration class for integration test that do not need message listening capabilities.
 */
@TestConfiguration
public class MockMessageListenerConfiguration {

    @MockitoBean
    private SimpleMessageListenerContainer messageListenerContainer;
}
