package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.service.SnsService;
import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import java.util.Map;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@Service
@CustomLog
public class SnsServiceImpl implements SnsService {

    private final SnsAsyncClient snsAsyncClient;
    private final PnEcConfig.Sms.SnsTopic snsTopicProperties;
    private final PnEcConfig.Sms smsProperties;

    public SnsServiceImpl(SnsAsyncClient snsAsyncClient, PnEcConfig pnEcConfig) {
        this.snsAsyncClient = snsAsyncClient;
        this.snsTopicProperties = pnEcConfig.getSms().getSnsTopic();
        this.smsProperties = pnEcConfig.getSms();
    }

    @Override
    public Mono<PublishResponse> send(String phoneNumber, String message) {
        log.info(CLIENT_METHOD_INVOCATION, SNS_SEND);
        PublishRequest.Builder builder = PublishRequest.builder().message(message);

        if (Boolean.TRUE.equals(smsProperties.getStressTestMode())) {
            builder = builder.topicArn(smsProperties.getStressTestTopicArn());
        } else
            builder = builder.phoneNumber(phoneNumber);

        return Mono.fromFuture(snsAsyncClient.publish(builder
                .message(message)
                .messageAttributes(Map.of(snsTopicProperties.getDefaultSenderIdKey(), MessageAttributeValue
                        .builder()
                        .dataType(snsTopicProperties.getDefaultSenderIdType())
                        .stringValue(snsTopicProperties.getDefaultSenderIdValue())
                        .build())).build()))
        .onErrorResume(throwable -> {
            log.error(EXCEPTION_IN_PROCESS, SNS_SEND, throwable, throwable.getMessage());
            return Mono.error(new SnsSendException());
        })
        .doOnSuccess(sendMessageResponse -> log.debug(CLIENT_METHOD_RETURN,SNS_SEND, sendMessageResponse));
    }

}
