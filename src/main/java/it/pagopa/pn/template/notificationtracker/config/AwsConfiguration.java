package it.pagopa.pn.template.notificationtracker.config;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;

import java.net.URI;
import java.util.Collections;

@Configuration
public class AwsConfiguration {

    /**
     * Set in LocalStackTestConfig
     */
    @Value("${test.sqs.dynamodb.endpoint:#{null}}")
    String sqsLocalStackEndpoint;

    /**
     * Set in LocalStackTestConfig
     */
    @Value("${test.aws.dynamodb.endpoint:#{null}}")
    String dynamoDbLocalStackEndpoint;

    /**
     * Set in LocalStackTestConfig
     */
    @Value("${test.aws.sns.endpoint:#{null}}")
    String snsLocalStackEndpoint;

    private static final DefaultAwsRegionProviderChain DEFAULT_AWS_REGION_PROVIDER_CHAIN = new DefaultAwsRegionProviderChain();
    private static final DefaultCredentialsProvider DEFAULT_CREDENTIALS_PROVIDER = DefaultCredentialsProvider.create();

//  <-- spring-cloud-starter-aws-messaging -->

    @Bean
    public QueueMessagingTemplate queueMessagingTemplate(final AmazonSQSAsync amazonSQSAsync) {
        return new QueueMessagingTemplate(amazonSQSAsync);
    }

    @Bean
    public QueueMessageHandlerFactory queueMessageHandlerFactory(final ObjectMapper objectMapper, final AmazonSQSAsync amazonSQSAsync) {

        final var queueHandlerFactory = new QueueMessageHandlerFactory();
        final var converter = new MappingJackson2MessageConverter();

        queueHandlerFactory.setAmazonSqs(amazonSQSAsync);
        converter.setObjectMapper(objectMapper);
        queueHandlerFactory.setArgumentResolvers(Collections.singletonList(new PayloadMethodArgumentResolver(converter)));

        return queueHandlerFactory;
    }

//  <-- AWS SDK for Java v2 -->

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        SqsAsyncClientBuilder sqsAsyncClientBuilder = SqsAsyncClient.builder()
                                                                    .region(DEFAULT_AWS_REGION_PROVIDER_CHAIN.getRegion())
                                                                    .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER);

        if (sqsLocalStackEndpoint != null) {
            sqsAsyncClientBuilder.endpointOverride(URI.create(dynamoDbLocalStackEndpoint));
        }

        return sqsAsyncClientBuilder.build();
    }

    // TODO: Change to DynamoDbAsyncClient for reactive
    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder dynamoDbClientBuilder = DynamoDbClient.builder()
                                                                    .region(DEFAULT_AWS_REGION_PROVIDER_CHAIN.getRegion())
                                                                    .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER);

        if (dynamoDbLocalStackEndpoint != null) {
            dynamoDbClientBuilder.endpointOverride(URI.create(dynamoDbLocalStackEndpoint));
        }

        return dynamoDbClientBuilder.build();
    }

    @Bean
    public DynamoDbEnhancedClient getDynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    }

    @Bean
    public DynamoDbWaiter getDynamoDbWaiter(DynamoDbClient dynamoDbClient) {
        return DynamoDbWaiter.builder().client(dynamoDbClient).build();
    }

    @Bean
    public SnsAsyncClient snsClient() {
        SnsAsyncClientBuilder snsAsyncClientBuilder = SnsAsyncClient.builder()
                                                                    .region(DEFAULT_AWS_REGION_PROVIDER_CHAIN.getRegion())
                                                                    .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER);

        if (snsLocalStackEndpoint != null) {
            snsAsyncClientBuilder.endpointOverride(URI.create(snsLocalStackEndpoint));
        }

        return snsAsyncClientBuilder.build();
    }
}
