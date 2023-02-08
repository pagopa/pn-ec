package it.pagopa.pn.ec.notificationtracker.service.impl;

import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.constant.ProcessId;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCallImpl;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.notificationtracker.model.NotificationResponseModel;
import it.pagopa.pn.ec.notificationtracker.model.NtStatoError;
import it.pagopa.pn.ec.notificationtracker.service.NotificationTrackerMessageReceiver;
import it.pagopa.pn.ec.notificationtracker.service.PutEventsImpl;
import it.pagopa.pn.ec.notificationtracker.service.callmachinestati.CallMachinaStatiImpl;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.ProcessId.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationTrackerServiceImplTest {

    @Mock
    PutEventsImpl putEventsImpl;
    @Mock
    SqsService sqsService;

    @Autowired
    NotificationTrackerMessageReceiver notificationtrackerMessageReceiver;

    @MockBean
    private GestoreRepositoryCallImpl gestoreRepositoryCall;

    @MockBean
    private CallMachinaStatiImpl callMachinaStati;

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    public static final NotificationResponseModel notificationResponseModel = new NotificationResponseModel();
    public static final NotificationTrackerQueueDto notificationTrackerQueueDto = new NotificationTrackerQueueDto();

    @Test
    @Order(1)
    void testGetValidateStatoSmSOKExternalEvent() {

        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_SMS);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();

        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(notificationTrackerQueueDto)).thenReturn(Mono.empty());
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));
        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req);

    }


    @Test
    @Order(2)
    void testGetValidateStatoSmSOKNOExternalEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_SMS);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();

        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));
        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req);

    }

    @Test
    @Order(3)
    void testGetValidateStatoSmSKOCodaErrore() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();

        String processId = "INVIO_CARTACEO";
        req.setProcessId(ProcessId.valueOf(processId));

        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));

        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req);
        when(sqsService.send(eq(notificationTrackerSqsName.statoSmsErratoName()),
                             any(NtStatoError.class))).thenReturn(Mono.error(new SqsPublishException(notificationTrackerSqsName.statoSmsErratoName())));

    }

    @Test
    @Order(4)
    void testGetValidateStatoSmSGestioneRepoKO() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_SMS);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));
        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenThrow(EcInternalEndpointHttpException.class);

    }


    @Test
    @Order(5)
    void testGetValidateStatoSmSGestioneRepoOK() {

        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_SMS);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));


        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
    }


    @Test
    @Order(6)
    void testGetValidateStatoSmSKOEvent() {

        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_SMS);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));


        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(req)).thenThrow(EcInternalEndpointHttpException.class);
        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req);
    }

    @Test
    @Order(7)
    void testGetValidateStatoSmSMachinaStati() {

        String process = "INVIO_SMS";
        String currStato = "BOOKED";
        String clientId = "C050";
        String nextStatus = "COMPOSED";
        webClient.get()
                 .uri("http://localhost:8080/statemachinemanager/validate/" + process + "/" + currStato + "?clientId=" + clientId +
                      "&nextStatus=" + nextStatus)
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isNotFound();
    }

    //
//
//
///*
//EMAIL TEST
// */
    @Test
    @Order(8)
    void testGetValidateStatoEmailOKExtenalEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_MAIL);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));

        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(req)).thenReturn(Mono.empty());
        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req);


    }

    @Test
    @Order(9)
    void testGetValidateStatoEmailNOExternalEvetn() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_MAIL);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));

        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req);

    }

    @Test
    @Order(10)
    void testGetValidateStatoEmailKO() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();

        String processId = "INVIO_CARTACEO";
        req.setProcessId(ProcessId.valueOf(processId));

        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));

        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req);

        when(sqsService.send(eq(notificationTrackerSqsName.statoSmsErratoName()),
                             any(NtStatoError.class))).thenReturn(Mono.error(new SqsPublishException(notificationTrackerSqsName.statoSmsErratoName())));


    }


    @Test
    @Order(11)
    void testGetValidateStatoEmailGestioneRepoKO() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_MAIL);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));


        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenThrow(EcInternalEndpointHttpException.class);
    }

    @Test
    @Order(12)
    void testGetValidateStatoEmailGestioneRepoOK() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_MAIL);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));

        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
    }

    @Test
    @Order(13)
    void testGetValidateStatoEmailKOEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_MAIL);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));

        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
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
                 .uri("http://localhost:8080/statemachinemanager/validate/" + process + "/" + currStato + "?clientId=" + clientId +
                      "&nextStatus=" + nextStatus)
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isNotFound();
    }

    //
