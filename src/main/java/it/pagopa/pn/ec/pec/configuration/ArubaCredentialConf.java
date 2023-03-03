package it.pagopa.pn.ec.pec.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.pec.model.pojo.ArubaSecretValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Configuration
public class ArubaCredentialConf {

    @Bean
    public ArubaSecretValue arubaCredentialProvider(SecretsManagerClient secretsManagerClient, ObjectMapper objectMapper)
            throws JsonProcessingException {
        String secretStringJson = secretsManagerClient.getSecretValue(builder -> builder.secretId("deploykey/pn-ec")).secretString();
        return objectMapper.readValue(secretStringJson, ArubaSecretValue.class);
    }
}
