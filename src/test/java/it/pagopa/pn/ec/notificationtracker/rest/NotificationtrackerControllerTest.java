package it.pagopa.pn.ec.notificationtracker.rest;

import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.notificationtracker.model.NtStatoError;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.*;
import static it.pagopa.pn.ec.notificationtracker.rest.util.StateMachineUtils.getStatiMacchinaEndpoint;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
class NotificationtrackerControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @SpyBean
    private SqsServiceImpl sqsService;


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
}