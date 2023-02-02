package it.pagopa.pn.ec.notificationtracker.service.impl;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCallImpl;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.notificationtracker.model.NotificationRequestModel;
import it.pagopa.pn.ec.notificationtracker.model.NotificationResponseModel;
import it.pagopa.pn.ec.notificationtracker.model.NtStatoError;
import it.pagopa.pn.ec.notificationtracker.service.NotificationtrackerMessageReceiver;
import it.pagopa.pn.ec.notificationtracker.service.PutEventsImpl;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.*;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto.EventCodeEnum.C000;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto.StatusEnum.OK;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationtrackerServiceImplTest {

    @Mock
    PutEventsImpl putEventsImpl;
    @Mock
    SqsService sqsService;

    @Autowired
    NotificationtrackerMessageReceiver notificationtrackerMessageReceiver;

    @MockBean
    private GestoreRepositoryCallImpl gestoreRepositoryCall;

    @Autowired
    private WebTestClient webClient;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    @Order(1)
    void testGetValidateStatoSmSOKExternalEvent() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_SMS");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");

        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(req)).thenReturn(Mono.empty());

    }


    @Test
    @Order(2)
    void testGetValidateStatoSmSOKNOExternalEvent() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_SMS");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");

        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());
    }

    @Test
    @Order(3)
    void testGetValidateStatoSmSKOCodaErrore() {
        NotificationRequestModel req = new NotificationRequestModel();
        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req);
        NtStatoError ntStatoError = new NtStatoError();
        ntStatoError.setClientId("C00");
        ntStatoError.setCurrStatus("BOOKED");
        ntStatoError.setProcessId("INVIO_SMS");
        when(sqsService.send(eq(NT_STATO_SMS_ERRATO_QUEUE_NAME), any(NtStatoError.class))).thenReturn(Mono.error(new SqsPublishException(NT_STATO_SMS_ERRATO_QUEUE_NAME)));

    }

    @Test
    @Order(4)
    void testGetValidateStatoSmSGestioneRepoKO() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_SMS");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");

        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenThrow(EcInternalEndpointHttpException.class);

    }


    @Test
    @Order(5)
    void testGetValidateStatoSmSGestioneRepoOK() {

        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_SMS");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");

        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());
    }


    @Test
    @Order(6)
    void testGetValidateStatoSmSKOEvent() {

        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_SMS");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");

        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(req)).thenThrow(EcInternalEndpointHttpException.class);
    }

    @Test
    @Order(7)
    void testGetValidateStatoSmSMachinaStati() {

        String process = "INVIO_SMS";
        String currStato = "BOOKED";
        String clientId = "C050";
        String nextStatus = "COMPOSED";
        webClient.get()
                .uri("http://localhost:8080/statemachinemanager/validate/" +process +"/"+ currStato +"?clientId="+clientId + "&nextStatus="+ nextStatus)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound();
    }



