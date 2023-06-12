package it.pagopa.pn.ec.scaricamentoesitipec.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scaricamento-esiti-pec")
public record ScaricamentoEsitiPecProperties(String getMessagesLimit, String sqsQueueName, String clientHeaderValue,
                                             String apiKeyHeaderValue) {
}
