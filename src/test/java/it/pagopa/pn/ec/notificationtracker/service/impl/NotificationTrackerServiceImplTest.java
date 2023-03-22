package it.pagopa.pn.ec.notificationtracker.service.impl;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.statemachine.StateMachineEndpointProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiDecodeResponseDto;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCallImpl;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStatiImpl;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.notificationtracker.model.NtStatoError;
import it.pagopa.pn.ec.rest.v1.dto.BaseMessageProgressEvent;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusEvent;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationTrackerServiceImplTest {

    @SpyBean
    private PutEventsImpl putEventsImpl;
    @SpyBean
    private SqsService sqsService;

    @Autowired
    private NotificationTrackerMessageReceiver notificationtrackerMessageReceiver;
    @Autowired
    private TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;
    @Autowired
    private StateMachineEndpointProperties stateMachineEndpoint;
    @Autowired
    private WebTestClient webClient;
    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @MockBean
    private GestoreRepositoryCallImpl gestoreRepositoryCall;
    @MockBean
    private CallMacchinaStatiImpl callMachinaStati;
    @Mock
    private Acknowledgment acknowledgment;

    public static final MacchinaStatiValidateStatoResponseDto STATE_MACHINE_DTO = new MacchinaStatiValidateStatoResponseDto();
    public static final BaseMessageProgressEvent baseMessageProgressEvent = new BaseMessageProgressEvent();
    public static final PaperProgressStatusEvent paperProgressEvent = new PaperProgressStatusEvent();

    @Test
    @Order(1)
    void testGetValidateStatoSmSOKExternalEvent() {

        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setProcessId(transactionProcessConfigurationProperties.sms());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        req.setXPagopaExtchCxId("C050");

        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(MacchinaStatiValidateStatoResponseDto.builder()
                                                                                                               .allowed(true)
                                                                                                               .build()));
        when(callMachinaStati.statusDecode(req)).thenReturn(Mono.just(new MacchinaStatiDecodeResponseDto()));
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(baseMessageProgressEvent, req.getProcessId())).thenReturn(Mono.empty());

        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(2)
    void testGetValidateStatoSmSOKNOExternalEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.sms());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");

        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());

        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(3)
    void testGetValidateStatoSmSKOCodaErrore() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setProcessId(transactionProcessConfigurationProperties.paper());

        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(sqsService.send(eq(notificationTrackerSqsName.statoSmsErratoName()),
                             any(NtStatoError.class))).thenReturn(Mono.error(new SqsPublishException(notificationTrackerSqsName.statoSmsErratoName())));

        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(4)
    void testGetValidateStatoSmSGestioneRepoKO() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.sms());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");

        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenThrow(EcInternalEndpointHttpException.class);

        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(5)
    void testGetValidateStatoSmSGestioneRepoOK() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.sms());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");

        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());

        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(6)
    void testGetValidateStatoSmSKOEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.sms());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");

        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(baseMessageProgressEvent, req.getProcessId())).thenThrow(EcInternalEndpointHttpException.class);

        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(7)
    void testGetValidateStatoSmSMachinaStati() {
        String process = "INVIO_SMS";
        String currStato = "BOOKED";
        String clientId = "C050";
        String nextStatus = "COMPOSED";

        webClient.get()
                 .uri(uriBuilder -> uriBuilder.path(stateMachineEndpoint.validate())
                                              .queryParam("clientId", clientId)
                                              .queryParam("nextStatus", nextStatus)
                                              .build(process, currStato))
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isNotFound();
    }

    @Test
    @Order(8)
    void testGetValidateStatoEmailOKExternalEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.email());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");

        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(baseMessageProgressEvent, req.getProcessId())).thenReturn(Mono.empty());

        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(9)
    void testGetValidateStatoEmailNOExternalEvetn() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.email());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");

        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());

        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(10)
    void testGetValidateStatoEmailKO() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setProcessId(transactionProcessConfigurationProperties.paper());

        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(sqsService.send(eq(notificationTrackerSqsName.statoSmsErratoName()),
                             any(NtStatoError.class))).thenReturn(Mono.error(new SqsPublishException(notificationTrackerSqsName.statoSmsErratoName())));

        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(11)
    void testGetValidateStatoEmailGestioneRepoKO() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.email());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");

        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenThrow(EcInternalEndpointHttpException.class);

        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(12)
    void testGetValidateStatoEmailGestioneRepoOK() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.email());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());

        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(13)
    void testGetValidateStatoEmailKOEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.email());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));

        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(baseMessageProgressEvent, req.getProcessId())).thenThrow(EcInternalEndpointHttpException.class);

        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(14)
    void testGetValidateStatoEmailKOMachinaStati() {
        String process = "INVIO_EMAIL";
        String currStato = "BOOKED";
        String clientId = "C050";
        String nextStatus = "COMPOSED";
        webClient.get()
                 .uri(uriBuilder -> uriBuilder.path(stateMachineEndpoint.validate())
                                              .queryParam("clientId", clientId)
                                              .queryParam("nextStatus", nextStatus)
                                              .build(process, currStato))
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
        req.setProcessId(transactionProcessConfigurationProperties.pec());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));

        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(baseMessageProgressEvent, req.getProcessId())).thenReturn(Mono.empty());

        notificationtrackerMessageReceiver.receivePecObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(16)
    void testGetValidateStatoPecOKNOExternalEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.pec());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());

        notificationtrackerMessageReceiver.receivePecObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(17)
    void testGetValidateStatoPecKOCodaErrore() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();

        req.setProcessId(transactionProcessConfigurationProperties.paper());

        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));

        notificationtrackerMessageReceiver.receivePecObjectMessage(req, acknowledgment);
        when(sqsService.send(eq(notificationTrackerSqsName.statoSmsErratoName()),
                             any(NtStatoError.class))).thenReturn(Mono.error(new SqsPublishException(notificationTrackerSqsName.statoSmsErratoName())));

    }

    @Test
    @Order(18)
    void testGetValidateStatoPecGestioneRepoKO() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.pec());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenThrow(EcInternalEndpointHttpException.class);

        notificationtrackerMessageReceiver.receivePecObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(19)
    void testGetValidateStatoPecGestioneRepoOK() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.pec());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());

        notificationtrackerMessageReceiver.receivePecObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(20)
    void testGetValidateStatoPecKOEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.pec());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));

        when(putEventsImpl.putEventExternal(baseMessageProgressEvent, req.getProcessId())).thenThrow(EcInternalEndpointHttpException.class);
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());

        notificationtrackerMessageReceiver.receivePecObjectMessage(req, acknowledgment);

    }

    @Test
    @Order(21)
    void testGetValidateStatoPecKOMachinaStati() {
        String process = "INVIO_PEC";
        String currStato = "BOOKED";
        String clientId = "C050";
        String nextStatus = "COMPOSED";
        webClient.get()
                 .uri(uriBuilder -> uriBuilder.path(stateMachineEndpoint.validate())
                                              .queryParam("clientId", clientId)
                                              .queryParam("nextStatus", nextStatus)
                                              .build(process, currStato))
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
        req.setProcessId(transactionProcessConfigurationProperties.paper());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));

        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(paperProgressEvent, req.getProcessId())).thenReturn(Mono.empty());

        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(23)
    void testGetValidateStatoCartaceoOKNOExternalEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.paper());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());

        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(23)
    void testGetValidateStatoCartaceoKOCodaErrore() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();

        req.setProcessId(transactionProcessConfigurationProperties.paper());

        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));

        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req, acknowledgment);

        when(sqsService.send(eq(notificationTrackerSqsName.statoCartaceoName()),
                             any(NtStatoError.class))).thenReturn(Mono.error(new SqsPublishException(notificationTrackerSqsName.statoCartaceoName())));
    }

    @Test
    @Order(24)
    void testGetValidateStatoCartaceoGestioneRepoKO() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.paper());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenThrow(EcInternalEndpointHttpException.class);

        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(25)
    void testGetValidateStatoCartaceoGestioneRepoOK() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.paper());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");
        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());

        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(26)
    void testGetValidateStatoCartaceoKOEvent() {
        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.paper());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("VALIDATE");
        req.setRequestIdx("123_test");

        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));

        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(paperProgressEvent, req.getProcessId())).thenThrow(EcInternalEndpointHttpException.class);

        notificationtrackerMessageReceiver.receiveCartaceoObjectMessage(req, acknowledgment);
    }

    @Test
    @Order(27)
    void testGetValidateStatoCartaceoMachinaStati() {
        String process = "INVIO_CARTACEO";
        String currStato = "BOOKED";
        String clientId = "C050";
        String nextStatus = "COMPOSED";
        webClient.get()
                 .uri(uriBuilder -> uriBuilder.path(stateMachineEndpoint.validate())
                                              .queryParam("clientId", clientId)
                                              .queryParam("nextStatus", nextStatus)
                                              .build(process, currStato))
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isNotFound();
    }
}
