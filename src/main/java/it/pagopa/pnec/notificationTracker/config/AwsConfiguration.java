package it.pagopa.pnec.notificationTracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
public class AwsConfiguration {
	
	
	   @Value("${AWS_PROFILE}")
	    private static String awsProfile;

	    @Bean
	    public SqsClient getSqsClient() {
	        return SqsClient.builder()
	                 .region(Region.of(awsProfile))
	                 .credentialsProvider(ProfileCredentialsProvider.create())
	                 .build();
	    }

}
