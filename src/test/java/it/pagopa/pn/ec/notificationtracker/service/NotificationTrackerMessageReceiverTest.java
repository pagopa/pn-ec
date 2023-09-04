package it.pagopa.pn.ec.notificationtracker.service;

import com.amazonaws.services.s3.transfer.internal.future.FutureImpl;
import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiDecodeResponseDto;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.commons.utils.RestUtils;
import it.pagopa.pn.ec.notificationtracker.service.impl.NotificationTrackerMessageReceiver;
import it.pagopa.pn.ec.repositorymanager.model.entity.DigitalRequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.DigitalRequestPersonal;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestPersonal;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Patch;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Request;
import it.pagopa.pn.ec.repositorymanager.service.RequestService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.Status.BOOKED;
import static it.pagopa.pn.ec.commons.constant.Status.SENT;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
public class NotificationTrackerMessageReceiverTest {
    @Autowired
    private NotificationTrackerMessageReceiver notificationTrackerMessageReceiver;
    @SpyBean
    private NotificationTrackerService notificationTrackerService;
    @MockBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @Autowired
    private RequestService requestService;
    @Autowired
    private TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;
    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;
    @SpyBean
    private PutEvents putEvents;
    @MockBean
    private CallMacchinaStati callMacchinaStati;

    @MockBean
    Acknowledgment acknowledgment;
    @Autowired
    RestUtils restUtils;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String REQUEST_IDX = "REQUEST_IDX";
    private static final String CLIENT_ID = "CLIENT_ID";


    @BeforeAll
    public static void initialize() {

    }

    @BeforeEach
    public void mockBeans() {
        when(acknowledgment.acknowledge()).thenReturn(new FutureImpl<>());

        when(gestoreRepositoryCall.getRichiesta(anyString(), anyString())).thenAnswer(invocation -> {
            String clientId = invocation.getArgument(0);
            String requestId = invocation.getArgument(1);
            Request request = requestService.getRequest(clientId, requestId).block();
            RequestDto requestDto = restUtils.entityToDto(request, RequestDto.class);
            return Mono.just(requestDto);
        });

        when(gestoreRepositoryCall.patchRichiestaEvent(anyString(), anyString(), any(EventsDto.class))).thenAnswer(invocation -> {
            String clientId = invocation.getArgument(0);
            String requestId = invocation.getArgument(1);
            EventsDto eventsDto = invocation.getArgument(2);
            PatchDto patchDto = new PatchDto().event(eventsDto);
            Patch patch = restUtils.entityToDto(patchDto, Patch.class);
            Request request = requestService.patchRequest(clientId, requestId, patch).block();
            RequestDto requestDto = restUtils.entityToDto(request, RequestDto.class);
            return Mono.just(requestDto);
        });

        when(callMacchinaStati.statusValidation(anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.just(new MacchinaStatiValidateStatoResponseDto()));
        when(callMacchinaStati.statusDecode(anyString(), anyString(), anyString())).thenReturn(Mono.just(MacchinaStatiDecodeResponseDto.builder().logicStatus("logicStatus").externalStatus("externalStatus").build()));


        requestService.insertRequest(Request.builder()
                        .requestId("REQUEST_IDX")
                        .xPagopaExtchCxId("CLIENT_ID")
                        .requestPersonal(RequestPersonal.builder().digitalRequestPersonal(new DigitalRequestPersonal()).build())
                        .requestMetadata(RequestMetadata.builder().digitalRequestMetadata(new DigitalRequestMetadata()).build())
                        .build())
                .block();
    }

    @Test
    void smsHandleRequestStatusChangeOk() {

        //GIVEN
        PresaInCaricoInfo smsPresaInCaricoInfo = PresaInCaricoInfo.builder().requestIdx(REQUEST_IDX).xPagopaExtchCxId(CLIENT_ID).build();
        DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto().status(BOOKED.getStatusTransactionTableCompliant());
        NotificationTrackerQueueDto notificationTrackerQueueDto = NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital(smsPresaInCaricoInfo, SENT.getStatusTransactionTableCompliant(), digitalProgressStatusDto);

        //WHEN

        //THEN
        notificationTrackerMessageReceiver.receiveSMSObjectMessage(notificationTrackerQueueDto, acknowledgment);
        verify(notificationTrackerService, times(1)).handleRequestStatusChange(eq(notificationTrackerQueueDto), eq(transactionProcessConfigurationProperties.sms()), eq(notificationTrackerSqsName.statoSmsName()), eq(notificationTrackerSqsName.statoSmsErratoName()), eq(acknowledgment));
        verify(putEvents, times(1)).putEventExternal(any(SingleStatusUpdate.class), eq(transactionProcessConfigurationProperties.sms()));
    }

}
