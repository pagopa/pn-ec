package it.pagopa.pn.ec.notificationtracker.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eventbridge.event.notificationtracker")
public record NotificationTrackerEventBridgeEventName(String notificationsBusName) {}
