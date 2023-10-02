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
import it.pagopa.pn.ec.repositorymanager.model.entity.DiscoveredAddress;
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
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.annotation.DirtiesContext;
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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@SpringBootTestWebEnv
public class NotificationTrackerMessageReceiverTest {
    @Autowired
    private NotificationTrackerMessageReceiver notificationTrackerMessageReceiver;
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
    @SpyBean
    private NotificationTrackerService notificationTrackerService;
    @MockBean
    private CallMacchinaStati callMacchinaStati;
    @MockBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @MockBean
    Acknowledgment acknowledgment;
    @Autowired
    RestUtils restUtils;

    //REQUESTIDX
    private static final String SMS_REQUEST_IDX = "SMS_REQUEST_IDX";
    private static final String EMAIL_REQUEST_IDX = "EMAIL_REQUEST_IDX";
    private static final String PEC_REQUEST_IDX = "PEC_REQUEST_IDX";
    private static final String PAPER_REQUEST_IDX = "PAPER_REQUEST_IDX";
    private static final String CLIENT_ID = "CLIENT_ID";
    private static final List<JSONObject> stateMachine = new ArrayList<>();

    //DIGITAL CHANNELS INFO
    private static String SMS_PROCESS_ID;
    private static String SMS_STATO_QUEUE;
    private static String SMS_STATO_ERRATO_QUEUE;
    private static String EMAIL_PROCESS_ID;
    private static String EMAIL_STATO_QUEUE;
    private static String EMAIL_STATO_ERRATO_QUEUE;
    private static String PEC_PROCESS_ID;
    private static String PEC_STATO_QUEUE;
    private static String PEC_STATO_ERRATO_QUEUE;

    //DYNAMO TABLES
    private static DynamoDbTable<RequestPersonal> requestPersonalDynamoDbTable;
    private static DynamoDbTable<RequestMetadata> requestMetadataDynamoDbTable;
    private static OffsetDateTime NOW;

    @BeforeAll
    public static void initialize(@Autowired DynamoDbEnhancedClient dynamoDbTestEnhancedClient,
                                  @Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) throws IOException, JSONException {
        buildStateMachine();

        requestPersonalDynamoDbTable = dynamoDbTestEnhancedClient.table(gestoreRepositoryDynamoDbTableName.richiestePersonalName(),
                TableSchema.fromBean(RequestPersonal.class));
        requestMetadataDynamoDbTable = dynamoDbTestEnhancedClient.table(gestoreRepositoryDynamoDbTableName.richiesteMetadataName(),
                TableSchema.fromBean(RequestMetadata.class));

        NOW = OffsetDateTime.now();

        insertSmsRequest();
        insertEmailRequest();
        insertPecRequest();
        insertPaperRequest();
    }

    @BeforeAll
    public static void initializeDigitalInfo(@Autowired TransactionProcessConfigurationProperties transactionProcessConfigurationProperties, @Autowired NotificationTrackerSqsName notificationTrackerSqsName) {

        SMS_PROCESS_ID = transactionProcessConfigurationProperties.sms();
        SMS_STATO_QUEUE = notificationTrackerSqsName.statoSmsName();
        SMS_STATO_ERRATO_QUEUE = notificationTrackerSqsName.statoSmsErratoName();

        EMAIL_PROCESS_ID = transactionProcessConfigurationProperties.email();
        EMAIL_STATO_QUEUE = notificationTrackerSqsName.statoEmailName();
        EMAIL_STATO_ERRATO_QUEUE = notificationTrackerSqsName.statoEmailErratoName();

        PEC_PROCESS_ID = transactionProcessConfigurationProperties.pec();
        PEC_STATO_QUEUE = notificationTrackerSqsName.statoPecName();
        PEC_STATO_ERRATO_QUEUE = notificationTrackerSqsName.statoPecErratoName();

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

    }

