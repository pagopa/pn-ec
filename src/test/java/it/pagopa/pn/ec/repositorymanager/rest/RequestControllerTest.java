package it.pagopa.pn.ec.repositorymanager.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestPersonal;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Request;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.repositorymanager.constant.GestoreRepositoryDynamoDbTableName.*;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusMetadataDto.EventCodeEnum.C000;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.PEC;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestPersonalDto.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestStatus.BOOKED;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestStatus.RETRY;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
class RequestControllerTest {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private GestoreRepositoryEndpointProperties gestoreRepositoryEndpointProperties;

    private static final String DEFAULT_ID_DIGITAL = "DIGITAL";
    private static final String DEFAULT_ID_PAPER = "PAPER";
    private static final RequestDto digitalRequest = new RequestDto();
    private static final RequestDto paperRequest = new RequestDto();

//    private static DynamoDbTable<Request> dynamoDbTable;
    private static DynamoDbTable<RequestMetadata> dynamoDbTableMetadata;
    private static DynamoDbTable<RequestPersonal> dynamoDbTablePersonal;

    private static void initializeRequestDto() {
        digitalRequest.setRequestIdx(DEFAULT_ID_DIGITAL);
        digitalRequest.setClientRequestTimeStamp(OffsetDateTime.now());
        digitalRequest.setRequestTimeStamp(OffsetDateTime.now());

        var digitalRequestMetadataDto = new DigitalRequestMetadataDto();
        digitalRequestMetadataDto.setCorrelationId(DEFAULT_ID_DIGITAL);
        digitalRequestMetadataDto.setEventType("");
        digitalRequestMetadataDto.setTags(null);
        digitalRequestMetadataDto.setChannel(PEC);
        digitalRequestMetadataDto.setMessageContentType(PLAIN);
//        var digitalProgressStatusMetadataDto = new DigitalProgressStatusMetadataDto();
//        digitalProgressStatusMetadataDto.setEventTimestamp(OffsetDateTime.now());
//        digitalProgressStatusMetadataDto.setStatus(BOOKED);
//        digitalProgressStatusMetadataDto.setEventCode(C000);
//        digitalProgressStatusMetadataDto.setEventDetails("");
//        var generateMessageDto = new DigitalProgressStatusMetadataDtoGeneratedMessage();
//        generateMessageDto.setId("id");
//        generateMessageDto.setSystem("System");
//        generateMessageDto.setLocation("location");
//        digitalProgressStatusMetadataDto.setGeneratedMessage(generateMessageDto);
//        var eventsMetadataDto = new EventsMetadataDto();
//        eventsMetadataDto.setDigProgrStatusMetadata(digitalProgressStatusMetadataDto);
        var requestMetadataDto = new RequestMetadataDto();
        requestMetadataDto.setDigitalRequestMetadata(digitalRequestMetadataDto);
//        var eventsMetadataList = new ArrayList<EventsMetadataDto>();
//        eventsMetadataList.add(eventsMetadataDto);
//        requestMetadataDto.setEventsMetadataList(eventsMetadataList);
        digitalRequest.setRequestMetadata(requestMetadataDto);


        var digitalRequestPersonalDto = new DigitalRequestPersonalDto();
        digitalRequestPersonalDto.setQos(INTERACTIVE);
        digitalRequestPersonalDto.setReceiverDigitalAddress("");
        digitalRequestPersonalDto.setMessageText("");
        digitalRequestPersonalDto.setSenderDigitalAddress("");
        digitalRequestPersonalDto.setSubjectText("");
        var attachmentsList = new ArrayList<String>();
        String attachmentUrl = "/http://ss/prova.pdf";
        attachmentsList.add(attachmentUrl);
        digitalRequestPersonalDto.setAttachmentsUrls(attachmentsList);
        var requestPersonalDto = new RequestPersonalDto();
        requestPersonalDto.setDigitalRequestPersonal(digitalRequestPersonalDto);
        digitalRequest.setRequestPersonal(requestPersonalDto);

//        paperRequest.setClientRequestTimeStamp(OffsetDateTime.now());
//
//        var digitalEvent = new EventsDto();
//        var digitalProgressStatusDto = new DigitalProgressStatusDto();
//        digitalProgressStatusDto.setStatus(BOOKED);
//        digitalProgressStatusDto.setEventCode(C000);
//        digitalEvent.setDigProgrStatus(digitalProgressStatusDto);
//
//        var paperEvent = new EventsDto();
//        var paperProgressStatusDto = new PaperProgressStatusDto();
//        paperProgressStatusDto.setProductType("");
//        paperProgressStatusDto.statusCode("");
//        paperProgressStatusDto.statusDescription("");
//        paperEvent.setPaperProgrStatus(paperProgressStatusDto);
//
//
//        var digitalRequestDto = new DigitalRequestDto();
//        digitalRequestDto.setEventType("");
//        digitalRequestDto.setQos(INTERACTIVE);
//        digitalRequestDto.setReceiverDigitalAddress("");
//        digitalRequestDto.setMessageText("");
//        digitalRequestDto.setChannel(SMS);
//        digitalRequestDto.setSubjectText("");
//        digitalRequestDto.setMessageContentType(PLAIN);
//        digitalRequest.setDigitalReq(digitalRequestDto);
//        digitalRequest.setEvents(List.of(digitalEvent));
//
//        paperRequest.setRequestIdx(DEFAULT_ID_PAPER);
//        var paperRequestDto = new PaperRequestDto();
//        paperRequestDto.setProductType("");
//        var paperRequestDtoAttachments = new PaperRequestDtoAttachments();
//        paperRequestDtoAttachments.setUri("");
//        paperRequestDtoAttachments.setOrder(new BigDecimal(1));
//        paperRequestDtoAttachments.setDocumentType("");
//        paperRequestDtoAttachments.setSha256("");
//        paperRequestDto.setAttachments(List.of(paperRequestDtoAttachments));
//        paperRequestDto.setReceiverAddress("");
//        paperRequestDto.setPrintType("");
//        paperRequestDto.setReceiverName("");
//        paperRequestDto.setReceiverCity("");
//        paperRequestDto.setSenderName("");
//        paperRequestDto.setSenderAddress("");
//        paperRequestDto.senderCity("");
//        paperRequestDto.setSenderPr("");
//        paperRequest.setPaperReq(paperRequestDto);
//        paperRequest.setEvents(List.of(paperEvent));
    }

