package it.pagopa.pn.ec.testutils.localstack;

import it.pagopa.pn.ec.commons.configurationproperties.AwsConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@TestConfiguration
public class LocalStackClientConfig {

    @Autowired
    private AwsConfigurationProperties awsConfigurationProperties;
    @Value("${test.aws.s3.endpoint}")
    String s3LocalStackEndpoint;

    @Bean
    public S3Client s3TestClient() {
        return S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.of(awsConfigurationProperties.regionCode()))
                .endpointOverride(URI.create(s3LocalStackEndpoint))
                .build();
    }

}
