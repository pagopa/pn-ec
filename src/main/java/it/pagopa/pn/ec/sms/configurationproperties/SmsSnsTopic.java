package it.pagopa.pn.ec.sms.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sns.topic.sms")
public record SmsSnsTopic(String externalNotificationName, String externalNotificationArn) {}
