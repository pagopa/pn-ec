package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.configurationproperties.sns.SnsTopicProperties;
import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.service.SnsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import java.util.Map;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@Service
@Slf4j
public class SnsServiceImpl implements SnsService {

    private final SnsAsyncClient snsAsyncClient;
    private final SnsTopicProperties snsTopicProperties;

    @Value("${SMSStressTestMode:false}")
    private boolean smsStressTestMode;
    @Value("${SMSStressTestTopicArn:}")
    private String smsStressTestTopicArn;

    public SnsServiceImpl(SnsAsyncClient snsAsyncClient, SnsTopicProperties snsTopicProperties) {
        this.snsAsyncClient = snsAsyncClient;
        this.snsTopicProperties = snsTopicProperties;
    }

    @Override
    public Mono<PublishResponse> send(String phoneNumber, String message) {
        log.info(CLIENT_METHOD_INVOCATION, SNS_SEND);
        PublishRequest.Builder builder = PublishRequest.builder().message(message);

        if (smsStressTestMode) {
            builder = builder.topicArn(smsStressTestTopicArn);
        } else
            builder = builder.phoneNumber(phoneNumber);

        return Mono.fromFuture(snsAsyncClient.publish(builder
                .message(message)
                .messageAttributes(Map.of(snsTopicProperties.defaultSenderIdKey(), MessageAttributeValue
                        .builder()
                        .dataType(snsTopicProperties.defaultSenderIdType())
                        .stringValue(snsTopicProperties.defaultSenderIdValue())
                        .build())).build()))
        .onErrorResume(throwable -> {
            log.error(EXCEPTION_IN_PROCESS, SNS_SEND, throwable, throwable.getMessage());
            return Mono.error(new SnsSendException());
        })
        .doOnSuccess(sendMessageResponse -> log.debug(CLIENT_METHOD_RETURN,SNS_SEND, sendMessageResponse));
    }

}
