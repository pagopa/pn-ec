package it.pagopa.pn.ec.commons.configuration.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory;
import io.awspring.cloud.messaging.listener.support.AcknowledgmentHandlerMethodArgumentResolver;
import lombok.extern.slf4j.Slf4j;
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
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.SesAsyncClientBuilder;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

import java.net.URI;
import java.util.List;

@Configuration
@Slf4j
public class AwsConfiguration {

    @Value("${test.aws.region-code:#{null}}")
    String regionCode;
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
    @Value("${test.aws.ssm.endpoint:#{null}}")
    String ssmLocalStackEndpoint;
    @Value("${test.aws.s3.endpoint:#{null}}")
    String s3LocalStackEndpoint;




    private static final DefaultCredentialsProvider DEFAULT_CREDENTIALS_PROVIDER_V2 = DefaultCredentialsProvider.create();



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
                                                                            .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2);

        if (eventLocalStackEndpoint != null) {
            eventBrClient.endpointOverride(URI.create(eventLocalStackEndpoint));
        }
        if(regionCode != null) {
            eventBrClient.region(Region.of(regionCode));
        }

        return eventBrClient.build();
    }

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        SqsAsyncClientBuilder sqsAsyncClientBuilder = SqsAsyncClient.builder()
                                                                    .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2);
        if (sqsLocalStackEndpoint != null) {
            sqsAsyncClientBuilder.endpointOverride(URI.create(sqsLocalStackEndpoint));
        }

        if (regionCode != null) {
            sqsAsyncClientBuilder.region(Region.of(regionCode));

        }

        return sqsAsyncClientBuilder.build();
    }

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        DynamoDbAsyncClientBuilder dynamoDbAsyncClientBuilder = DynamoDbAsyncClient.builder()
                                                                                   .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2);
        if (dynamoDbLocalStackEndpoint != null) {
            dynamoDbAsyncClientBuilder.endpointOverride(URI.create(dynamoDbLocalStackEndpoint));
        }
        if(regionCode != null) {
            dynamoDbAsyncClientBuilder.region(Region.of(regionCode));
        }

        return dynamoDbAsyncClientBuilder.build();
    }

    @Bean
    public DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient(DynamoDbAsyncClient dynamoDbAsyncClient) {
        return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(dynamoDbAsyncClient).build();
    }

    @Bean
    public SnsAsyncClient snsClient(CloudWatchAsyncClient cloudWatchAsyncClient) {
        SnsAsyncClientBuilder snsAsyncClientBuilder = SnsAsyncClient.builder()
                                                                    .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2)
                                                                    .overrideConfiguration(c -> c.addMetricPublisher(CloudWatchMetricPublisher.builder()
                                                                            .cloudWatchClient(cloudWatchAsyncClient).build()));

        if (snsLocalStackEndpoint != null) {
            snsAsyncClientBuilder.endpointOverride(URI.create(snsLocalStackEndpoint));
        }

        if(regionCode != null) {
            snsAsyncClientBuilder.region(Region.of(regionCode));
        }

        return snsAsyncClientBuilder.build();
    }

    @Bean
    public SesAsyncClient sesClient(CloudWatchAsyncClient cloudWatchAsyncClient) {
        SesAsyncClientBuilder sesAsyncClientBuilder = SesAsyncClient.builder()
                                                                    .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2)
                                                                    .overrideConfiguration(c -> c.addMetricPublisher(CloudWatchMetricPublisher.builder()
                                                                            .cloudWatchClient(cloudWatchAsyncClient).build()));

        if (sesLocalStackEndpoint != null) {
            sesAsyncClientBuilder.endpointOverride(URI.create(sesLocalStackEndpoint));
        }

        if(regionCode != null) {
            sesAsyncClientBuilder.region(Region.of(regionCode));
        }


        return sesAsyncClientBuilder.build();
    }

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        SecretsManagerClientBuilder secretsManagerClient = SecretsManagerClient.builder()
                                                                               .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2);

        if (sesLocalStackEndpoint != null) {
            secretsManagerClient.endpointOverride(URI.create(secretsmanagerLocalStackEndpoint));
        }
        if(regionCode != null) {
            secretsManagerClient.region(Region.of(regionCode));
        }

        return secretsManagerClient.build();
    }

    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient() {
        CloudWatchAsyncClientBuilder cloudWatchAsyncClientBuilder = CloudWatchAsyncClient.builder()
                                                                                         .credentialsProvider(
                                                                                                 DEFAULT_CREDENTIALS_PROVIDER_V2);
        if (cloudwatchLocalStackEndpoint != null) {
            cloudWatchAsyncClientBuilder.endpointOverride(URI.create(cloudwatchLocalStackEndpoint));
        }

        if(regionCode != null) {
            cloudWatchAsyncClientBuilder.region(Region.of(regionCode));
        }

        return cloudWatchAsyncClientBuilder.build();
    }

    @Bean
    public SsmClient ssmClient() {
        SsmClientBuilder ssmClientBuilder = SsmClient.builder()
                .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2);

        if(ssmLocalStackEndpoint != null) {
            ssmClientBuilder.endpointOverride(URI.create(ssmLocalStackEndpoint));
        }
        if(regionCode != null) {
            ssmClientBuilder.region(Region.of(regionCode));
        }

        return ssmClientBuilder.build();
    }

    @Bean
    public S3AsyncClient s3AsyncClient() {
        S3AsyncClientBuilder s3Client = S3AsyncClient.builder()
                .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2);
        if (s3LocalStackEndpoint != null) {
            s3Client.endpointOverride(URI.create(s3LocalStackEndpoint));
        }

        if(regionCode != null) {
            s3Client.region(Region.of(regionCode));
        }


        return s3Client.build();
    }

}
