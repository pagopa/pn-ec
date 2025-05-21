package it.pagopa.pn.ec.sqs;

import it.pagopa.pn.ec.commons.configuration.sqs.SqsTimeoutConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class SqsTimeoutProviderConfiguration {
    @Bean
    public SqsTimeoutProvider sqsTimeoutProvider(SqsAsyncClient sqsAsyncClient, SqsTimeoutConfigurationProperties config){
        return new SqsTimeoutProvider(sqsAsyncClient,config);
    }
}
