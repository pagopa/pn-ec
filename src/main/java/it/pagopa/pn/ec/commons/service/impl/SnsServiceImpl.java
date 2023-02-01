package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.service.SnsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

@Service
@Slf4j
public class SnsServiceImpl implements SnsService {

    private final SnsAsyncClient snsAsyncClient;

    public SnsServiceImpl(SnsAsyncClient snsAsyncClient) {
        this.snsAsyncClient = snsAsyncClient;
    }

    private Mono<PublishResponse> sendSmsWithNoRetry(String message, String phoneNumber) {
        return Mono.fromFuture(snsAsyncClient.publish(builder -> builder.message(message).phoneNumber(phoneNumber)))
                   .onErrorResume(throwable -> {
                       log.error(throwable.getMessage(), throwable);
                       return Mono.error(new SnsSendException());
                   })
                   .doOnSuccess(sendMessageResponse -> log.info("Send SMS '{} 'to '{}' has returned a {} as status",
                                                                message,
                                                                phoneNumber,
                                                                sendMessageResponse.sdkHttpResponse().statusCode()));
    }

    @Override
    public Mono<PublishResponse> send(String message, String phoneNumber) {
        return sendSmsWithNoRetry(message, phoneNumber).retryWhen(DEFAULT_RETRY_STRATEGY);
    }

    @Override
    public Mono<PublishResponse> send(String message, String phoneNumber, Retry customRetryStrategy) {
        return sendSmsWithNoRetry(message, phoneNumber).retryWhen(customRetryStrategy);
    }
}
