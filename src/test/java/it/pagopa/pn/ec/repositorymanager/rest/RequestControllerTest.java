package it.pagopa.pn.ec.repositorymanager.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ec.GestoreRepositoryEndpointProperties;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestPersonal;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.encodeMessageId;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.PEC;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestPersonalDto.QosEnum.INTERACTIVE;

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
    public static final String ID_CLIENT_HEADER_NAME = "x-pagopa-extch-cx-id";
    private static final String X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE = "CLIENT1";
    private static final String DEFAULT_CONCATE_ID_DIGITAL = "CLIENT1~DIGITAL";
    private static final String DEFAULT_CONCATE_ID_PAPER = "CLIENT1~PAPER";
    private static final RequestDto digitalRequest = new RequestDto();
    private static final RequestDto paperRequest = new RequestDto();

    private static DynamoDbTable<RequestMetadata> dynamoDbTableMetadata;
    private static DynamoDbTable<RequestPersonal> dynamoDbTablePersonal;

    private static void initializeRequestDto() {

//      Initialize digitalRequestDto
        digitalRequest.setRequestIdx(DEFAULT_ID_DIGITAL);
        digitalRequest.setxPagopaExtchCxId(X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE);
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

//      Initialize paperRequestDto
        paperRequest.setRequestIdx(DEFAULT_ID_PAPER);
        paperRequest.setxPagopaExtchCxId(X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE);
        paperRequest.setClientRequestTimeStamp(OffsetDateTime.now());
        paperRequest.setRequestTimeStamp(OffsetDateTime.now());

        var paperRequestMetadataDto = new PaperRequestMetadataDto();
        paperRequestMetadataDto.setIun("iun");
        paperRequestMetadataDto.setRequestPaId("PagoPa");
        paperRequestMetadataDto.setProductType("product type");
        paperRequestMetadataDto.setPrintType("B/N");
        var vas = new HashMap<String, String>();
        paperRequestMetadataDto.setVas(vas);
        var requestMetadataDto2 = new RequestMetadataDto();
        requestMetadataDto2.setPaperRequestMetadata(paperRequestMetadataDto);
        paperRequest.setRequestMetadata(requestMetadataDto2);

        var paperRequestPersonalDto = new PaperRequestPersonalDto();
        var attachments = new ArrayList<AttachmentsEngageRequestDto>();
        var attachment = new AttachmentsEngageRequestDto();
        attachment.setUri("");
        attachment.setOrder(new BigDecimal(1));
        attachment.setDocumentType("documentType");
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

    private static void insertRequestPersonal(String idRequest, RequestPersonal requestPersonal) {
        requestPersonal.setRequestId(idRequest);
        requestPersonal.setXPagopaExtchCxId(X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE);
        dynamoDbTablePersonal.putItem(builder -> builder.item(requestPersonal));
    }

    private static void insertRequestMetadata(String idRequest, RequestMetadata requestMetadata) {
        requestMetadata.setRequestId(idRequest);
        requestMetadata.setXPagopaExtchCxId(X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE);
        requestMetadata.setMessageId(encodeMessageId(idRequest));
        dynamoDbTableMetadata.putItem(builder -> builder.item(requestMetadata));
    }

    @BeforeAll
    public static void insertDefaultRequest(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient,
                                            @Autowired ObjectMapper objectMapper,
                                            @Autowired RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        dynamoDbTablePersonal = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.richiestePersonalName(),
                                                             TableSchema.fromBean(RequestPersonal.class));
        dynamoDbTableMetadata = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.richiesteMetadataName(),
                                                             TableSchema.fromBean(RequestMetadata.class));

        initializeRequestDto();
        insertRequestPersonal(DEFAULT_CONCATE_ID_DIGITAL,
                              objectMapper.convertValue(digitalRequest.getRequestPersonal(), RequestPersonal.class));
        insertRequestMetadata(DEFAULT_CONCATE_ID_DIGITAL,
                              objectMapper.convertValue(digitalRequest.getRequestMetadata(), RequestMetadata.class));
        insertRequestPersonal(DEFAULT_CONCATE_ID_PAPER,
                              objectMapper.convertValue(paperRequest.getRequestPersonal(), RequestPersonal.class));
        insertRequestMetadata(DEFAULT_CONCATE_ID_PAPER,
                              objectMapper.convertValue(paperRequest.getRequestMetadata(), RequestMetadata.class));
    }

    private static Stream<Arguments> provideDigitalAndPaperRequestToInsert() {
        return Stream.of(Arguments.of(digitalRequest, "idDigitalToInsert"), Arguments.of(paperRequest, "idPaperToInsert"));
    }

    // test.100.1
    @ParameterizedTest
    @MethodSource("provideDigitalAndPaperRequestToInsert")
    void insertRequestTestSuccess(RequestDto requestDto, String newId) {

        requestDto.setRequestIdx(newId);

        webClient.post().uri(gestoreRepositoryEndpointProperties.postRequest()).bodyValue(requestDto).exchange().expectStatus().isOk();
    }

    // test.100.2
    @Test
    void insertDigitalRequestAlreadyPresentTest() {

//      Set any field from which the hash is calculated to modify it
        digitalRequest.getRequestPersonal().getDigitalRequestPersonal().setMessageText("Different message text");

        webClient.post()
                 .uri(gestoreRepositoryEndpointProperties.postRequest())
                 .bodyValue(digitalRequest)
                 .exchange()
                 .expectStatus()
                 .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void insertDigitalRequestWithSameHashTest() {
        webClient.post()
                 .uri(gestoreRepositoryEndpointProperties.postRequest())
                 .bodyValue(digitalRequest)
                 .exchange()
                 .expectStatus()
                 .isEqualTo(HttpStatus.NO_CONTENT);
    }

    // test.101.1
    @ParameterizedTest
    @ValueSource(strings = {DEFAULT_ID_DIGITAL, DEFAULT_ID_PAPER})
    void readRequestTestSuccess(String id) {
        webClient.get()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.getRequest()).build(id))
                 .header(ID_CLIENT_HEADER_NAME, X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE)
                 .exchange()
                 .expectStatus()
                 .isOk()
                 .expectBody(RequestDto.class);
    }

    // test.101.2
    @Test
    void readRequestTestFailed() {
        webClient.get()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.getRequest()).build("idNotExist"))
                 .header(ID_CLIENT_HEADER_NAME, X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE)
                 .exchange()
                 .expectStatus()
                 .isNotFound();
    }

    private static Stream<Arguments> provideDigitalAndPaperEventToUpdate() {

        var patchDto1 = new PatchDto();
        var patchDto2 = new PatchDto();

        var digitalEventDto = new EventsDto();
        var paperEventDto = new EventsDto();

        var digitalProgressStatusDto = new DigitalProgressStatusDto();
        digitalProgressStatusDto.setEventTimestamp(OffsetDateTime.now());
        digitalProgressStatusDto.setStatus("booked");
        digitalProgressStatusDto.setStatusCode(null);
        digitalProgressStatusDto.setEventDetails("");
        var generateMessageDto = new GeneratedMessageDto();
        generateMessageDto.setId("id");
        generateMessageDto.setSystem("System");
        generateMessageDto.setLocation("location");
        digitalProgressStatusDto.setGeneratedMessage(generateMessageDto);
        digitalEventDto.setDigProgrStatus(digitalProgressStatusDto);

        patchDto1.setEvent(digitalEventDto);

        var paperProgressStatusDto = new PaperProgressStatusDto();
        paperProgressStatusDto.setRegisteredLetterCode("");
        paperProgressStatusDto.setStatusCode(null);
        paperProgressStatusDto.setStatus("printed");
        paperProgressStatusDto.setStatusDescription("printed");
        paperProgressStatusDto.setStatusDateTime(OffsetDateTime.now());
        paperProgressStatusDto.setDeliveryFailureCause("");
        var discoveredAddressDto = new DiscoveredAddressDto();
        discoveredAddressDto.setName("");
        discoveredAddressDto.setNameRow2("");
        discoveredAddressDto.setAddress("");
        discoveredAddressDto.setAddressRow2("");
        discoveredAddressDto.setCap("");
        discoveredAddressDto.setCity("");
        discoveredAddressDto.setCity2("");
        discoveredAddressDto.setPr("");
        discoveredAddressDto.setCountry("");
        var paperProgressStatusDtoAttachmentsList = new ArrayList<AttachmentsProgressEventDto>();
        var paperProgressStatusDtoAttachments = new AttachmentsProgressEventDto();
        paperProgressStatusDtoAttachments.setId("");
        paperProgressStatusDtoAttachments.setDocumentType("");
        paperProgressStatusDtoAttachments.setUri("");
        paperProgressStatusDtoAttachments.setSha256("");
        paperProgressStatusDtoAttachments.setDate(OffsetDateTime.now());
        paperProgressStatusDtoAttachmentsList.add(paperProgressStatusDtoAttachments);
        paperProgressStatusDto.setAttachments(paperProgressStatusDtoAttachmentsList);
        paperProgressStatusDto.setDiscoveredAddress(discoveredAddressDto);
        paperEventDto.setPaperProgrStatus(paperProgressStatusDto);

        patchDto2.setEvent(paperEventDto);

        return Stream.of(Arguments.of(patchDto1, DEFAULT_ID_DIGITAL), Arguments.of(patchDto2, DEFAULT_ID_PAPER));
    }

    // test.102.1
    @ParameterizedTest
    @MethodSource("provideDigitalAndPaperEventToUpdate")
    void testUpdateSuccess(PatchDto patchDto, String idRequest) {

        webClient.patch()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.patchRequest()).build(idRequest))
                 .header(ID_CLIENT_HEADER_NAME, X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE)
                 .body(BodyInserters.fromValue(patchDto))
                 .exchange()
                 .expectStatus()
                 .isOk();
    }

    // test.102.2
    @ParameterizedTest
    @MethodSource("provideDigitalAndPaperEventToUpdate")
    void testUpdateFailed(PatchDto patchDto) {

        webClient.patch()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.patchRequest()).build("idCheNonEsiste1"))
                 .header(ID_CLIENT_HEADER_NAME, X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE)
                 .body(BodyInserters.fromValue(patchDto))
                 .exchange()
                 .expectStatus()
                 .isNotFound();
    }

    //retry test
    @Test
    void retryTestSuccess() {

        var patchDto = new PatchDto();

        var retry = new RetryDto();
        retry.setLastRetryTimestamp(OffsetDateTime.now());
        retry.setRetryStep(new BigDecimal(1));

        List<BigDecimal> listPolicy = new ArrayList<>();
        listPolicy.add(new BigDecimal(40));

        retry.setRetryPolicy(listPolicy);

        patchDto.setRetry(retry);

        webClient.patch()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.patchRequest()).build(DEFAULT_ID_DIGITAL))
                 .header(ID_CLIENT_HEADER_NAME, X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE)
                 .body(BodyInserters.fromValue(patchDto))
                 .exchange()
                 .expectStatus()
                 .isOk();
    }

    private static Stream<Arguments> provideDigitalAndPaperRequestForDelete() {
        return Stream.of(Arguments.of(digitalRequest, "idDigitalToDelete"), Arguments.of(paperRequest, "idPaperToDelete"));
    }

    // test.103.1
    @ParameterizedTest
    @MethodSource("provideDigitalAndPaperRequestForDelete")
    void deleteRequestTestSuccess(RequestDto requestDto, String idToDelete) {

        insertRequestPersonal("CLIENT1~" + idToDelete, objectMapper.convertValue(requestDto.getRequestPersonal(), RequestPersonal.class));
        insertRequestMetadata("CLIENT1~" + idToDelete, objectMapper.convertValue(requestDto.getRequestMetadata(), RequestMetadata.class));

        webClient.delete()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.deleteRequest()).build(idToDelete))
                 .header(ID_CLIENT_HEADER_NAME, X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE)
                 .exchange()
                 .expectStatus()
                 .isOk();
    }

    // test.103.2
    @Test
    void deleteRequestTestFailed() {
        webClient.delete()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.deleteRequest()).build("idCheNonEsiste2"))
                 .header(ID_CLIENT_HEADER_NAME, X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE)
                 .exchange()
                 .expectStatus()
                 .isNotFound();
    }

    @ParameterizedTest
    @ValueSource(strings = {DEFAULT_CONCATE_ID_DIGITAL, DEFAULT_CONCATE_ID_PAPER})
    void getRequestByMessageIdOk(String idRequest) {
        webClient.get()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.getRequestByMessageId())
                                              .build(encodeMessageId(idRequest)))
                 .header(ID_CLIENT_HEADER_NAME, X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE)
                 .exchange()
                 .expectStatus()
                 .isOk();
    }

    @Test
    void getRequestByMessageNotFound() {
        webClient.get()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.getRequestByMessageId())
                                              .build(encodeMessageId(X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE + "~idRequestCheNonEsiste")))
                 .header(ID_CLIENT_HEADER_NAME, X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE)
                 .exchange()
                 .expectStatus()
                 .isNotFound();
    }

    @Test
    void getRequestByMessageBadMessageId() {
        webClient.get()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.getRequestByMessageId()).build("badMessageId"))
                 .exchange()
                 .expectStatus()
                 .isBadRequest();
    }

    @ParameterizedTest
    @ValueSource(strings = {DEFAULT_ID_DIGITAL, DEFAULT_ID_PAPER})
    void updateMessageIdInRequestMetadataOk(String idRequest) {
        webClient.post()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.setMessageIdInRequestMetadata()).build(idRequest))
                 .header(ID_CLIENT_HEADER_NAME, X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE)
                 .exchange()
                 .expectStatus()
                 .isOk();
    }

    @Test
    void updateMessageIdInRequestMetadataNotFound() {
        webClient.post()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.setMessageIdInRequestMetadata())
                                              .build("idRequestCheNonEsiste"))
                 .header(ID_CLIENT_HEADER_NAME, X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE)
                 .exchange()
                 .expectStatus()
                 .isNotFound();
    }
}
