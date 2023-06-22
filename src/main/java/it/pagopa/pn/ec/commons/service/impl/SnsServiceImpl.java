package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.service.SnsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@Service
@Slf4j
public class SnsServiceImpl implements SnsService {

    private final SnsAsyncClient snsAsyncClient;

    @Value("sns.topic.sms.stress-test.mode")
    private boolean smsStressTestMode;
    @Value("sns.topic.sms.stress-test.topic-arn")
    private String smsStressTestTopicArn;

    public SnsServiceImpl(SnsAsyncClient snsAsyncClient) {
        this.snsAsyncClient = snsAsyncClient;
    }

    @Override
    public Mono<PublishResponse> send(String phoneNumber, String message) {
        log.info("<-- START SENDING SMS  -->");
        PublishRequest.Builder builder = PublishRequest.builder().message(message);

        if (smsStressTestMode) {
            builder = builder.topicArn(smsStressTestTopicArn);
        } else
            builder = builder.phoneNumber(phoneNumber);

        return Mono.fromFuture(snsAsyncClient.publish(builder.build()))
                   .onErrorResume(throwable -> {
                       log.error(throwable.getMessage());
                       return Mono.error(new SnsSendException());
                   })
                   .doOnSuccess(sendMessageResponse -> log.debug("Send SMS has returned a {} as status", sendMessageResponse.sdkHttpResponse().statusCode()));
    }

}
