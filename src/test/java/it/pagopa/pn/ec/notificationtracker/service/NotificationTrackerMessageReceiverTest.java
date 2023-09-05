package it.pagopa.pn.ec.notificationtracker.service;

import com.amazonaws.services.s3.transfer.internal.future.FutureImpl;
import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.exception.InvalidNextStatusException;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiDecodeResponseDto;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.utils.RestUtils;
import it.pagopa.pn.ec.notificationtracker.service.impl.NotificationTrackerMessageReceiver;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.model.entity.*;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Patch;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Request;
import it.pagopa.pn.ec.repositorymanager.service.RequestService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.org.bouncycastle.cert.ocsp.Req;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.awt.print.Paper;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.commons.constant.Status.BOOKED;
import static it.pagopa.pn.ec.commons.constant.Status.SENT;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    @SpyBean
    private SqsService sqsService;
    @MockBean
    private CallMacchinaStati callMacchinaStati;
    @MockBean
    Acknowledgment acknowledgment;
    @Autowired
    RestUtils restUtils;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String SMS_REQUEST_IDX = "SMS_REQUEST_IDX";
    private static final String EMAIL_REQUEST_IDX = "EMAIL_REQUEST_IDX";
    private static final String PEC_REQUEST_IDX = "PEC_REQUEST_IDX";
    private static final String PAPER_REQUEST_IDX = "PAPER_REQUEST_IDX";
    private static final String CLIENT_ID = "CLIENT_ID";

    private static final List<JSONObject> stateMachine = new ArrayList<>();

    private static DynamoDbTable<RequestPersonal> requestPersonalDynamoDbTable;
    private static DynamoDbTable<RequestMetadata> requestMetadataDynamoDbTable;

    @BeforeAll
    public static void initialize(@Autowired DynamoDbEnhancedClient dynamoDbTestEnhancedClient,
                                  @Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) throws IOException, JSONException {
        buildStateMachine();

        requestPersonalDynamoDbTable = dynamoDbTestEnhancedClient.table(gestoreRepositoryDynamoDbTableName.richiestePersonalName(),
                TableSchema.fromBean(RequestPersonal.class));
        requestMetadataDynamoDbTable = dynamoDbTestEnhancedClient.table(gestoreRepositoryDynamoDbTableName.richiesteMetadataName(),
                TableSchema.fromBean(RequestMetadata.class));

        insertSmsRequest();
        insertEmailRequest();
        insertPecRequest();
        insertPaperRequest();
    }

    @BeforeEach
    public void mockBeans() {
        //when(acknowledgment.acknowledge()).thenReturn(new FutureImpl<>());

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

        when(callMacchinaStati.statusDecode(anyString(), anyString(), anyString())).thenAnswer(invocation -> {

            var processId = invocation.getArgument(1);
            var statusToDecode = invocation.getArgument(2);

            var statusArray = stateMachine.stream().filter(jsonObject -> {
                        try {
                            var processClientId = jsonObject.getJSONObject("processClientId").get("S");
                            var currStatus = jsonObject.getJSONObject("currStatus").get("S");
                            return processClientId.equals(processId) && currStatus.equals(statusToDecode);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(jsonObject -> {
                        try {
                            String[] statuses = new String[2];
                            statuses[0] = (String) jsonObject.getJSONObject("externalStatus").get("S");
                            var logicStatus = jsonObject.optJSONObject("logicStatus");
                            if (logicStatus != null)
                                statuses[1] = (String) logicStatus.get("S");
                            else statuses[1] = null;
                            return statuses;
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .findFirst()
                    .get();

            return Mono.just(MacchinaStatiDecodeResponseDto.builder().externalStatus(statusArray[0]).logicStatus(statusArray[1]).build());
        });

    }

    private Stream<Arguments> provideArguments() {
        return Stream.of(Arguments.of(SMS_REQUEST_IDX, transactionProcessConfigurationProperties.sms(), notificationTrackerSqsName.statoSmsName(), notificationTrackerSqsName.statoSmsErratoName()),
                Arguments.of(EMAIL_REQUEST_IDX, transactionProcessConfigurationProperties.email(), notificationTrackerSqsName.statoEmailName(), notificationTrackerSqsName.statoEmailErratoName()),
                Arguments.of(PEC_REQUEST_IDX, transactionProcessConfigurationProperties.pec(), notificationTrackerSqsName.statoPecName(), notificationTrackerSqsName.statoPecErratoName()));
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void digitalHandleRequestStatusChangeOk(String requestId, String processId, String statoQueueName, String statoDlqQueueName) {

        //GIVEN
        PresaInCaricoInfo presaInCaricoInfo = PresaInCaricoInfo.builder().requestIdx(requestId).xPagopaExtchCxId(CLIENT_ID).build();
        DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto().status(BOOKED.getStatusTransactionTableCompliant()).generatedMessage(new GeneratedMessageDto().id("id").system("system").location("location"));
        NotificationTrackerQueueDto notificationTrackerQueueDto = NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital(presaInCaricoInfo, SENT.getStatusTransactionTableCompliant(), digitalProgressStatusDto);

        //WHEN
        when(callMacchinaStati.statusValidation(anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.just(new MacchinaStatiValidateStatoResponseDto()));

        //THEN
        switch (processId) {
            case "SMS" ->
                    notificationTrackerMessageReceiver.receiveSMSObjectMessage(notificationTrackerQueueDto, acknowledgment);
            case "EMAIL" ->
                    notificationTrackerMessageReceiver.receiveEmailObjectMessage(notificationTrackerQueueDto, acknowledgment);
            case "PEC" ->
                    notificationTrackerMessageReceiver.receivePecObjectMessage(notificationTrackerQueueDto, acknowledgment);
        }

        verify(notificationTrackerService, times(1)).handleRequestStatusChange(eq(notificationTrackerQueueDto), eq(processId), eq(statoQueueName), eq(statoDlqQueueName), eq(acknowledgment));
        verify(putEvents, times(1)).putEventExternal(any(SingleStatusUpdate.class), eq(processId));

    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void digitalHandleRequestStatusChangeKo(String requestId, String processId, String statoQueueName, String statoDlqQueueName) {

        //GIVEN
        PresaInCaricoInfo presaInCaricoInfo = PresaInCaricoInfo.builder().requestIdx(requestId).xPagopaExtchCxId(CLIENT_ID).build();
        DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto().status(BOOKED.getStatusTransactionTableCompliant()).generatedMessage(new GeneratedMessageDto().id("id").system("system").location("location"));
        NotificationTrackerQueueDto notificationTrackerQueueDto = NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital(presaInCaricoInfo, SENT.getStatusTransactionTableCompliant(), digitalProgressStatusDto);

        //WHEN
        when(callMacchinaStati.statusValidation(anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.error(new InvalidNextStatusException("", "", "", "")));

        //THEN
        switch (processId) {
            case "SMS" ->
                    notificationTrackerMessageReceiver.receiveSMSObjectMessage(notificationTrackerQueueDto, acknowledgment);
            case "EMAIL" ->
                    notificationTrackerMessageReceiver.receiveEmailObjectMessage(notificationTrackerQueueDto, acknowledgment);
            case "PEC" ->
                    notificationTrackerMessageReceiver.receivePecObjectMessage(notificationTrackerQueueDto, acknowledgment);
        }

        verify(notificationTrackerService, times(1)).handleRequestStatusChange(eq(notificationTrackerQueueDto), eq(processId), eq(statoQueueName), eq(statoDlqQueueName), eq(acknowledgment));
        verify(sqsService, times(1)).send(any(), any());

    }

    @Test
    void paperHandleRequestStatusChangeOk() {

        //GIVEN
        PresaInCaricoInfo presaInCaricoInfo = PresaInCaricoInfo.builder().requestIdx(PAPER_REQUEST_IDX).xPagopaExtchCxId(CLIENT_ID).build();
        PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto().status(BOOKED.getStatusTransactionTableCompliant());
        NotificationTrackerQueueDto notificationTrackerQueueDto = NotificationTrackerQueueDto.createNotificationTrackerQueueDtoPaper(presaInCaricoInfo, SENT.getStatusTransactionTableCompliant(), paperProgressStatusDto);

        //WHEN
        when(callMacchinaStati.statusValidation(anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.just(new MacchinaStatiValidateStatoResponseDto()));

        //THEN
        notificationTrackerMessageReceiver.receiveCartaceoObjectMessage(notificationTrackerQueueDto, acknowledgment);
        verify(notificationTrackerService, times(1)).handleRequestStatusChange(eq(notificationTrackerQueueDto), eq(transactionProcessConfigurationProperties.paper()), eq(notificationTrackerSqsName.statoCartaceoName()), eq(notificationTrackerSqsName.statoCartaceoErratoName()), eq(acknowledgment));
        verify(putEvents, times(1)).putEventExternal(any(SingleStatusUpdate.class), eq(transactionProcessConfigurationProperties.paper()));

    }

    @Test
    void paperHandleRequestStatusChangeKo() {

        //GIVEN
        PresaInCaricoInfo presaInCaricoInfo = PresaInCaricoInfo.builder().requestIdx(PAPER_REQUEST_IDX).xPagopaExtchCxId(CLIENT_ID).build();
        PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto().status(BOOKED.getStatusTransactionTableCompliant());
        NotificationTrackerQueueDto notificationTrackerQueueDto = NotificationTrackerQueueDto.createNotificationTrackerQueueDtoPaper(presaInCaricoInfo, SENT.getStatusTransactionTableCompliant(), paperProgressStatusDto);

        //WHEN
        when(callMacchinaStati.statusValidation(anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.error(new InvalidNextStatusException("", "", "", "")));

        //THEN
        notificationTrackerMessageReceiver.receiveCartaceoObjectMessage(notificationTrackerQueueDto, acknowledgment);
        verify(notificationTrackerService, times(1)).handleRequestStatusChange(eq(notificationTrackerQueueDto), eq(transactionProcessConfigurationProperties.paper()), eq(notificationTrackerSqsName.statoCartaceoName()), eq(notificationTrackerSqsName.statoCartaceoErratoName()), eq(acknowledgment));
        verify(sqsService, times(1)).send(any(), any());
    }

    private static void insertSmsRequest() {
        var concatRequestId = CLIENT_ID + "~" + SMS_REQUEST_IDX;
        requestPersonalDynamoDbTable.putItem(requestBuilder -> requestBuilder.item(RequestPersonal.builder().requestId(concatRequestId).xPagopaExtchCxId(CLIENT_ID).digitalRequestPersonal(DigitalRequestPersonal.builder().build()).build()));
        requestMetadataDynamoDbTable.putItem(requestBuilder -> requestBuilder.item(RequestMetadata.builder().requestId(concatRequestId).xPagopaExtchCxId(CLIENT_ID).digitalRequestMetadata(DigitalRequestMetadata.builder().channel("SMS").build()).build()));
    }

    private static void insertEmailRequest() {
        var concatRequestId = CLIENT_ID + "~" + EMAIL_REQUEST_IDX;
        requestPersonalDynamoDbTable.putItem(requestBuilder -> requestBuilder.item(RequestPersonal.builder().requestId(concatRequestId).xPagopaExtchCxId(CLIENT_ID).digitalRequestPersonal(DigitalRequestPersonal.builder().build()).build()));
        requestMetadataDynamoDbTable.putItem(requestBuilder -> requestBuilder.item(RequestMetadata.builder().requestId(concatRequestId).xPagopaExtchCxId(CLIENT_ID).digitalRequestMetadata(DigitalRequestMetadata.builder().channel("EMAIL").build()).build()));
    }

    private static void insertPecRequest() {
        var concatRequestId = CLIENT_ID + "~" + PEC_REQUEST_IDX;
        requestPersonalDynamoDbTable.putItem(requestBuilder -> requestBuilder.item(RequestPersonal.builder().requestId(concatRequestId).xPagopaExtchCxId(CLIENT_ID).digitalRequestPersonal(DigitalRequestPersonal.builder().build()).build()));
        requestMetadataDynamoDbTable.putItem(requestBuilder -> requestBuilder.item(RequestMetadata.builder().requestId(concatRequestId).xPagopaExtchCxId(CLIENT_ID).digitalRequestMetadata(DigitalRequestMetadata.builder().channel("PEC").build()).build()));
    }

    private static void insertPaperRequest() {
        var concatRequestId = CLIENT_ID + "~" + PAPER_REQUEST_IDX;
        requestPersonalDynamoDbTable.putItem(requestBuilder -> requestBuilder.item(RequestPersonal.builder().requestId(concatRequestId).xPagopaExtchCxId(CLIENT_ID).paperRequestPersonal(PaperRequestPersonal.builder().build()).build()));
        requestMetadataDynamoDbTable.putItem(requestBuilder -> requestBuilder.item(RequestMetadata.builder().requestId(concatRequestId).xPagopaExtchCxId(CLIENT_ID).paperRequestMetadata(PaperRequestMetadata.builder().build()).build()));
    }

    private static void buildStateMachine() throws IOException, JSONException {
        BufferedReader br = null;
        br = new BufferedReader(new FileReader("src/test/resources/statemachine/StateMachines.json"));
        Object obj;
        String line;
        while ((line = br.readLine()) != null) {
            JSONObject object = new JSONObject(line);
            stateMachine.add(object);
        }
    }

}
