package it.pagopa.pnec.notificationtracker.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnec.exception.SqsException;
import it.pagopa.pnec.notificationtracker.service.SqsService;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;


import static it.pagopa.pnec.notificationtracker.utils.QueueUtils.getQueueUrl;

@Service
@Slf4j
public class SqsServiceImpl implements SqsService {


	private final SqsClient sqsClient;
	private final ObjectMapper objectMapper;

	public SqsServiceImpl(SqsClient sqsClient, ObjectMapper objectMapper) {
		this.sqsClient = sqsClient;
		this.objectMapper = objectMapper;
	}

	    
//	    public static void sendMessage(SqsClient sqsClient, String queueName, String message) {
//
//	        try {
//	            CreateQueueRequest request = CreateQueueRequest.builder()
//	                .queueName(queueName)
//	                .build();
//	            sqsClient.createQueue(request);
//
//	            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
//	                .queueName(queueName)
//	                .build();
//
//	            String queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();
//	            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
//	                .queueUrl(queueUrl)
//	                .messageBody(message)
//	                .delaySeconds(5)
//	                .build();
//
//	            sqsClient.sendMessage(sendMsgRequest);
//
//	        } catch (SqsException e) {
//	            System.err.println(e.awsErrorDetails().errorMessage());
//	            System.exit(1);
//	        }
//	    }


	@Override
	public <T> void send(String queueName, T queuePayload) {
		try {
			String jsonPayload = objectMapper.writeValueAsString(queuePayload);
			log.info("Send to {} queue with payload -> {}", queueName, jsonPayload);

			sqsClient.sendMessage(SendMessageRequest.builder()
					.queueUrl(getQueueUrl(sqsClient, queueName))
					.messageBody(jsonPayload)
					.build());
		} catch (JsonProcessingException e) {
			log.error(e.getMessage(), e);
			throw new SqsException.SqsObjectToJsonException(queueName);
		}
	}
}
