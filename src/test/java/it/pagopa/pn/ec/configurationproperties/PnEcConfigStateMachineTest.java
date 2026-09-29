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
        "pn.ec.statemachine.retry-strategy.max-attempts=3",
        "pn.ec.statemachine.retry-strategy.min-backoff=1",
        "pn.ec.statemachine.endpoint.container-base-url=http://pn-ec-statemachine:8080",
        "pn.ec.statemachine.endpoint.validate=/validate",
        "pn.ec.statemachine.endpoint.decode=/decode"
})
class PnEcConfigStateMachineTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    @Test
    void shouldBindRetryStrategyProperties() {
        var retryStrategy = pnEcConfig.getStateMachine().getRetryStrategy();
        assertThat(retryStrategy.getMaxAttempts()).isEqualTo(3L);
        assertThat(retryStrategy.getMinBackoff()).isEqualTo(1L);
    }

    @Test
    void shouldBindEndpointProperties() {
        var endpoint = pnEcConfig.getStateMachine().getEndpoint();
        assertThat(endpoint.getContainerBaseUrl()).isEqualTo("http://pn-ec-statemachine:8080");
        assertThat(endpoint.getValidate()).isEqualTo("/validate");
        assertThat(endpoint.getDecode()).isEqualTo("/decode");
    }
}
