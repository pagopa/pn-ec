package it.pagopa.pn.ec.commons.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.pojo.sqs.SqsMessageWrapper;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.function.Consumer;

@Service
@Slf4j
public class SqsServiceImpl implements SqsService {

    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;
    private final JsonUtils jsonUtils;

    public SqsServiceImpl(SqsAsyncClient sqsAsyncClient, ObjectMapper objectMapper, JsonUtils jsonUtils) {
        this.sqsAsyncClient = sqsAsyncClient;
        this.objectMapper = objectMapper;
        this.jsonUtils = jsonUtils;
    }

    @Override
    public <T> Mono<SendMessageResponse> send(final String queueName, final T queuePayload) throws SqsPublishException {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(queuePayload))
                   .doOnNext(sendMessageResponse -> log.info("Try to publish on {} with payload {}", queueName, sendMessageResponse))
                   .zipWith(getQueueUrlFromName(queueName))
                   .flatMap(objects -> Mono.fromCompletionStage(sqsAsyncClient.sendMessage(builder -> builder.queueUrl(objects.getT2())
                                                                                                             .messageBody(objects.getT1()))))
                   .onErrorResume(throwable -> {
                       log.error(throwable.getMessage(), throwable);
                       return Mono.error(new SqsPublishException(queueName));
                   });
    }

    @Override
    public <T> Flux<SqsMessageWrapper<T>> getAllQueueMessage(String queueName, Class<T> messageContentClass) {
        return getQueueUrlFromName(queueName).flux()
                                             .flatMap(queueUrl -> getAllQueueMessage(messageContentClass,
                                                                                     builder -> builder.queueUrl(queueUrl)));
    }

    @Override
    public <T> Flux<SqsMessageWrapper<T>> getAllQueueMessage(String queueName, Class<T> messageContentClass, int maxNumberOfMessages) {
        return getQueueUrlFromName(queueName).flux()
                                             .flatMap(queueUrl -> getAllQueueMessage(messageContentClass,
                                                                                     builder -> builder.queueUrl(queueUrl)
                                                                                                       .maxNumberOfMessages(
                                                                                                               maxNumberOfMessages)));
    }

    private <T> Flux<SqsMessageWrapper<T>> getAllQueueMessage(Class<T> messageContentClass,
                                                              Consumer<ReceiveMessageRequest.Builder> receiveMessageRequest) {
        return Mono.fromCompletionStage(sqsAsyncClient.receiveMessage(receiveMessageRequest))
                   .map(ReceiveMessageResponse::messages)
                   .flatMapIterable(messages -> messages)
                   .map(message -> new SqsMessageWrapper<>(message,
                                                           jsonUtils.convertJsonStringToObject(message.body(), messageContentClass)));
    }

    @Override
    public Mono<DeleteMessageResponse> deleteMessageFromQueue(final Message message, final String queueName) {
        return getQueueUrlFromName(queueName).doOnNext(queueUrl -> log.info("Delete message with id {} from {} queue",
                                                                            message.messageId(),
                                                                            queueName))
                                             .flatMap(queueUrl -> Mono.fromCompletionStage(sqsAsyncClient.deleteMessage(builder -> builder.queueUrl(
                                                     queueUrl).receiptHandle(message.receiptHandle()))));
    }

    @Override
    public Mono<String> getQueueUrlFromName(final String queueName) {
        return Mono.fromCompletionStage(sqsAsyncClient.getQueueUrl(builder -> builder.queueName(queueName)))
                   .map(GetQueueUrlResponse::queueUrl);
    }
}
