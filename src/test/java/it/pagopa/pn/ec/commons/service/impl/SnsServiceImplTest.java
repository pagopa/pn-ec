package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnsServiceImplTest {

    @Mock
    private SnsAsyncClient snsAsyncClient;

    private SnsServiceImpl snsService;

    @BeforeEach
    void setUp() {
        PnEcConfig pnEcConfig = new PnEcConfig();
        pnEcConfig.getSms().getSnsTopic().setDefaultSenderIdKey("AWS.SNS.SMS.SenderID");
        pnEcConfig.getSms().getSnsTopic().setDefaultSenderIdType("String");
        pnEcConfig.getSms().getSnsTopic().setDefaultSenderIdValue("SEND");
        snsService = new SnsServiceImpl(snsAsyncClient, pnEcConfig);
    }

    @Test
    void send_publishFails_errorIsSnsSendExceptionWithOriginalCause() {
        SdkClientException cause = SdkClientException.create("sns publish failure");
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(CompletableFuture.failedFuture(cause));

        StepVerifier.create(snsService.send("+393331234567", "message"))
                    .expectErrorSatisfies(error -> {
                        assertInstanceOf(SnsSendException.class, error);
                        assertSame(cause, error.getCause());
                    })
                    .verify();
    }

    @Test
    void send_publishFails_messageIsUnchangedAndDoesNotContainCauseMessage() {
        SdkClientException cause = SdkClientException.create("sns publish failure");
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(CompletableFuture.failedFuture(cause));

        StepVerifier.create(snsService.send("+393331234567", "message"))
                    .expectErrorSatisfies(error -> {
                        assertEquals("An error occurred while sending via SNS", error.getMessage());
                        assertFalse(error.getMessage().contains("sns publish failure"));
                    })
                    .verify();
    }

    @Test
    void send_publishSucceeds_returnsResponse() {
        PublishResponse response = PublishResponse.builder().messageId("messageId").build();
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(CompletableFuture.completedFuture(response));

        StepVerifier.create(snsService.send("+393331234567", "message")).expectNext(response).verifyComplete();
    }

}
