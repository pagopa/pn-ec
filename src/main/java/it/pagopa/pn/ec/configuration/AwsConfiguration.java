package it.pagopa.pn.ec.configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;
import java.util.Collections;

@Configuration
public class AwsConfiguration {

    @Value("${cloud.aws.credentials.access-key}")
    String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    String secretKey;

    @Value("${cloud.aws.region.static}")
    String defaultRegion;

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

//  <-- spring-cloud-starter-aws-messaging -->

    @Bean
    public QueueMessagingTemplate queueMessagingTemplate(final AmazonSQSAsync amazonSQSAsync) {
        return new QueueMessagingTemplate(amazonSQSAsync);
    }

    @Bean
    public QueueMessageHandlerFactory queueMessageHandlerFactory(final ObjectMapper objectMapper, final AmazonSQSAsync amazonSQSAsync) {

        final QueueMessageHandlerFactory queueHandlerFactory = new QueueMessageHandlerFactory();
        final MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();

        queueHandlerFactory.setAmazonSqs(amazonSQSAsync);
        converter.setObjectMapper(objectMapper);
        queueHandlerFactory.setArgumentResolvers(Collections.singletonList(new PayloadMethodArgumentResolver(converter)));

        return queueHandlerFactory;
    }

    @Bean
    @Primary
    public AmazonSQSAsync amazonSQSAsync() {
        AmazonSQSAsyncClientBuilder amazonSQSAsyncClientBuilder = AmazonSQSAsyncClientBuilder.standard()
                                                                                             .withCredentials(new AWSStaticCredentialsProvider(
                                                                                                     new BasicAWSCredentials(accessKey,
                                                                                                                             secretKey)));

        if (sqsLocalStackEndpoint != null) {
            amazonSQSAsyncClientBuilder.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(sqsLocalStackEndpoint,
                                                                                                            defaultRegion));
        } else {
            amazonSQSAsyncClientBuilder.setRegion(defaultRegion);
        }

        return amazonSQSAsyncClientBuilder.build();
    }

//  <-- AWS SDK for Java v2 -->

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
    public SqsClient getSqsClient() {
        SqsClientBuilder sqsClientBuilder = SqsClient.builder()
                                                     .region(Region.of(defaultRegion))
                                                     .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                                                             accessKey,
                                                             secretKey)));

        if (snsLocalStackEndpoint != null) {
            sqsClientBuilder.endpointOverride(URI.create(snsLocalStackEndpoint));
        }

        return sqsClientBuilder.build();
    }
}
