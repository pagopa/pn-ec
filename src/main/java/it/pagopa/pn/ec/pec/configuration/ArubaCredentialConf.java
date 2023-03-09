package it.pagopa.pn.ec.pec.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.pec.model.pojo.ArubaSecretValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Configuration
public class ArubaCredentialConf {

    @Value("${ArubaPecUsername:#{null}}")
    String arubaPecUsername;

    @Value("${ArubaPecPassword:#{null}}")
    String arubaPecPassword;

    @Bean
    public ArubaSecretValue arubaCredentialProvider(SecretsManagerClient secretsManagerClient, ObjectMapper objectMapper)
            throws JsonProcessingException {
        if (arubaPecUsername != null && arubaPecPassword != null) {
            return new ArubaSecretValue(arubaPecUsername, arubaPecPassword);
        } else {
            String secretStringJson = secretsManagerClient.getSecretValue(builder -> builder.secretId("pn/identity/pec")).secretString();
            return objectMapper.readValue(secretStringJson, ArubaSecretValue.class);
        }
    }
}
