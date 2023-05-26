package it.pagopa.pn.ec.commons.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.model.pojo.sqs.SqsMessageWrapper;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import static it.pagopa.pn.ec.commons.utils.OptionalUtils.getFirstListElement;

@Service
@Slf4j
public class SqsServiceImpl implements SqsService {

    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;
    private final JsonUtils jsonUtils;
    private static final int MESSAGE_GROUP_ID_LENGTH= 64;

    public SqsServiceImpl(SqsAsyncClient sqsAsyncClient, ObjectMapper objectMapper, JsonUtils jsonUtils) {
        this.sqsAsyncClient = sqsAsyncClient;
        this.objectMapper = objectMapper;
        this.jsonUtils = jsonUtils;
    }

    @Override
    public <T> Mono<SendMessageResponse> send(final String queueName, final T queuePayload) throws SqsClientException {
        log.info("<-- START SENDING MESSAGE ON QUEUE  --> Queue name : {}", queueName);
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(queuePayload))
                .doOnNext(sendMessageResponse -> log.debug("Try to publish on {} with payload {}", queueName, sendMessageResponse))
                .zipWith(getQueueUrlFromName(queueName))
                .flatMap(objects -> Mono.fromCompletionStage(sqsAsyncClient.sendMessage(builder -> builder.queueUrl(objects.getT2())
                        .messageBody(objects.getT1())
                        .messageGroupId(RandomStringUtils.randomAlphanumeric(MESSAGE_GROUP_ID_LENGTH)))))
                .onErrorResume(throwable -> {
                    log.error(throwable.getMessage(), throwable);
                    return Mono.error(new SqsClientException(queueName));
                });
    }

    @Override
    public <T> Mono<SqsMessageWrapper<T>> getOneMessage(String queueName, Class<T> messageContentClass) {
        return getQueueUrlFromName(queueName).flatMap(queueUrl -> Mono.fromCompletionStage(sqsAsyncClient.receiveMessage(builder -> builder.queueUrl(
                        queueUrl))))
                .flatMap(receiveMessageResponse -> Mono.justOrEmpty(getFirstListElement(receiveMessageResponse.messages())))
                .map(message -> new SqsMessageWrapper<>(message,
                        jsonUtils.convertJsonStringToObject(message.body(),
                                messageContentClass)))
                .onErrorResume(throwable -> {
                    log.error(throwable.getMessage(), throwable);
                    return Mono.error(new SqsClientException(queueName));
                });
    }

    @Override
    public <T> Flux<SqsMessageWrapper<T>> getMessages(String queueName, Class<T> messageContentClass) {
        return getQueueUrlFromName(queueName).flatMap(queueUrl -> Mono.fromCompletionStage(sqsAsyncClient.receiveMessage(builder -> builder.queueUrl(
                        queueUrl))))
                .flatMap(receiveMessageResponse -> Mono.justOrEmpty(receiveMessageResponse.messages()))
                .flatMapMany(Flux::fromIterable)
                .map(message -> new SqsMessageWrapper<>(message,
                        jsonUtils.convertJsonStringToObject(message.body(),
                                messageContentClass)))
                .onErrorResume(throwable -> {
                    log.error(throwable.getMessage(), throwable);
                    return Mono.error(new SqsClientException(queueName));
                });
    }

    @Override
    public Mono<DeleteMessageResponse> deleteMessageFromQueue(final Message message, final String queueName) {
        return getQueueUrlFromName(queueName).doOnNext(queueUrl -> log.debug("Delete message with id {} from {} queue",
                        message.messageId(),
                        queueName))
                .flatMap(queueUrl -> Mono.fromCompletionStage(sqsAsyncClient.deleteMessage(builder -> builder.queueUrl(
                        queueUrl).receiptHandle(message.receiptHandle()))))
                .onErrorResume(throwable -> {
                    log.error(throwable.getMessage(), throwable);
                    return Mono.error(new SqsClientException(queueName));
                });
    }

    @Override
    public Mono<String> getQueueUrlFromName(final String queueName) {
        return Mono.fromCompletionStage(sqsAsyncClient.getQueueUrl(builder -> builder.queueName(queueName)))
                .map(GetQueueUrlResponse::queueUrl);
    }
}
