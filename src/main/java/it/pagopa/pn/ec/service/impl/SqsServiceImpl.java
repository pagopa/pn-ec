package it.pagopa.pn.ec.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.exception.SqsException;
import it.pagopa.pn.ec.service.SqsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static it.pagopa.pn.ec.utils.QueueUtils.getQueueUrl;

@Service
@Slf4j
public class SqsServiceImpl implements SqsService {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    public SqsServiceImpl(SqsClient sqsClient, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

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
