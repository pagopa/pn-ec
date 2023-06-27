package it.pagopa.pn.ec.commons.configurationproperties.sns;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sns.topic.sms")
public record SnsTopicProperties(String defaultSenderIdKey, String defaultSenderIdValue, String defaultSenderIdType) {
}
