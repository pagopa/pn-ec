package it.pagopa.pnec.notificationTracker.service.impl;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

@Service
@Slf4j
public class SqsServiceImpl  {

	
	  private final SqsClient sqsClient;
	  
	    public SqsServiceImpl(SqsClient sqsClient) {
	        this.sqsClient = sqsClient;
	    }
	

	    
	    public static void sendMessage(SqsClient sqsClient, String queueName, String message) {

	        try {
	            CreateQueueRequest request = CreateQueueRequest.builder()
	                .queueName(queueName)
	                .build();
	            sqsClient.createQueue(request);

	            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
	                .queueName(queueName)
	                .build();

	            String queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();
	            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
	                .queueUrl(queueUrl)
	                .messageBody(message)
	                .delaySeconds(5)
	                .build();

	            sqsClient.sendMessage(sendMsgRequest);

	        } catch (SqsException e) {
	            System.err.println(e.awsErrorDetails().errorMessage());
	            System.exit(1);
	        }
	    }
	    

}
