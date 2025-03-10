package it.pagopa.pn.ec.commons.configurationproperties.sm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pn.ec.statemachine.retry.strategy")
public record StateMachineRetryStrategyProperties(Long maxAttempts, Long minBackoff) {
}

