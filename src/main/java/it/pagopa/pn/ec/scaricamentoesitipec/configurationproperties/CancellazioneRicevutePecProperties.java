package it.pagopa.pn.ec.scaricamentoesitipec.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cancellazione-ricevute-pec")
public record CancellazioneRicevutePecProperties(String sqsQueueName) {
}
