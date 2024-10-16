package it.pagopa.pn.ec.commons.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.SqsRetryStrategyProperties;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.model.pojo.s3.S3Pointer;
import it.pagopa.pn.ec.commons.model.pojo.sqs.SqsMessageWrapper;
import it.pagopa.pn.ec.commons.service.S3Service;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.utils.JsonUtils;
import lombok.CustomLog;
import lombok.SneakyThrows;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.OptionalUtils.getFirstListElement;

@Service
@CustomLog
public class SqsServiceImpl implements SqsService {

    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;
    private final JsonUtils jsonUtils;
    private final S3Service s3Service;
    private final RetryBackoffSpec sqsRetryStrategy;
    private static final int MESSAGE_GROUP_ID_LENGTH = 64;
    private static final int MESSAGE_DEDUPLICATION_ID_LENGTH = 64;
    @Value("${sqs.queue.max-message-size}")
    private Integer sqsQueueMaxMessageSize;
    @Value("${sqs.queue.max-batch-subscribed-msgs}")
    private Integer maxMessages;

    public SqsServiceImpl(SqsAsyncClient sqsAsyncClient, ObjectMapper objectMapper, JsonUtils jsonUtils, S3Service s3Service, SqsRetryStrategyProperties sqsRetryStrategyProperties) {
        this.sqsAsyncClient = sqsAsyncClient;
        this.objectMapper = objectMapper;
        this.jsonUtils = jsonUtils;
        this.s3Service = s3Service;
        this.sqsRetryStrategy = Retry.backoff(sqsRetryStrategyProperties.maxAttempts(), Duration.ofSeconds(sqsRetryStrategyProperties.minBackoff()))
                .filter(SqsException.class::isInstance)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure());
    }

    @Override
    public <T> Mono<SendMessageResponse> send(final String queueName, final T queuePayload) throws SqsClientException {
        return send(queueName, (Integer) null, queuePayload);
    }

    @Override
    public <T> Mono<SendMessageResponse> sendWithDeduplicationId(String queueName, T queuePayload) throws SqsClientException {
        return send(queueName, RandomStringUtils.randomAlphanumeric(MESSAGE_GROUP_ID_LENGTH), RandomStringUtils.randomAlphanumeric(MESSAGE_DEDUPLICATION_ID_LENGTH), null, queuePayload);
    }

    @Override
    public <T> Mono<SendMessageResponse> send(String queueName, Integer delaySeconds, T queuePayload) throws SqsClientException {
        return send(queueName, RandomStringUtils.randomAlphanumeric(MESSAGE_GROUP_ID_LENGTH), null, delaySeconds, queuePayload);
    }

    @Override
    public <T> Mono<SendMessageResponse> send(String queueName, String messageGroupId, T queuePayload) throws SqsClientException {
        return send(queueName, messageGroupId, null, null, queuePayload);
    }

    @Override
    public <T> Mono<SendMessageResponse> send(String queueName, String messageGroupId, String messageDeduplicationId, Integer delaySeconds, T queuePayload) throws SqsClientException {
        log.debug(INSERTING_DATA_IN_SQS, queuePayload, queueName);
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(queuePayload))
                .doOnSuccess(sendMessageResponse -> log.info("Try to publish on {} with payload {}", queueName, queuePayload))
                .zipWith(getQueueUrlFromName(queueName))
                .flatMap(objects -> Mono.fromCompletionStage(sqsAsyncClient.sendMessage(builder -> builder.queueUrl(objects.getT2())
                        .messageBody(objects.getT1())
                        .messageGroupId(messageGroupId)
                        .messageDeduplicationId(messageDeduplicationId)
                        .delaySeconds(delaySeconds))))
                .retryWhen(getSqsRetryStrategy())
                .onErrorResume(throwable -> {
                    log.error("Error on sqs publish : {}", throwable.getMessage(), throwable);
                    return Mono.error(new SqsClientException(queueName));
                })
                .doOnSuccess(result -> log.info(INSERTED_DATA_IN_SQS, queueName));
    }

    @Override
    public <T> Mono<SendMessageResponse> sendWithLargePayload(String queueName, String messageGroupId, String bucketName, T queuePayload) throws SqsClientException {
        log.debug(INSERTING_DATA_IN_SQS, queuePayload, queueName);
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(queuePayload))
                .doOnSuccess(sendMessageResponse -> log.info("Try to publish on {} with payload {}", queueName, queuePayload))
                .zipWith(getQueueUrlFromName(queueName))
                .map(objects -> SendMessageRequest.builder().queueUrl(objects.getT2())
                        .messageBody(objects.getT1())
                        .messageGroupId(messageGroupId))
                .flatMap(sendMessageRequestBuilder -> {
                    if (isLarge(sendMessageRequestBuilder.build())) {
                        return s3Service.convertAndPutObject(bucketName, queuePayload)
                                .map(fileKey -> sendMessageRequestBuilder.messageBody(writeValueAsString(new S3Pointer(fileKey))));
                    } else return Mono.just(sendMessageRequestBuilder);
                })
                .flatMap(sendMessageRequest -> Mono.fromCompletionStage(sqsAsyncClient.sendMessage(sendMessageRequest.build())))
                .retryWhen(getSqsRetryStrategy())
                .onErrorResume(throwable -> {
                    log.error("Error on sqs publish : {}", throwable.getMessage(), throwable);
                    return Mono.error(new SqsClientException(queueName));
                });
    }


    @Override
    public <T> Mono<SqsMessageWrapper<T>> getOneMessage(String queueName, Class<T> messageContentClass) {
        return getQueueUrlFromName(queueName).flatMap(queueUrl -> Mono.fromCompletionStage(sqsAsyncClient.receiveMessage(builder -> builder.queueUrl(
                        queueUrl))))
                .retryWhen(getSqsRetryStrategy())
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
    public Mono<ChangeMessageVisibilityResponse> changeMessageVisibility(String queueName, Integer visibilityTimeout, String receiptHandle) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, CHANGE_MESSAGE_VISIBILITY, Stream.of(queueName, visibilityTimeout, receiptHandle).toList());
        return getQueueUrlFromName(queueName).flatMap(queueUrl -> Mono.fromCompletionStage(sqsAsyncClient.changeMessageVisibility(builder -> builder.queueUrl(
                        queueUrl).visibilityTimeout(visibilityTimeout).receiptHandle(receiptHandle))))
                .onErrorResume(throwable -> {
                    log.error(throwable.getMessage(), throwable);
                    return Mono.error(new SqsClientException(queueName));
                });
    }

    @Override
    public <T> Flux<SqsMessageWrapper<T>> getMessages(String queueName, Class<T> messageContentClass) {
        return getMessages(queueName, messageContentClass, maxMessages);
    }

    @Override
    public <T> Flux<SqsMessageWrapper<T>> getMessages(String queueName, Class<T> messageContentClass, Integer maxMessages) {

        AtomicInteger actualMessages = new AtomicInteger();
        AtomicBoolean listIsEmpty = new AtomicBoolean();
        listIsEmpty.set(false);

        BooleanSupplier condition = () -> (actualMessages.get() <= maxMessages && !listIsEmpty.get());

        return getQueueUrlFromName(queueName).flatMap(queueUrl -> Mono.fromCompletionStage(sqsAsyncClient.receiveMessage(builder -> builder.queueUrl(
                        queueUrl))))
                .retryWhen(getSqsRetryStrategy())
                .flatMap(receiveMessageResponse ->
                        {
                            var messages = receiveMessageResponse.messages();
                            if (messages.isEmpty())
                                listIsEmpty.set(true);
                            return Mono.justOrEmpty(messages);
                        }
                )
                .flatMapMany(Flux::fromIterable)
                .map(message ->
                {
                    actualMessages.incrementAndGet();
                    return new SqsMessageWrapper<>(message,
                            jsonUtils.convertJsonStringToObject(message.body(),
                                    messageContentClass));
                })
                .onErrorResume(throwable -> {
                    log.error(throwable.getMessage(), throwable);
                    return Mono.error(new SqsClientException(queueName));
                })
                .repeat(condition);
    }

    @Override
    public Mono<DeleteMessageResponse> deleteMessageFromQueue(final Message message, final String queueName) {
        return getQueueUrlFromName(queueName).doOnSuccess(queueUrl -> log.debug("Delete message with id {} from {} queue",
                        message.messageId(),
                        queueName))
                .flatMap(queueUrl -> Mono.fromCompletionStage(sqsAsyncClient.deleteMessage(builder -> builder.queueUrl(
                        queueUrl).receiptHandle(message.receiptHandle()))))
                .retryWhen(getSqsRetryStrategy())
                .onErrorResume(throwable -> {
                    log.error(throwable.getMessage(), throwable);
                    return Mono.error(new SqsClientException(queueName));
                });
    }

    @Override
    public Mono<String> getQueueUrlFromName(final String queueName) {
        return Mono.fromCompletionStage(sqsAsyncClient.getQueueUrl(builder -> builder.queueName(queueName)))
                .retryWhen(getSqsRetryStrategy())
                .map(GetQueueUrlResponse::queueUrl);
    }

    private boolean isLarge(SendMessageRequest sendMessageRequest) {
        int msgAttributesSize = getMsgAttributesSize(sendMessageRequest.messageAttributes());
        long msgBodySize = getStringSizeInBytes(sendMessageRequest.messageBody());
        long totalMsgSize = msgAttributesSize + msgBodySize;
        return (totalMsgSize > sqsQueueMaxMessageSize);
    }

    private int getMsgAttributesSize(Map<String, MessageAttributeValue> msgAttributes) {
        int totalMsgAttributesSize = 0;
        for (Map.Entry<String, MessageAttributeValue> entry : msgAttributes.entrySet()) {
            totalMsgAttributesSize += getStringSizeInBytes(entry.getKey());

            MessageAttributeValue entryVal = entry.getValue();
            if (entryVal.dataType() != null) {
                totalMsgAttributesSize += getStringSizeInBytes(entryVal.dataType());
            }

            String stringVal = entryVal.stringValue();
            if (stringVal != null) {
                totalMsgAttributesSize += getStringSizeInBytes(entryVal.stringValue());
            }

            SdkBytes binaryVal = entryVal.binaryValue();
            if (binaryVal != null) {
                totalMsgAttributesSize += binaryVal.asByteArray().length;
            }
        }
        return totalMsgAttributesSize;
    }

    @SneakyThrows(IOException.class)
    public static long getStringSizeInBytes(String str) {
        CountingOutputStream counterOutputStream = new CountingOutputStream(new ByteArrayOutputStream());
        Writer writer = new OutputStreamWriter(counterOutputStream, StandardCharsets.UTF_8);
        writer.write(str);
        writer.flush();
        writer.close();
        return counterOutputStream.getCount();
    }

    @SneakyThrows
    private <T> String writeValueAsString(T object) {
        return objectMapper.writeValueAsString(object);
    }

    private RetryBackoffSpec getSqsRetryStrategy() {
        var mdcContextMap = MDCUtils.retrieveMDCContextMap();
        return sqsRetryStrategy.doBeforeRetry(retrySignal -> {
            MDCUtils.enrichWithMDC(null, mdcContextMap);
            log.debug(SHORT_RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage());
        });
    }

}
