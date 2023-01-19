package it.pagopa.pn.ec.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

@Configuration
public class AwsConfiguration {

    @Value("${aws.config.access.key}")
    String accessKey;

    @Value("${aws.config.secret.key}")
    String secretKey;

    @Value("${aws.config.default.region}")
    String defaultRegion;

    /**
     * Set in SQSLocalStackTestConfig
     */
    @Value("${aws.sqs.test.endpoint:#{null}}")
    String sqsLocalStackEndpoint;

    /**
     * Set in DynamoDbLocalStackTestConfig
     */
    @Value("${aws.dynamodb.test.endpoint:#{null}}")
    String dynamoDbLocalStackEndpoint;

    @Bean
    public SqsClient getSqsClient() {
        SqsClientBuilder sqsClientBuilder = SqsClient.builder()
                                                     .region(Region.of(defaultRegion))
                                                     .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                                                             accessKey,
                                                             secretKey)));

        if (sqsLocalStackEndpoint != null) {
            sqsClientBuilder.endpointOverride(URI.create(sqsLocalStackEndpoint));
        }

        return sqsClientBuilder.build();
    }

    @Bean
    public DynamoDbClient getDynamoDbClient() {
        DynamoDbClientBuilder dynamoDbClientBuilder = DynamoDbClient.builder()
                                                                    .region(Region.of(defaultRegion))
                                                                    .credentialsProvider(StaticCredentialsProvider.create(
                                                                            AwsBasicCredentials.create(accessKey, secretKey)));

        if (dynamoDbLocalStackEndpoint != null) {
            dynamoDbClientBuilder.endpointOverride(URI.create(dynamoDbLocalStackEndpoint));
        }

        return dynamoDbClientBuilder.build();
    }

    @Bean
    public DynamoDbEnhancedClient getDynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    }
}