    private static void insertRequestPersonal(RequestPersonal requestPersonal) {
        requestPersonal.setRequestId(digitalRequest.getRequestIdx());
        requestPersonal.setClientRequestTimeStamp(digitalRequest.getClientRequestTimeStamp());
        dynamoDbTablePersonal.putItem(builder -> builder.item(requestPersonal));
    }

    private static void insertRequestMetadata(RequestMetadata requestMetadata) {
        requestMetadata.setRequestId(digitalRequest.getRequestIdx());
        requestMetadata.setClientRequestTimeStamp(digitalRequest.getClientRequestTimeStamp());
        dynamoDbTableMetadata.putItem(builder -> builder.item(requestMetadata));
    }

    @BeforeAll
    public static void insertDefaultClientConfiguration(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient, @Autowired ObjectMapper objectMapper) {
        dynamoDbTablePersonal = dynamoDbEnhancedClient.table(REQUEST_PERSONAL_TABLE_NAME, TableSchema.fromBean(RequestPersonal.class));
        dynamoDbTableMetadata = dynamoDbEnhancedClient.table(REQUEST_METADATA_TABLE_NAME, TableSchema.fromBean(RequestMetadata.class));

        initializeRequestDto();
        insertRequestPersonal(objectMapper.convertValue(digitalRequest.getRequestPersonal(), RequestPersonal.class));
        insertRequestMetadata(objectMapper.convertValue(digitalRequest.getRequestMetadata(), RequestMetadata.class));
    }

