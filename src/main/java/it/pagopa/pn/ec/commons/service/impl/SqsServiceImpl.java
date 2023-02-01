package it.pagopa.pn.ec.commons.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.exception.sqs.SqsConvertToJsonPayloadException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.service.SqsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class SqsServiceImpl implements SqsService {

    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;

    public SqsServiceImpl(SqsAsyncClient sqsAsyncClient, ObjectMapper objectMapper) {
        this.sqsAsyncClient = sqsAsyncClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> Mono<SendMessageResponse> send(String queueName, T queuePayload) {
        AtomicReference<String> jsonPayload = new AtomicReference<>("");
        return Mono.fromCompletionStage(sqsAsyncClient.sendMessage(builder -> {
            String getQueueUrl = GetQueueUrlRequest.builder().queueName(queueName).build().queueName();
            try {
                jsonPayload.set(objectMapper.writeValueAsString(queuePayload));
                builder.queueUrl(getQueueUrl).messageBody(jsonPayload.get());
            } catch (JsonProcessingException e) {
                throw new SqsConvertToJsonPayloadException(queuePayload);
            }
        })).onErrorResume(throwable -> {
            log.error(throwable.getMessage(), throwable);
            return Mono.error(new SqsPublishException(queueName));
        }).doOnSuccess(sendMessageResponse -> log.info("Publishing on {} with payload {}", queueName, jsonPayload.get()));
    }

    @Override
    public <T> Mono<T> incomingMessageFlow(T queuePayload) {
        return Mono.just(queuePayload)
                   .doOnNext(message -> log.info("Incoming message {}", message))
                   .timeout(Duration.ofSeconds(MAXIMUM_LISTENING_TIME))
                   .doOnError(throwable -> log.error("Maximum listening time on incoming message queue exceed"));
    }
}
