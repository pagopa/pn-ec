package it.pagopa.pn.ec.commons.configuration.secret;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.Map;

@CustomLog
@Configuration
public class PnPecCredentialConf {

    @Value("${aws.region-code}")
    private String regionCode;

    @Value("${pn.ec.identity.pec}")
    private String pnEcIdentityPec;

    @Value("${test.aws.secretsmanager.endpoint:#{null}}")
    private String smLocalStackEndpoint;

    @Value("${test.secret.properties:#{null}}")
    private String testSecretProperties;

    private final ObjectMapper objectMapper;

    private static final DefaultCredentialsProvider DEFAULT_CREDENTIALS_PROVIDER_V2 = DefaultCredentialsProvider.create();

    public PnPecCredentialConf(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void setSecretProperties() throws JsonProcessingException {
        if (testSecretProperties == null) {
            SecretsManagerClient smClient = initializeSmClient();
            log.debug("Getting secrets from Secret Manager...");
            String secretStringJson = smClient.getSecretValue(builder -> builder.secretId(pnEcIdentityPec)).secretString();
            Map<String, String> secretMap = objectMapper.readValue(secretStringJson, Map.class);
            secretMap.forEach(System::setProperty);
            log.debug("All secret properties has been set.");
        }
    }

    private SecretsManagerClient initializeSmClient() {
        SecretsManagerClientBuilder secretsManagerClient = SecretsManagerClient.builder().credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2).region(Region.of(regionCode));

        if (smLocalStackEndpoint != null) {
            secretsManagerClient.endpointOverride(URI.create(smLocalStackEndpoint));
        }

        return secretsManagerClient.build();
    }

}