//
//
//    /*
//        PEC TEST
// */
    @Test
    @Order(15)
    void testGetValidateStatoPecOKExternalEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_PEC);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));


        notificationtrackerMessageReceiver.receivePecObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(req)).thenReturn(Mono.empty());

    }

    @Test
    @Order(16)
    void testGetValidateStatoPecOKNOExternalEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_PEC);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));


        notificationtrackerMessageReceiver.receivePecObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());

    }

    @Test
    @Order(17)
    void testGetValidateStatoPecKOCodaErrore() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();

        String processId = "INVIO_CARTACEO";
        req.setProcessId(ProcessId.valueOf(processId));

        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));

        notificationtrackerMessageReceiver.receivePecObjectMessage(req);
        when(sqsService.send(eq(notificationTrackerSqsName.statoSmsErratoName()),
                             any(NtStatoError.class))).thenReturn(Mono.error(new SqsPublishException(notificationTrackerSqsName.statoSmsErratoName())));

    }

    @Test
    @Order(18)
    void testGetValidateStatoPecGestioneRepoKO() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_PEC);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));

        notificationtrackerMessageReceiver.receivePecObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenThrow(EcInternalEndpointHttpException.class);

    }

    @Test
    @Order(19)
    void testGetValidateStatoPecGestioneRepoOK() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_PEC);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));

        notificationtrackerMessageReceiver.receivePecObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());

    }

    @Test
    @Order(20)
    void testGetValidateStatoPecKOEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_PEC);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));


        when(putEventsImpl.putEventExternal(req)).thenThrow(EcInternalEndpointHttpException.class);
        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
        notificationtrackerMessageReceiver.receivePecObjectMessage(req);

    }

    @Test
    @Order(21)
    void testGetValidateStatoPecKOMachinaStati() {
        String process = "INVIO_PEC";
        String currStato = "BOOKED";
        String clientId = "C050";
        String nextStatus = "COMPOSED";
        webClient.get()
                 .uri("http://localhost:8080/statemachinemanager/validate/" + process + "/" + currStato + "?clientId=" + clientId +
                      "&nextStatus=" + nextStatus)
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isNotFound();
    }

    //
//
//    /*
//        CARTACEO TEST
// */
//
    @Test
    @Order(22)
    void testGetValidateStatoCartaceoOKExternalEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_CARTACEO);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));


        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(req)).thenReturn(Mono.empty());


    }

    @Test
    @Order(23)
    void testGetValidateStatoCartaceoOKNOExternalEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_CARTACEO);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));


        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());


    }

    @Test
    @Order(23)
    void testGetValidateStatoCartaceoKOCodaErrore() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();

        String processId = "INVIO_CARTACEO";
        req.setProcessId(ProcessId.valueOf(processId));

        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));

        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req);

        when(sqsService.send(eq(notificationTrackerSqsName.statoCartaceoName()),
                             any(NtStatoError.class))).thenReturn(Mono.error(new SqsPublishException(notificationTrackerSqsName.statoCartaceoName())));
    }

    @Test
    @Order(24)
    void testGetValidateStatoCartaceoGestioneRepoKO() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_CARTACEO);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));


        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenThrow(EcInternalEndpointHttpException.class);

    }

    @Test
    @Order(25)
    void testGetValidateStatoCartaceoGestioneRepoOK() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_CARTACEO);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));


        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());

    }

    @Test
    @Order(26)
    void testGetValidateStatoCartaceoKOEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_CARTACEO);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,
                                       req.getCurrentStatus(),
                                       req.getXPagopaExtchCxId(),
                                       req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));


        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
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
                 .uri("http://localhost:8080/statemachinemanager/validate/" + process + "/" + currStato + "?clientId=" + clientId +
                      "&nextStatus=" + nextStatus)
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isNotFound();
    }
}
