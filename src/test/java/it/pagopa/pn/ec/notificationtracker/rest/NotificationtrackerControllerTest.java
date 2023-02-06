package it.pagopa.pn.ec.notificationtracker.rest;

import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCallImpl;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.notificationtracker.model.NotificationResponseModel;
import it.pagopa.pn.ec.notificationtracker.service.NotificationtrackerMessageReceiver;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationtrackerControllerTest {

    @Mock
    PutEventsImpl putEventsImpl;
    @Mock
    SqsService sqsService;

    @Autowired
    NotificationtrackerMessageReceiver notificationtrackerMessageReceiver;

    @MockBean
    private GestoreRepositoryCallImpl gestoreRepositoryCall;

    @MockBean
    private CallMachinaStatiImpl callMachinaStati;

    @Autowired
    private WebTestClient webClient;


    public static final NotificationResponseModel notificationResponseModel = new NotificationResponseModel();
    public static final NotificationTrackerQueueDto  notificationTrackerQueueDto = new NotificationTrackerQueueDto();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetStatoSmS() {

        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_SMS);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();

        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(notificationTrackerQueueDto)).thenReturn(Mono.empty());
        when(callMachinaStati.getStato(processId,req.getCurrentStatus(),req.getXPagopaExtchCxId(),req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));
        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req);

    }

    @Test
    void testGetEmailStatus() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_MAIL);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,req.getCurrentStatus(),req.getXPagopaExtchCxId(),req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));

        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(req)).thenReturn(Mono.empty());
        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req);    }

    @Test
    void testGetPecStatus() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_PEC);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,req.getCurrentStatus(),req.getXPagopaExtchCxId(),req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));


        notificationtrackerMessageReceiver.receivePecObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(req)).thenReturn(Mono.empty());
    }

    @Test
    void testGetCartaceoStatus() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(INVIO_CARTACEO);
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        String processId = req.getProcessId().toString();
        when(callMachinaStati.getStato(processId,req.getCurrentStatus(),req.getXPagopaExtchCxId(),req.getNextStatus())).thenReturn(Mono.just(notificationResponseModel));


        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req);
        when(gestoreRepositoryCall.updateRichiesta(anyString(),eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(req)).thenReturn(Mono.empty());

        putEventsImpl.putEventExternal(req).then();
    }
}

