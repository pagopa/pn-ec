package it.pagopa.pn.ec.testutils.localstack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;

import java.net.URI;

@TestConfiguration
public class LocalStackClientConfig {

    @Value("${test.aws.region-code:#{null}}")
    String regionCode;

    @Value("${test.aws.s3.endpoint}")
    String s3LocalStackEndpoint;

    @Bean
    public S3Client s3TestClient() {
        return S3Client.builder()
                       .credentialsProvider(DefaultCredentialsProvider.create())
                       .region(Region.of(regionCode))
                       .endpointOverride(URI.create(s3LocalStackEndpoint))
                       .build();
    }

}
