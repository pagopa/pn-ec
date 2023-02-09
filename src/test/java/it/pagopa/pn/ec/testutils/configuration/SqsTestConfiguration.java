package it.pagopa.pn.ec.testutils.configuration;

import it.pagopa.pn.ec.commons.configurationproperties.AwsConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@TestConfiguration
public class SqsTestConfiguration {


    private final AwsConfigurationProperties awsConfigurationProperties;

    /**
     * Set in LocalStackTestConfig
     */
    @Value("${test.aws.sqs.endpoint}")
    String sqsLocalStackEndpoint;

    public SqsTestConfiguration(AwsConfigurationProperties awsConfigurationProperties) {
        this.awsConfigurationProperties = awsConfigurationProperties;
    }

//  <-- AWS SDK for Java v2 -->

    @Bean
    public SqsClient sqsTestClient() {
        return SqsClient.builder()
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .region(Region.of(awsConfigurationProperties.regionCode()))
                        .endpointOverride(URI.create(sqsLocalStackEndpoint))
                        .build();
    }
}