    @BeforeEach
    public void createDefaultClientConfigurationDto() {
        initializeRequestDto();
    }

    private static Stream<Arguments> provideDigitalAndPaperRequest() {
        return Stream.of(Arguments.of(digitalRequest, "newIdDigital"));
    }

    // test.100.1
    @ParameterizedTest
    @MethodSource("provideDigitalAndPaperRequest")
    void insertRequestTestSuccess(RequestDto requestDto, String newId) {

        requestDto.setRequestIdx(newId);

        webClient.post()
                 .uri(gestoreRepositoryEndpointProperties.postRequest())
                 .accept(APPLICATION_JSON)
                 .contentType(APPLICATION_JSON)
                 .body(BodyInserters.fromValue(requestDto))
                 .exchange()
                 .expectStatus()
                 .isOk();
    }

    //test.100.2
    @Test
    void insertRequestTestFailed() {
        webClient.post()
                 .uri(gestoreRepositoryEndpointProperties.postRequest())
                 .accept(APPLICATION_JSON)
                 .contentType(APPLICATION_JSON)
                 .body(BodyInserters.fromValue(digitalRequest))
                 .exchange()
                 .expectStatus()
                 .isForbidden();
    }

    //test.101.1
    @ParameterizedTest
    @ValueSource(strings = {DEFAULT_ID_DIGITAL})
    void readRequestTestSuccess(String id) {
        webClient.get()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.getRequest()).build(id))
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isOk()
                 .expectBody(RequestDto.class);
    }

    //test.101.2
    @Test
    void readRequestTestFailed() {
        webClient.get()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.getRequest()).build("idNotExist"))
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isBadRequest();
    }

//    //test.102.1
//    @Test
//    void testUpdateSuccess() {
//
//        var newEvent = new EventsDto();
//        var newDigitalProgressStatusDto = new DigitalProgressStatusDto();
//        newDigitalProgressStatusDto.setStatus(RETRY);
//        newDigitalProgressStatusDto.setEventCode(C000);
//        newEvent.setDigProgrStatus(newDigitalProgressStatusDto);
//
//        webClient.patch()
//                 .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(DEFAULT_ID_DIGITAL))
//                 .accept(APPLICATION_JSON)
//                 .contentType(APPLICATION_JSON)
//                 .body(BodyInserters.fromValue(newEvent))
//                 .exchange()
//                 .expectStatus()
//                 .isOk();
//    }
//
//    //test.102.2
//    @Test
//    void testUpdateFailed() {
//
//        var newEvent = new EventsDto();
//        var newDigitalProgressStatusDto = new DigitalProgressStatusDto();
//        newDigitalProgressStatusDto.setStatus(RETRY);
//        newDigitalProgressStatusDto.setEventCode(C000);
//        newEvent.setDigProgrStatus(newDigitalProgressStatusDto);
//
//        webClient.patch()
//                 .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build("idCheNonEsiste"))
//                 .accept(APPLICATION_JSON)
//                 .contentType(APPLICATION_JSON)
//                 .body(BodyInserters.fromValue(newEvent))
//                 .exchange()
//                 .expectStatus()
//                 .isBadRequest();
//    }

    //test.103.1
    @ParameterizedTest
    @MethodSource("provideDigitalAndPaperRequest")
    void deleteRequestTestSuccess(RequestDto requestDto, String idToDelete) {

        requestDto.setRequestIdx(idToDelete);
        insertRequestPersonal(objectMapper.convertValue(requestDto.getRequestPersonal(), RequestPersonal.class));
        insertRequestMetadata(objectMapper.convertValue(requestDto.getRequestMetadata(), RequestMetadata.class));

        webClient.delete()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.deleteRequest()).build(idToDelete))
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isOk();
    }

    //test.103.2
    @Test
    void deleteRequestTestFailed() {
        webClient.delete()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.deleteRequest()).build("idCheNonEsiste"))
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isBadRequest();
    }
}
