package it.pagopa.pn.ec.testutils.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import java.net.URI;

@TestConfiguration
public class DynamoTestConfiguration {


    /**
     * Set in LocalStackTestConfig
     */
    @Value("${test.aws.dynamodb.endpoint}")
    String dynamoDbLocalStackEndpoint;

    @Value("${test.aws.region-code:#{null}}")
    String regionCode;
//  <-- AWS SDK for Java v2 -->


    @Bean
    public DynamoDbClient dynamoDbTestClient() {
        return DynamoDbClient.builder()
                             .credentialsProvider(DefaultCredentialsProvider.create())
                             .region(Region.of(regionCode))
                             .endpointOverride(URI.create(dynamoDbLocalStackEndpoint))
                             .build();
    }


    @Bean
    public DynamoDbEnhancedClient dynamoDbTestEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    }

    @Bean
    public DynamoDbWaiter dynamoDbWaiter(DynamoDbClient dynamoDbClient) {
        return DynamoDbWaiter.builder().client(dynamoDbClient).build();
    }
}
