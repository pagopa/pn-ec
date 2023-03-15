package it.pagopa.pn.ec.commons.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.service.SqsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

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
    public Flux<Message> getAllQueueMessage(final String queueName, final int maxNumberOfMessages) {
        return getQueueUrlFromName(queueName).flatMap(queueUrl -> Mono.fromCompletionStage(sqsAsyncClient.receiveMessage(builder -> builder.queueUrl(
                                                     queueUrl).maxNumberOfMessages(maxNumberOfMessages))))
                                             .map(ReceiveMessageResponse::messages)
                                             .flatMapIterable(messages -> messages);
    }

    @Override
    public Mono<DeleteMessageResponse> deleteMessageFromQueue(final Message message, final String queueName) {
        return getQueueUrlFromName(queueName).flatMap(queueUrl -> Mono.fromCompletionStage(sqsAsyncClient.deleteMessage(builder -> builder.queueUrl(
                queueUrl).receiptHandle(message.receiptHandle()))));
    }

    @Override
    public Mono<String> getQueueUrlFromName(final String queueName) {
        return Mono.fromCompletionStage(sqsAsyncClient.getQueueUrl(builder -> builder.queueName(queueName)))
                   .map(GetQueueUrlResponse::queueUrl);
    }
}
