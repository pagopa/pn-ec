package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.ses.SesSendException;
import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.commons.service.SesService;
import it.pagopa.pn.ec.util.LogSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SesServiceImplTest {

    @Mock
    private SesAsyncClient sesAsyncClient;

    private SesServiceImpl sesService;

    @BeforeEach
    void setUp() {
        sesService = new SesServiceImpl(sesAsyncClient, new LogSanitizer());
    }

    private static EmailField emailField() {
        return EmailField.builder()
                         .from("sender@example.com")
                         .to("receiver@example.com")
                         .subject("subject")
                         .text("text")
                         .contentType("text/plain; charset=UTF-8")
                         .emailAttachments(new ArrayList<>())
                         .build();
    }

    @Test
    void send_sendRawEmailFails_errorIsSesSendExceptionWithOriginalCause() {
        SdkClientException cause = SdkClientException.create("ses failure");
        when(sesAsyncClient.sendRawEmail(any(SendRawEmailRequest.class))).thenReturn(CompletableFuture.failedFuture(cause));

        StepVerifier.create(sesService.send(emailField()))
                    .expectErrorSatisfies(error -> {
                        assertInstanceOf(SesSendException.class, error);
                        assertSame(cause, error.getCause());
                    })
                    .verify();
    }

    @Test
    void send_sendRawEmailFails_messageIsUnchangedAndDoesNotContainCauseMessage() {
        SdkClientException cause = SdkClientException.create("ses failure");
        when(sesAsyncClient.sendRawEmail(any(SendRawEmailRequest.class))).thenReturn(CompletableFuture.failedFuture(cause));

        StepVerifier.create(sesService.send(emailField()))
                    .expectErrorSatisfies(error -> {
                        assertEquals("An error occurred while sending via SES", error.getMessage());
                        assertFalse(error.getMessage().contains("ses failure"));
                    })
                    .verify();
    }

    @Test
    void send_sendRawEmailSucceeds_returnsResponse() {
        SendRawEmailResponse response = SendRawEmailResponse.builder().messageId("messageId").build();
        when(sesAsyncClient.sendRawEmail(any(SendRawEmailRequest.class))).thenReturn(CompletableFuture.completedFuture(response));

        StepVerifier.create(sesService.send(emailField())).expectNext(response).verifyComplete();
    }

    @Test
    void defaultRetryStrategy_retriesExhausted_throwsSesMaxRetriesExceededWithLastFailureAsCause() {
        SesSendException lastFailure = new SesSendException("ses failure");

        StepVerifier.withVirtualTime(() -> Mono.<String>error(lastFailure).retryWhen(SesService.DEFAULT_RETRY_STRATEGY))
                    .thenAwait(Duration.ofMinutes(5))
                    .expectErrorSatisfies(error -> {
                        assertInstanceOf(SesSendException.SesMaxRetriesExceededException.class, error);
                        assertSame(lastFailure, error.getCause());
                    })
                    .verify();
    }

    @Test
    void defaultRetryStrategy_retriesExhausted_messageIsUnchangedAndDoesNotContainLastFailureMessage() {
        SesSendException lastFailure = new SesSendException("ses failure");

        StepVerifier.withVirtualTime(() -> Mono.<String>error(lastFailure).retryWhen(SesService.DEFAULT_RETRY_STRATEGY))
                    .thenAwait(Duration.ofMinutes(5))
                    .expectErrorSatisfies(error -> {
                        assertEquals("SES max retries exceeded", error.getMessage());
                        assertFalse(error.getMessage().contains("ses failure"));
                    })
                    .verify();
    }

}
