package it.pagopa.pn.ec.commons.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory;
import io.awspring.cloud.messaging.listener.support.AcknowledgmentHandlerMethodArgumentResolver;
import it.pagopa.pn.ec.commons.configurationproperties.AwsConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbAsyncWaiter;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;

import java.net.URI;
import java.util.List;

@Configuration
public class AwsConfiguration {

    private final AwsConfigurationProperties awsConfigurationProperties;

    /**
     * Set in LocalStackTestConfig
     */
    @Value("${test.aws.sqs.endpoint:#{null}}")
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

    private static final DefaultCredentialsProvider DEFAULT_CREDENTIALS_PROVIDER = DefaultCredentialsProvider.create();

    public AwsConfiguration(AwsConfigurationProperties awsConfigurationProperties) {
        this.awsConfigurationProperties = awsConfigurationProperties;
    }

//  <-- spring-cloud-starter-aws-messaging -->

    @Bean
    public QueueMessageHandlerFactory queueMessageHandlerFactory(ObjectMapper objectMapper, LocalValidatorFactoryBean validator) {

        final var queueMessageHandlerFactory = new QueueMessageHandlerFactory();
        final var converter = new MappingJackson2MessageConverter();

        converter.setObjectMapper(objectMapper);
        converter.setStrictContentTypeMatch(false);

        final var acknowledgmentResolver = new AcknowledgmentHandlerMethodArgumentResolver("Acknowledgment");

        queueMessageHandlerFactory.setArgumentResolvers(List.of(acknowledgmentResolver,
                                                                new PayloadMethodArgumentResolver(converter, validator)));

        return queueMessageHandlerFactory;
    }

//  <-- AWS SDK for Java v2 -->

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        SqsAsyncClientBuilder sqsAsyncClientBuilder = SqsAsyncClient.builder()
                                                                    .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER)
                                                                    .region(Region.of(awsConfigurationProperties.regionCode()));

        if (sqsLocalStackEndpoint != null) {
            sqsAsyncClientBuilder.endpointOverride(URI.create(sqsLocalStackEndpoint));
        }

        return sqsAsyncClientBuilder.build();
    }

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        DynamoDbAsyncClientBuilder dynamoDbAsyncClientBuilder = DynamoDbAsyncClient.builder()
                                                                                   .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER)
                                                                                   .region(Region.of(awsConfigurationProperties.regionCode()));

        if (dynamoDbLocalStackEndpoint != null) {
            dynamoDbAsyncClientBuilder.endpointOverride(URI.create(dynamoDbLocalStackEndpoint));
        }

        return dynamoDbAsyncClientBuilder.build();
    }

    @Bean
    public DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient(DynamoDbAsyncClient dynamoDbAsyncClient) {
        return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(dynamoDbAsyncClient).build();
    }

    @Bean
    public DynamoDbAsyncWaiter dynamoDbAsyncWaiter(DynamoDbAsyncClient dynamoDbAsyncClient) {
        return DynamoDbAsyncWaiter.builder().client(dynamoDbAsyncClient).build();
    }

    // TODO: In the future, delete these synchronous dynamo clients. Only asynchronous Dynamo clients will be used
    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder dynamoDbClientBuilder = DynamoDbClient.builder()
                                                                    .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER)
                                                                    .region(Region.of(awsConfigurationProperties.regionCode()));

        if (dynamoDbLocalStackEndpoint != null) {
            dynamoDbClientBuilder.endpointOverride(URI.create(dynamoDbLocalStackEndpoint));
        }

        return dynamoDbClientBuilder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    }

    @Bean
    public DynamoDbWaiter dynamoDbWaiter(DynamoDbClient dynamoDbClient) {
        return DynamoDbWaiter.builder().client(dynamoDbClient).build();
    }

    @Bean
    public SnsAsyncClient snsClient() {
        SnsAsyncClientBuilder snsAsyncClientBuilder = SnsAsyncClient.builder()
                                                                    .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER)
                                                                    .region(Region.of(awsConfigurationProperties.regionCode()));

        if (snsLocalStackEndpoint != null) {
            snsAsyncClientBuilder.endpointOverride(URI.create(snsLocalStackEndpoint));
        }

        return snsAsyncClientBuilder.build();
    }
}
