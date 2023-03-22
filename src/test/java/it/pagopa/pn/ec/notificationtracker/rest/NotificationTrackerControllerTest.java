package it.pagopa.pn.ec.notificationtracker.rest;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCallImpl;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStatiImpl;
import it.pagopa.pn.ec.notificationtracker.service.impl.NotificationTrackerMessageReceiver;
import it.pagopa.pn.ec.notificationtracker.service.impl.PutEventsImpl;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationTrackerControllerTest {

    @Mock
    PutEventsImpl putEventsImpl;

    @Autowired
    private TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;

    @Autowired
    NotificationTrackerMessageReceiver notificationtrackerMessageReceiver;

    @MockBean
    private GestoreRepositoryCallImpl gestoreRepositoryCall;

    @MockBean
    private CallMacchinaStatiImpl callMachinaStati;
    @Mock
    private Acknowledgment acknowledgment;

    public static final MacchinaStatiValidateStatoResponseDto STATE_MACHINE_DTO = new MacchinaStatiValidateStatoResponseDto();
//    public static final BaseMessageProgressEvent baseMessageProgressEvent = new BaseMessageProgressEvent();
    public static final PaperProgressStatusEvent paperProgressEvent = new PaperProgressStatusEvent();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetStatoSmS() {

        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
        req.setXPagopaExtchCxId("C050");
        req.setProcessId(transactionProcessConfigurationProperties.sms());
        req.setCurrentStatus("BOOKED");
        req.setNextStatus("PROGRESS");
        req.setRequestIdx("123_test");

        var digitalProgressStatusDto = new DigitalProgressStatusDto();
        digitalProgressStatusDto.setEventTimestamp(OffsetDateTime.now());
        digitalProgressStatusDto.setEventDetails(null);
        digitalProgressStatusDto.setGeneratedMessage(null);
        req.setDigitalProgressStatusDto(digitalProgressStatusDto);

        var baseMessageProgressEvent = new BaseMessageProgressEvent();
        baseMessageProgressEvent.setRequestId(req.getRequestIdx());
        baseMessageProgressEvent.setEventTimestamp(req.getDigitalProgressStatusDto().getEventTimestamp());
        var status = Enum.valueOf(ProgressEventCategory.class, req.getNextStatus());
        baseMessageProgressEvent.setStatus(status);
        baseMessageProgressEvent.setEventCode(null);
        baseMessageProgressEvent.setEventDetails(req.getDigitalProgressStatusDto().getEventDetails());
        baseMessageProgressEvent.setGeneratedMessage(null);


        when(callMachinaStati.statusValidation(req)).thenReturn(Mono.just(STATE_MACHINE_DTO));
        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
        when(putEventsImpl.putEventExternal(baseMessageProgressEvent, req.getProcessId())).thenReturn(Mono.empty());

        notificationtrackerMessageReceiver.receiveSMSObjectMessage(req, acknowledgment);

    }

//    @Test
//    void testGetEmailStatus() {
//        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
//        req.setXPagopaExtchCxId("C050");
//        req.setProcessId(transactionProcessConfigurationProperties.email());
//        req.setCurrentStatus("BOOKED");
//        req.setNextStatus("VALIDATE");
//        req.setRequestIdx("123_test");
//
//        when(callMachinaStati.statusValidation(req).thenReturn(Mono.just(STATE_MACHINE_DTO)));
//        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
//        when(putEventsImpl.putEventExternal(baseMessageProgressEvent, req.getProcessId())).thenReturn(Mono.empty());
//
//        notificationtrackerMessageReceiver.receiveEmailObjectMessage(req);
//    }
//
//    @Test
//    void testGetPecStatus() {
//        NotificationTrackerQueueDto req = new NotificationTrackerQueueDto();
//        req.setXPagopaExtchCxId("C050");
//        req.setProcessId(transactionProcessConfigurationProperties.pec());
//        req.setCurrentStatus("BOOKED");
//        req.setNextStatus("VALIDATE");
//        req.setRequestIdx("123_test");
//
//        when(callMachinaStati.statusValidation(req).thenReturn(Mono.just(STATE_MACHINE_DTO)));
//        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), eq(new EventsDto()))).thenReturn(Mono.empty());
//        when(putEventsImpl.putEventExternal(baseMessageProgressEvent, req.getProcessId())).thenReturn(Mono.empty());
//
//        notificationtrackerMessageReceiver.receivePecObjectMessage(req);
//    }

    @Test
    void testGetCartaceoStatus() {
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
}