/*
EMAIL TEST
 */
    @Test
    @Order(8)
    void testGetValidateStatoEmailOKExtenalEvent() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_EMAIL");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");
        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(req)).thenReturn(Mono.empty());


    }

    @Test
    @Order(9)
    void testGetValidateStatoEmailNOExternalEvetn() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_EMAIL");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");
        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());

    }

    @Test
    @Order(10)
    void testGetValidateStatoEmailKO() {
        NotificationRequestModel req = new NotificationRequestModel();
        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req);
        when(sqsService.send(eq(NT_STATO_EMAIL_ERRATO_QUEUE_NAME), any(NtStatoError.class))).thenReturn(Mono.error(new SqsPublishException(NT_STATO_EMAIL_ERRATO_QUEUE_NAME)));

    }



    @Test
    @Order(11)
    void testGetValidateStatoEmailGestioneRepoKO() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_SMS");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");

        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenThrow(EcInternalEndpointHttpException.class);
    }

    @Test
    @Order(12)
    void testGetValidateStatoEmailGestioneRepoOK() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_EMAIL");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");
        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());
    }
    @Test
    @Order(13)
    void testGetValidateStatoEmailKOEvent() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_EMAIL");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");
        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(req)).thenThrow(EcInternalEndpointHttpException.class);
    }


    @Test
    @Order(14)
    void testGetValidateStatoEmailKOMachinaStati() {
        String process = "INVIO_EMAIL";
        String currStato = "BOOKED";
        String clientId = "C050";
        String nextStatus = "COMPOSED";
        webClient.get()
                .uri("http://localhost:8080/statemachinemanager/validate/" +process +"/"+ currStato +"?clientId="+clientId + "&nextStatus="+ nextStatus)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound();
    }



    /*
        PEC TEST
 */
    @Test
    @Order(15)
    void testGetValidateStatoPecOKExternalEvent() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_PEC");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");

        notificationtrackerMessageReceiver.receivePecObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(req)).thenReturn(Mono.empty());

    }

    @Test
    @Order(16)
    void testGetValidateStatoPecOKNOExternalEvent() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_PEC");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");

        notificationtrackerMessageReceiver.receivePecObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());

    }

    @Test
    @Order(17)
    void testGetValidateStatoPecKOCodaErrore() {
        NotificationRequestModel req = new NotificationRequestModel();
        notificationtrackerMessageReceiver.receivePecObjectMessage(req);
        when(sqsService.send(eq(NT_STATO_PEC_ERRATO_QUEUE_NAME), any(NtStatoError.class))).thenReturn(Mono.error(new SqsPublishException(NT_STATO_PEC_ERRATO_QUEUE_NAME)));

    }

    @Test
    @Order(18)
    void testGetValidateStatoPecGestioneRepoKO() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_SMS");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");
        notificationtrackerMessageReceiver.receivePecObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenThrow(EcInternalEndpointHttpException.class);

    }

    @Test
    @Order(19)
    void testGetValidateStatoPecGestioneRepoOK() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_SMS");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");
        notificationtrackerMessageReceiver.receivePecObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());

    }

    @Test
    @Order(20)
    void testGetValidateStatoPecKOEvent() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_SMS");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");
        notificationtrackerMessageReceiver.receivePecObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(req)).thenThrow(EcInternalEndpointHttpException.class);

    }

    @Test
    @Order(21)
    void testGetValidateStatoPecKOMachinaStati() {
        String process = "INVIO_PEC";
        String currStato = "BOOKED";
        String clientId = "C050";
        String nextStatus = "COMPOSED";
        webClient.get()
                .uri("http://localhost:8080/statemachinemanager/validate/" +process +"/"+ currStato +"?clientId="+clientId + "&nextStatus="+ nextStatus)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound();
    }


    /*
        CARTACEO TEST
 */

    @Test
    @Order(22)
    void testGetValidateStatoCartaceoOKExternalEvent() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_SMS");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");

        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(req)).thenReturn(Mono.empty());


    }
    @Test
    @Order(23)
    void testGetValidateStatoCartaceoOKNOExternalEvent() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_SMS");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");

        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());


    }

    @Test
    @Order(23)
    void testGetValidateStatoCartaceoKOCodaErrore() {
        NotificationRequestModel req = new NotificationRequestModel();

        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req);
        when(sqsService.send(eq(NT_STATO_CARTACEO_QUEUE_NAME), any(NtStatoError.class))).thenReturn(Mono.error(new SqsPublishException(NT_STATO_CARTACEO_QUEUE_NAME)));


    }

    @Test
    @Order(24)
    void testGetValidateStatoCartaceoGestioneRepoKO() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_SMS");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");

        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenThrow(EcInternalEndpointHttpException.class);

    }

    @Test
    @Order(25)
    void testGetValidateStatoCartaceoGestioneRepoOK() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_SMS");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");

        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());

    }

    @Test
    @Order(26)
    void testGetValidateStatoCartaceoKOEvent() {
        NotificationRequestModel req = new NotificationRequestModel();
        req.setProcessId("INVIO_SMS");
        req.setCurrStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setXpagopaExtchCxId("C050");

        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(req)).thenThrow(EcInternalEndpointHttpException.class);
    }

    @Test
    @Order(27)
    void testGetValidateStatoCartaceoMachinaStati() {
        String process = "INVIO_CARTACEO";
        String currStato = "BOOKED";
        String clientId = "C050";
        String nextStatus = "COMPOSED";
        webClient.get()
                .uri("http://localhost:8080/statemachinemanager/validate/" +process +"/"+ currStato +"?clientId="+clientId + "&nextStatus="+ nextStatus)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound();
    }
}

