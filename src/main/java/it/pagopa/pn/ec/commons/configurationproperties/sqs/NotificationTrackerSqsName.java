package it.pagopa.pn.ec.commons.configurationproperties.sqs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sqs.queue.notification-tracker")
public record NotificationTrackerSqsName(

//      SMS
        String statoSmsName, String statoSmsErratoName,

//      EMAIL
        String statoEmailName, String statoEmailErratoName,

//      PEC
        String statoPecName, String statoPecErratoName,

//      CARTACEO
        String statoCartaceoName, String statoCartaceoErratoName) {}
