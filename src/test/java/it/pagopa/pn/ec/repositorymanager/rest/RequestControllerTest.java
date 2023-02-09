package it.pagopa.pn.ec.repositorymanager.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ec.GestoreRepositoryEndpointProperties;
import it.pagopa.pn.ec.repositorymanager.model.dto.DiscoveredAddressDto;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestPersonal;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto.EventCodeEnum.C000;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.PEC;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestPersonalDto.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestStatus.BOOKED;
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

    private static DynamoDbTable<RequestMetadata> dynamoDbTableMetadata;
    private static DynamoDbTable<RequestPersonal> dynamoDbTablePersonal;

    private static void initializeRequestDto() {
//        inizialize digitalRequestDto
        digitalRequest.setRequestIdx(DEFAULT_ID_DIGITAL);
        digitalRequest.setClientRequestTimeStamp(OffsetDateTime.now());
        digitalRequest.setRequestTimeStamp(OffsetDateTime.now());

        var digitalRequestMetadataDto = new DigitalRequestMetadataDto();
        digitalRequestMetadataDto.setCorrelationId(DEFAULT_ID_DIGITAL);
        digitalRequestMetadataDto.setEventType("");
        digitalRequestMetadataDto.setTags(null);
        digitalRequestMetadataDto.setChannel(PEC);
        digitalRequestMetadataDto.setMessageContentType(PLAIN);

        var requestMetadataDto1 = new RequestMetadataDto();
        requestMetadataDto1.setDigitalRequestMetadata(digitalRequestMetadataDto);

//        dto per l'update'

//        var digitalProgressStatusDto = new DigitalProgressStatusDto();
//        digitalProgressStatusDto.setEventTimestamp(OffsetDateTime.now());
//        digitalProgressStatusDto.setStatus(BOOKED);
//        digitalProgressStatusDto.setEventCode(C000);
//        digitalProgressStatusDto.setEventDetails("");
//        var generateMessageDto = new DigitalProgressStatusDtoGeneratedMessage();
//        generateMessageDto.setId("id");
//        generateMessageDto.setSystem("System");
//        generateMessageDto.setLocation("location");
//        digitalProgressStatusDto.setGeneratedMessage(generateMessageDto);
//        var eventsDto1 = new EventsDto();
//        eventsDto1.setDigProgrStatus(digitalProgressStatusDto);
//        var eventsList1 = new ArrayList<EventsDto>();
//        eventsList1.add(eventsDto1);
//        requestMetadataDto1.setEventsList(eventsList1);

        digitalRequest.setRequestMetadata(requestMetadataDto1);


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
        var requestPersonalDto1 = new RequestPersonalDto();
        requestPersonalDto1.setDigitalRequestPersonal(digitalRequestPersonalDto);
        digitalRequest.setRequestPersonal(requestPersonalDto1);

//        inizialize paperRequestDto
        paperRequest.setRequestIdx(DEFAULT_ID_PAPER);
        paperRequest.setClientRequestTimeStamp(OffsetDateTime.now());
        paperRequest.setRequestTimeStamp(OffsetDateTime.now());

        var paperRequestMetadataDto = new PaperRequestMetadataDto();
        paperRequestMetadataDto.setIun("iun");
        paperRequestMetadataDto.setRequestPaId("PagoPa");
        paperRequestMetadataDto.setProductType("product type");
        paperRequestMetadataDto.setPrintType("B/N");
        var vas = new HashMap<String,String>();
        paperRequestMetadataDto.setVas(vas);

        var requestMetadataDto2 = new RequestMetadataDto();
        requestMetadataDto2.setPaperRequestMetadata(paperRequestMetadataDto);

//      dto per l'update'

//        var paperProgressStatusDto = new PaperProgressStatusDto();
//        paperProgressStatusDto.setRegisteredLetterCode("");
//        paperProgressStatusDto.setStatusCode("");
//        paperProgressStatusDto.setStatusDescription("");
//        paperProgressStatusDto.setStatusDateTime(OffsetDateTime.now());
//        paperProgressStatusDto.setDeliveryFailureCause("");
//        var discoveredAddressDto = new DiscoveredAddressDto();
//        discoveredAddressDto.setName("");
//        discoveredAddressDto.setNameRow2("");
//        discoveredAddressDto.setAddress("");
//        discoveredAddressDto.setAddressRow2("");
//        discoveredAddressDto.setCap("");
//        discoveredAddressDto.setCity("");
//        discoveredAddressDto.setCity2("");
//        discoveredAddressDto.setPr("");
//        discoveredAddressDto.setCountry("");
//        var paperProgressStatusDtoAttachmentsList = new ArrayList<PaperProgressStatusDtoAttachments>();
//        var paperProgressStatusDtoAttachments = new PaperProgressStatusDtoAttachments();
//        paperProgressStatusDtoAttachmentsList.add(paperProgressStatusDtoAttachments);
//        paperProgressStatusDto.setAttachments(paperProgressStatusDtoAttachmentsList);
//        paperProgressStatusDto.setDiscoveredAddress(discoveredAddressDto);
//        var eventList2 = new ArrayList<EventsDto>();
//        var eventsDto2 = new EventsDto();
//        eventsDto2.setPaperProgrStatus(paperProgressStatusDto);
//        eventList2.add(eventsDto2);
//        requestMetadataDto2.setEventsList(eventList2);

        paperRequest.setRequestMetadata(requestMetadataDto2);

        var paperRequestPersonalDto = new PaperRequestPersonalDto();
        var attachments = new ArrayList<PaperRequestPersonalDtoAttachments>();
        var attachment = new PaperRequestPersonalDtoAttachments();
        attachment.setUri("");
        attachment.setOrder(new BigDecimal(1));
        attachment.setDocumentType("document type");
        attachment.setSha256("sha256");
        attachments.add(attachment);
        paperRequestPersonalDto.setAttachments(attachments);
        paperRequestPersonalDto.setReceiverName("");
        paperRequestPersonalDto.setReceiverNameRow2("");
        paperRequestPersonalDto.setReceiverAddress("");
        paperRequestPersonalDto.setReceiverAddressRow2("");
        paperRequestPersonalDto.setReceiverCap("");
        paperRequestPersonalDto.setReceiverCity("");
        paperRequestPersonalDto.setReceiverCity2("");
        paperRequestPersonalDto.setReceiverPr("");
        paperRequestPersonalDto.setReceiverCountry("");
        paperRequestPersonalDto.setReceiverFiscalCode("");
        paperRequestPersonalDto.setSenderName("");
        paperRequestPersonalDto.setSenderAddress("");
        paperRequestPersonalDto.setSenderCity("");
        paperRequestPersonalDto.setSenderPr("");
        paperRequestPersonalDto.setSenderDigitalAddress("");
        paperRequestPersonalDto.setArName("");
        paperRequestPersonalDto.setArAddress("");
        paperRequestPersonalDto.setArCap("");
        paperRequestPersonalDto.setArCity("");
        var requestPersonalDto2 = new RequestPersonalDto();
        requestPersonalDto2.setPaperRequestPersonal(paperRequestPersonalDto);
        paperRequest.setRequestPersonal(requestPersonalDto2);

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
//        dynamoDbTablePersonal = dynamoDbEnhancedClient.table(REQUEST_PERSONAL_TABLE_NAME, TableSchema.fromBean(RequestPersonal.class));
//        dynamoDbTableMetadata = dynamoDbEnhancedClient.table(REQUEST_METADATA_TABLE_NAME, TableSchema.fromBean(RequestMetadata.class));

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
