package it.pagopa.pn.ec.commons.configuration.aws;

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
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClientBuilder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.SesAsyncClientBuilder;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;

import java.net.URI;
import java.util.List;

@Configuration
public class AwsConfiguration {

    private final AwsConfigurationProperties awsConfigurationProperties;

    //  These properties are set in LocalStackTestConfig
    @Value("${test.aws.sqs.endpoint:#{null}}")
    String sqsLocalStackEndpoint;
    @Value("${test.aws.dynamodb.endpoint:#{null}}")
    String dynamoDbLocalStackEndpoint;
    @Value("${test.aws.sns.endpoint:#{null}}")
    String snsLocalStackEndpoint;
    @Value("${test.aws.ses.endpoint:#{null}}")
    String sesLocalStackEndpoint;
    @Value("${test.aws.event:#{null}}")
    String eventLocalStackEndpoint;
    @Value("${test.aws.secretsmanager.endpoint:#{null}}")
    String secretsmanagerLocalStackEndpoint;
    @Value("${test.aws.cloudwatch.endpoint:#{null}}")
    String cloudwatchLocalStackEndpoint;



    private static final DefaultCredentialsProvider DEFAULT_CREDENTIALS_PROVIDER_V2 = DefaultCredentialsProvider.create();

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
    public EventBridgeAsyncClient eventBridgeAsyncClient() {
        EventBridgeAsyncClientBuilder eventBrClient = EventBridgeAsyncClient.builder()
                                                                            .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2)
                                                                            .region(Region.of(awsConfigurationProperties.regionCode()));

        if (eventLocalStackEndpoint != null) {
            eventBrClient.endpointOverride(URI.create(eventLocalStackEndpoint));
        }

        return eventBrClient.build();
    }

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        SqsAsyncClientBuilder sqsAsyncClientBuilder = SqsAsyncClient.builder()
                                                                    .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2)
                                                                    .region(Region.of(awsConfigurationProperties.regionCode()));

        if (sqsLocalStackEndpoint != null) {
            sqsAsyncClientBuilder.endpointOverride(URI.create(sqsLocalStackEndpoint));
        }

        return sqsAsyncClientBuilder.build();
    }

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        DynamoDbAsyncClientBuilder dynamoDbAsyncClientBuilder = DynamoDbAsyncClient.builder()
                                                                                   .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2)
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
    public SnsAsyncClient snsClient() {
        SnsAsyncClientBuilder snsAsyncClientBuilder = SnsAsyncClient.builder()
                                                                    .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2)
                                                                    .region(Region.of(awsConfigurationProperties.regionCode()))
                                                                    .overrideConfiguration(c -> c.addMetricPublisher(
                                                                            CloudWatchMetricPublisher.create()));

        if (snsLocalStackEndpoint != null) {
            snsAsyncClientBuilder.endpointOverride(URI.create(snsLocalStackEndpoint));
        }

        return snsAsyncClientBuilder.build();
    }

    @Bean
    public SesAsyncClient sesClient() {
        SesAsyncClientBuilder sesAsyncClientBuilder = SesAsyncClient.builder()
                                                                    .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2)
                                                                    .region(Region.of(awsConfigurationProperties.regionCode()))
                                                                    .overrideConfiguration(c -> c.addMetricPublisher(
                                                                            CloudWatchMetricPublisher.create()));

        if (sesLocalStackEndpoint != null) {
            sesAsyncClientBuilder.endpointOverride(URI.create(sesLocalStackEndpoint));
        }

        return sesAsyncClientBuilder.build();
    }

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        SecretsManagerClientBuilder secretsManagerClient = SecretsManagerClient.builder()
                                                                               .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2)
                                                                               .region(Region.of(awsConfigurationProperties.regionCode()));

        if (sesLocalStackEndpoint != null) {
            secretsManagerClient.endpointOverride(URI.create(secretsmanagerLocalStackEndpoint));
        }

        return secretsManagerClient.build();
    }

    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient() {
        CloudWatchAsyncClientBuilder cloudWatchAsyncClientBuilder = CloudWatchAsyncClient.builder()
                                                                                         .credentialsProvider(
                                                                                                 DEFAULT_CREDENTIALS_PROVIDER_V2)
                                                                                         .region(Region.of(awsConfigurationProperties.regionCode()));

        if (cloudwatchLocalStackEndpoint != null) {
            cloudWatchAsyncClientBuilder.endpointOverride(URI.create(cloudwatchLocalStackEndpoint));
        }

        return cloudWatchAsyncClientBuilder.build();
    }
}
