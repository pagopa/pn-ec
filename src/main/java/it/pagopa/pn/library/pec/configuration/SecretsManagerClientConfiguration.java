package it.pagopa.pn.library.pec.configuration;

import lombok.CustomLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

import java.net.URI;

@Configuration
@CustomLog
public class SecretsManagerClientConfiguration {

    @Value("${aws.region-code}")
    private String regionCode;
    @Value("${test.aws.ses.endpoint:#{null}}")
    String sesLocalStackEndpoint;
    @Value("${test.aws.secretsmanager.endpoint:#{null}}")
    String secretsmanagerLocalStackEndpoint;
    private static final DefaultCredentialsProvider DEFAULT_CREDENTIALS_PROVIDER_V2 = DefaultCredentialsProvider.create();

    @Bean
    public SecretsManagerClient smClient() {
        SecretsManagerClientBuilder secretsManagerClient = SecretsManagerClient.builder()
                .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2)
                .region(Region.of(regionCode));

        if (sesLocalStackEndpoint != null) {
            secretsManagerClient.endpointOverride(URI.create(secretsmanagerLocalStackEndpoint));
        }

        return secretsManagerClient.build();
    }

}
