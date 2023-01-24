package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.service.SnsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.SnsException;

@Service
@Slf4j
public class SnsServiceImpl implements SnsService {

    private final SnsAsyncClient snsAsyncClient;

    public SnsServiceImpl(SnsAsyncClient snsAsyncClient) {
        this.snsAsyncClient = snsAsyncClient;
    }

    private Mono<Void> sendSmsWithNoRetry(String message, String phoneNumber) {
        return Mono.fromFuture(snsAsyncClient.publish(builder -> {
                       try {
                           builder.message(message).phoneNumber(phoneNumber);
                       } catch (SnsException snsException) {
                           throw new SnsSendException();
                       }
                   }))
                   .doOnNext(sendMessageResponse -> log.info("Send SMS to '{}' has returned a {} as status",
                                                             phoneNumber,
                                                             sendMessageResponse.sdkHttpResponse().statusCode()))
                   .then();
    }

    @Override
    public Mono<Void> send(String message, String phoneNumber) {
        return sendSmsWithNoRetry(message, phoneNumber).retryWhen(DEFAULT_RETRY_STRATEGY);
    }

    @Override
    public Mono<Void> send(String message, String phoneNumber, Retry customRetryStrategy) {
        return sendSmsWithNoRetry(message, phoneNumber).retryWhen(customRetryStrategy);
    }
}
