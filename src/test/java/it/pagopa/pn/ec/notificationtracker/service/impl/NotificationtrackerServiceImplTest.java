package it.pagopa.pn.ec.notificationtracker.service.impl;

import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.notificationtracker.model.NtStatoError;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.*;
import static it.pagopa.pn.ec.notificationtracker.rest.util.StateMachineUtils.getStatiMacchinaEndpoint;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
class NotificationtrackerServiceImplTest {
    @Autowired
    private WebTestClient webTestClient;
    @Mock
    SqsService sqsService;
    @Mock
    Logger log;
    @InjectMocks
    NotificationtrackerServiceImpl notificationtrackerServiceImpl;


    @Test
    void testGetValidateStatoSmS() throws Exception {
        doThrow(SqsPublishException.class).when(sqsService).send(eq(NT_STATO_SMS_ERRATO_QUEUE_NAME), any(NtStatoError.class));

    }

    @Test
    void testGetValidateStatoEmail() {
        doThrow(SqsPublishException.class).when(sqsService).send(eq(NT_STATO_EMAIL_ERRATO_QUEUE_NAME), any(NtStatoError.class));
    }

    @Test
    void testGetValidateStatoPec() {
        doThrow(SqsPublishException.class).when(sqsService).send(eq(NT_STATO_PEC_ERRATO_QUEUE_NAME), any(NtStatoError.class));
    }

    @Test
    void testGetValidateCartaceStatus() {
        doThrow(SqsPublishException.class).when(sqsService).send(eq(NT_STATO_CARTACEO_ERRATO_QUEUE_NAME), any(NtStatoError.class));
    }

    @Test
    void testPutStatoSms() {
        Mono<Void> result = notificationtrackerServiceImpl.putStatoSms("process", "nextStatus", "clientId");
        Assertions.assertEquals(null, result);
    }

    @Test
    void testPutStatoEmail() {
        Mono<Void> result = notificationtrackerServiceImpl.putStatoEmail("process", "nextStatus", "clientId");
        Assertions.assertEquals(null, result);
    }

    @Test
    void testPutStatoPec() {
        Mono<Void> result = notificationtrackerServiceImpl.putStatoPec("process", "nextStatus", "clientId");
        Assertions.assertEquals(null, result);
    }

    @Test
    void testPutStatoCartaceo() {
        Mono<Void> result = notificationtrackerServiceImpl.putStatoCartaceo("process", "nextStatus", "clientId");
        Assertions.assertEquals(null, result);
    }
}

//Generated with love by TestMe :) Please report issues and submit feature requests at: http://weirddev.com/forum#!/testme