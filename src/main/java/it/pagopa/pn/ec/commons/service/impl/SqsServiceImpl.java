package it.pagopa.pn.ec.commons.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.service.SqsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

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
    public <T> Mono<SendMessageResponse> send(String queueName, T queuePayload) throws SqsPublishException {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(queuePayload))
                   .doOnNext(sendMessageResponse -> log.info("Try to publish on {} with payload {}", queueName, sendMessageResponse))
                   .flatMap(jsonPayload -> Mono.fromCompletionStage(sqsAsyncClient.getQueueUrl(builder -> builder.queueName(queueName))
                                                                                  .thenCompose(queueUrlResult -> sqsAsyncClient.sendMessage(
                                                                                          builder -> builder.queueUrl(queueUrlResult.queueUrl())
                                                                                                            .messageBody(jsonPayload)))))
                   .onErrorResume(throwable -> {
                       log.error(throwable.getMessage(), throwable);
                       return Mono.error(new SqsPublishException(queueName));
                   });
    }
}
