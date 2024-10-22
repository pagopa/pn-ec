package it.pagopa.pn.ec.commons.configurationproperties.sqs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sqs.queue.notification-tracker")
public record NotificationTrackerSqsName(

//      SMS
        String statoSmsName, String statoSmsErratoName, String statoSmsDlqName,

//      EMAIL
        String statoEmailName, String statoEmailErratoName, String statoEmailDlqName,

//      PEC
        String statoPecName, String statoPecErratoName, String statoPecDlqName,

//      CARTACEO
        String statoCartaceoName, String statoCartaceoErratoName, String statoCartaceoDlqName,

//      SERCQ
        String statoSercqName, String statoSercqErratoName,

//      COMMONS
        Integer delaySeconds, Long elapsedTimeSeconds) {
}