    private void mockStatusDecode() {
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

    private static Stream<Arguments> provideArguments() {
        return Stream.of(Arguments.of(SMS_REQUEST_IDX, SMS_PROCESS_ID, SMS_STATO_QUEUE, SMS_STATO_ERRATO_QUEUE),
                Arguments.of(EMAIL_REQUEST_IDX, EMAIL_PROCESS_ID, EMAIL_STATO_QUEUE, EMAIL_STATO_ERRATO_QUEUE),
                Arguments.of(PEC_REQUEST_IDX, PEC_PROCESS_ID, PEC_STATO_QUEUE, PEC_STATO_ERRATO_QUEUE));
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void digitalNtOk(String requestId, String processId, String statoQueueName, String statoDlqQueueName) {

        //WHEN
        when(callMacchinaStati.statusValidation(anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.just(new MacchinaStatiValidateStatoResponseDto()));
        mockStatusDecode();

        //THEN
        NotificationTrackerQueueDto notificationTrackerQueueDto = receiveDigitalObjectMessage(requestId, processId);

        verify(notificationTrackerService, times(1)).handleRequestStatusChange(notificationTrackerQueueDto, processId, statoQueueName, statoDlqQueueName, acknowledgment);
        verify(gestoreRepositoryCall, times(1)).patchRichiestaEvent(anyString(), anyString(), any(EventsDto.class));
        verify(putEvents, times(1)).putEventExternal(any(SingleStatusUpdate.class), eq(processId));

    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void digitalNtFromErrorOk(String requestId, String processId, String statoQueueName, String statoDlqQueueName) {

        //WHEN
        when(callMacchinaStati.statusValidation(anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.just(new MacchinaStatiValidateStatoResponseDto()));
        mockStatusDecode();

        //GIVEN
        PresaInCaricoInfo presaInCaricoInfo = PresaInCaricoInfo.builder().requestIdx(requestId).xPagopaExtchCxId(CLIENT_ID).build();
        DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto().status(RETRY.getStatusTransactionTableCompliant()).generatedMessage(new GeneratedMessageDto().id("id").system("system").location("location"));
        NotificationTrackerQueueDto notificationTrackerQueueDto = NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital(presaInCaricoInfo, SENT.getStatusTransactionTableCompliant(), digitalProgressStatusDto);
        receiveDigitalObjectFromErrorQueue(requestId, processId, notificationTrackerQueueDto);

        //THEN
        verify(notificationTrackerService, times(1)).handleMessageFromErrorQueue(notificationTrackerQueueDto, statoQueueName, acknowledgment);
        verify(sqsService, times(1)).send(statoQueueName, notificationTrackerQueueDto);
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void digitalNtFromErrorKo(String requestId, String processId, String statoQueueName, String statoDlqQueueName) {

        //WHEN
        when(callMacchinaStati.statusValidation(anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.just(new MacchinaStatiValidateStatoResponseDto()));
        mockStatusDecode();

        //THEN
        PresaInCaricoInfo presaInCaricoInfo = PresaInCaricoInfo.builder().requestIdx(requestId).xPagopaExtchCxId(CLIENT_ID).build();
        DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto().status(RETRY.getStatusTransactionTableCompliant()).generatedMessage(new GeneratedMessageDto().id("id").system("system").location("location"));
        NotificationTrackerQueueDto notificationTrackerQueueDto = NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital(presaInCaricoInfo, SENT.getStatusTransactionTableCompliant(), digitalProgressStatusDto);
        notificationTrackerQueueDto.getDigitalProgressStatusDto().setEventTimestamp(NOW.minusDays(1));
        receiveDigitalObjectFromErrorQueue(requestId, processId, notificationTrackerQueueDto);

        verify(notificationTrackerService, times(1)).handleMessageFromErrorQueue(notificationTrackerQueueDto, statoQueueName, acknowledgment);
        verify(sqsService, times(0)).send(anyString(), any(NotificationTrackerQueueDto.class));
    }

    @Test
    void paperNtOk() {

        //GIVEN
        PresaInCaricoInfo presaInCaricoInfo = PresaInCaricoInfo.builder().requestIdx(PAPER_REQUEST_IDX).xPagopaExtchCxId(CLIENT_ID).build();
        PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto().status(RETRY.getStatusTransactionTableCompliant()).discoveredAddress(new DiscoveredAddressDto()).attachments(List.of(new AttachmentsProgressEventDto().id("id")));
        NotificationTrackerQueueDto notificationTrackerQueueDto = NotificationTrackerQueueDto.createNotificationTrackerQueueDtoPaper(presaInCaricoInfo, SENT.getStatusTransactionTableCompliant(), paperProgressStatusDto);

        //WHEN
        when(callMacchinaStati.statusValidation(anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.just(new MacchinaStatiValidateStatoResponseDto()));
        mockStatusDecode();

        //THEN
        notificationTrackerMessageReceiver.receiveCartaceoObjectMessage(notificationTrackerQueueDto, acknowledgment);
        verify(notificationTrackerService, times(1)).handleRequestStatusChange(notificationTrackerQueueDto, transactionProcessConfigurationProperties.paper(), notificationTrackerSqsName.statoCartaceoName(), notificationTrackerSqsName.statoCartaceoErratoName(), acknowledgment);
        verify(gestoreRepositoryCall, times(1)).patchRichiestaEvent(anyString(), anyString(), any(EventsDto.class));
        verify(putEvents, times(1)).putEventExternal(any(SingleStatusUpdate.class), eq(transactionProcessConfigurationProperties.paper()));

    }

    @Test
    void paperNtFromErrorOk() {

        //GIVEN
        PresaInCaricoInfo presaInCaricoInfo = PresaInCaricoInfo.builder().requestIdx(PAPER_REQUEST_IDX).xPagopaExtchCxId(CLIENT_ID).build();
        PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto().status(RETRY.getStatusTransactionTableCompliant());
        NotificationTrackerQueueDto notificationTrackerQueueDto = NotificationTrackerQueueDto.createNotificationTrackerQueueDtoPaper(presaInCaricoInfo, SENT.getStatusTransactionTableCompliant(), paperProgressStatusDto);

        //WHEN
        when(callMacchinaStati.statusValidation(anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.just(new MacchinaStatiValidateStatoResponseDto()));
        mockStatusDecode();

        //THEN
        notificationTrackerMessageReceiver.receiveCartaceoObjectFromErrorQueue(notificationTrackerQueueDto, acknowledgment);
        verify(notificationTrackerService, times(1)).handleMessageFromErrorQueue(notificationTrackerQueueDto, notificationTrackerSqsName.statoCartaceoName(), acknowledgment);
        verify(sqsService, times(1)).send(notificationTrackerSqsName.statoCartaceoName(), notificationTrackerQueueDto);
    }

    @Test
    void paperNtFromErrorKo() {

        //GIVEN
        PresaInCaricoInfo presaInCaricoInfo = PresaInCaricoInfo.builder().requestIdx(PAPER_REQUEST_IDX).xPagopaExtchCxId(CLIENT_ID).build();
        PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto().status(RETRY.getStatusTransactionTableCompliant());
        NotificationTrackerQueueDto notificationTrackerQueueDto = NotificationTrackerQueueDto.createNotificationTrackerQueueDtoPaper(presaInCaricoInfo, SENT.getStatusTransactionTableCompliant(), paperProgressStatusDto);
        notificationTrackerQueueDto.getPaperProgressStatusDto().setStatusDateTime(NOW.minusDays(1));

        //WHEN
        when(callMacchinaStati.statusValidation(anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.just(new MacchinaStatiValidateStatoResponseDto()));
        mockStatusDecode();

        //THEN
        notificationTrackerMessageReceiver.receiveCartaceoObjectFromErrorQueue(notificationTrackerQueueDto, acknowledgment);
        verify(notificationTrackerService, times(1)).handleMessageFromErrorQueue(notificationTrackerQueueDto, notificationTrackerSqsName.statoCartaceoName(), acknowledgment);
        verify(sqsService, times(0)).send(anyString(), any(NotificationTrackerQueueDto.class));
    }

    @Test
    void ntNullLogicStatusOk() {

        //GIVEN
        PresaInCaricoInfo presaInCaricoInfo = PresaInCaricoInfo.builder().requestIdx(PAPER_REQUEST_IDX).xPagopaExtchCxId(CLIENT_ID).build();
        PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto().status(RETRY.getStatusTransactionTableCompliant());
        NotificationTrackerQueueDto notificationTrackerQueueDto = NotificationTrackerQueueDto.createNotificationTrackerQueueDtoPaper(presaInCaricoInfo, SENT.getStatusTransactionTableCompliant(), paperProgressStatusDto);

        //WHEN
        when(callMacchinaStati.statusValidation(anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.just(new MacchinaStatiValidateStatoResponseDto()));
        when(callMacchinaStati.statusDecode(anyString(), anyString(), anyString())).thenReturn(Mono.just(MacchinaStatiDecodeResponseDto.builder().externalStatus("EXT_STATUS").build()));


        //THEN
        notificationTrackerMessageReceiver.receiveCartaceoObjectMessage(notificationTrackerQueueDto, acknowledgment);
        verify(notificationTrackerService, times(1)).handleRequestStatusChange(notificationTrackerQueueDto, transactionProcessConfigurationProperties.paper(), notificationTrackerSqsName.statoCartaceoName(), notificationTrackerSqsName.statoCartaceoErratoName(), acknowledgment);
        verify(gestoreRepositoryCall, times(1)).patchRichiestaEvent(anyString(), anyString(), any(EventsDto.class));
        verify(putEvents, times(0)).putEventExternal(any(SingleStatusUpdate.class), eq(transactionProcessConfigurationProperties.paper()));

    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    void ntStatusValidationKo(Integer retry) {

        //GIVEN
        PresaInCaricoInfo presaInCaricoInfo = PresaInCaricoInfo.builder().requestIdx(PAPER_REQUEST_IDX).xPagopaExtchCxId(CLIENT_ID).build();
        PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto().status(RETRY.getStatusTransactionTableCompliant());
        NotificationTrackerQueueDto notificationTrackerQueueDto = NotificationTrackerQueueDto.createNotificationTrackerQueueDtoPaper(presaInCaricoInfo, SENT.getStatusTransactionTableCompliant(), paperProgressStatusDto);
        notificationTrackerQueueDto.setRetry(retry);

        //WHEN
        when(callMacchinaStati.statusValidation(anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.error(new InvalidNextStatusException("", "", "", "")));
        mockStatusDecode();

        //THEN
        notificationTrackerMessageReceiver.receiveCartaceoObjectMessage(notificationTrackerQueueDto, acknowledgment);
        verify(notificationTrackerService, times(1)).handleRequestStatusChange(notificationTrackerQueueDto, transactionProcessConfigurationProperties.paper(), notificationTrackerSqsName.statoCartaceoName(), notificationTrackerSqsName.statoCartaceoErratoName(), acknowledgment);
        verify(sqsService, times(1)).send(anyString(), any(NotificationTrackerQueueDto.class));
    }

    private NotificationTrackerQueueDto receiveDigitalObjectMessage(String requestId, String processId) {
        //GIVEN
        PresaInCaricoInfo presaInCaricoInfo = PresaInCaricoInfo.builder().requestIdx(requestId).xPagopaExtchCxId(CLIENT_ID).build();
        DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto().status(RETRY.getStatusTransactionTableCompliant()).generatedMessage(new GeneratedMessageDto().id("id").system("system").location("location"));
        NotificationTrackerQueueDto notificationTrackerQueueDto = NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital(presaInCaricoInfo, SENT.getStatusTransactionTableCompliant(), digitalProgressStatusDto);

        //THEN
        switch (processId) {
            case "SMS" ->
                    notificationTrackerMessageReceiver.receiveSMSObjectMessage(notificationTrackerQueueDto, acknowledgment);
            case "EMAIL" ->
                    notificationTrackerMessageReceiver.receiveEmailObjectMessage(notificationTrackerQueueDto, acknowledgment);
            case "PEC" ->
                    notificationTrackerMessageReceiver.receivePecObjectMessage(notificationTrackerQueueDto, acknowledgment);
        }

        return notificationTrackerQueueDto;
    }

    private void receiveDigitalObjectFromErrorQueue(String requestId, String processId, NotificationTrackerQueueDto notificationTrackerQueueDto) {

        //THEN
        switch (processId) {
            case "SMS" ->
                    notificationTrackerMessageReceiver.receiveSMSObjectFromErrorQueue(notificationTrackerQueueDto, acknowledgment);
            case "EMAIL" ->
                    notificationTrackerMessageReceiver.receiveEmailObjectFromErrorQueue(notificationTrackerQueueDto, acknowledgment);
            case "PEC" ->
                    notificationTrackerMessageReceiver.receivePecObjectFromErrorQueue(notificationTrackerQueueDto, acknowledgment);
        }

    }

    private static void insertSmsRequest() {
        var concatRequestId = CLIENT_ID + "~" + SMS_REQUEST_IDX;
        requestPersonalDynamoDbTable.putItem(requestBuilder -> requestBuilder.item(RequestPersonal.builder().requestId(concatRequestId).xPagopaExtchCxId(CLIENT_ID).digitalRequestPersonal(DigitalRequestPersonal.builder().build()).build()));
        Events event = Events.builder().digProgrStatus(DigitalProgressStatus.builder().status(BOOKED.getStatusTransactionTableCompliant()).eventTimestamp(NOW).build()).build();
        requestMetadataDynamoDbTable.putItem(requestBuilder -> requestBuilder.item(RequestMetadata.builder().eventsList(List.of(event)).requestId(concatRequestId).xPagopaExtchCxId(CLIENT_ID).digitalRequestMetadata(DigitalRequestMetadata.builder().channel("SMS").build()).build()));
    }

    private static void insertEmailRequest() {
        var concatRequestId = CLIENT_ID + "~" + EMAIL_REQUEST_IDX;
        requestPersonalDynamoDbTable.putItem(requestBuilder -> requestBuilder.item(RequestPersonal.builder().requestId(concatRequestId).xPagopaExtchCxId(CLIENT_ID).digitalRequestPersonal(DigitalRequestPersonal.builder().build()).build()));
        Events event = Events.builder().digProgrStatus(DigitalProgressStatus.builder().status(BOOKED.getStatusTransactionTableCompliant()).eventTimestamp(NOW).build()).build();
        requestMetadataDynamoDbTable.putItem(requestBuilder -> requestBuilder.item(RequestMetadata.builder().eventsList(List.of(event)).requestId(concatRequestId).xPagopaExtchCxId(CLIENT_ID).digitalRequestMetadata(DigitalRequestMetadata.builder().channel("EMAIL").build()).build()));
    }

    private static void insertPecRequest() {
        var concatRequestId = CLIENT_ID + "~" + PEC_REQUEST_IDX;
        requestPersonalDynamoDbTable.putItem(requestBuilder -> requestBuilder.item(RequestPersonal.builder().requestId(concatRequestId).xPagopaExtchCxId(CLIENT_ID).digitalRequestPersonal(DigitalRequestPersonal.builder().build()).build()));
        Events event = Events.builder().digProgrStatus(DigitalProgressStatus.builder().status(BOOKED.getStatusTransactionTableCompliant()).eventTimestamp(NOW).build()).build();
        requestMetadataDynamoDbTable.putItem(requestBuilder -> requestBuilder.item(RequestMetadata.builder().eventsList(List.of(event)).requestId(concatRequestId).xPagopaExtchCxId(CLIENT_ID).digitalRequestMetadata(DigitalRequestMetadata.builder().channel("PEC").build()).build()));
    }

    private static void insertPaperRequest() {
        var concatRequestId = CLIENT_ID + "~" + PAPER_REQUEST_IDX;
        requestPersonalDynamoDbTable.putItem(requestBuilder -> requestBuilder.item(RequestPersonal.builder().requestId(concatRequestId).xPagopaExtchCxId(CLIENT_ID).paperRequestPersonal(PaperRequestPersonal.builder().build()).build()));
        Events event = Events.builder().paperProgrStatus(PaperProgressStatus.builder()
                .status(BOOKED.getStatusTransactionTableCompliant())
                .statusDateTime(NOW)
                // .attachments(List.of(new PaperProgressStatusEventAttachments()))
                .discoveredAddress(new DiscoveredAddress())
                .build()).build();
        requestMetadataDynamoDbTable.putItem(requestBuilder -> requestBuilder.item(RequestMetadata.builder().eventsList(List.of(event)).requestId(concatRequestId).xPagopaExtchCxId(CLIENT_ID).paperRequestMetadata(PaperRequestMetadata.builder().build()).build()));
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
