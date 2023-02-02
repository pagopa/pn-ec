package it.pagopa.pn.ec.notificationtracker.rest;

import it.pagopa.pn.ec.notificationtracker.model.NotificationRequestModel;
import it.pagopa.pn.ec.notificationtracker.service.NotificationtrackerMessageReceiver;
import it.pagopa.pn.ec.notificationtracker.service.impl.NotificationtrackerServiceImpl;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;
@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationtrackerControllerTest {


    @Autowired
    NotificationtrackerMessageReceiver notificationtrackerMessageReceiver;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetStatoSmS() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_SMS");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");

        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req);
    }

    @Test
    void testGetEmailStatus() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_EMAIL");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");
        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req);
    }

    @Test
    void testGetPecStatus() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_PEC");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");
        notificationtrackerMessageReceiver.receivePecObjectMessage(req);
    }

    @Test
    void testGetCartaceoStatus() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_CARTACEO");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");
        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req);
    }
}

