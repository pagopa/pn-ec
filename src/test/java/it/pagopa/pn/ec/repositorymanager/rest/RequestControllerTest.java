package it.pagopa.pn.ec.repositorymanager.rest;

import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto.EventCodeEnum.C000;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto.StatusEnum.PROGRESS;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestDto.ChannelEnum.SMS;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestDto.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestDto.QosEnum.INTERACTIVE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RequestControllerTest {

    private static final String BASE_PATH = "/gestoreRepository/requests";
    private static final String BASE_PATH_WITH_PARAM = String.format("%s/{requestIdx}", BASE_PATH);

    private static final String DEFAULT_ID_DIGITAL = "DIGITAL";
    private static final String DEFAULT_ID_PAPER = "PAPER";
    private static final RequestDto digitalRequest = new RequestDto();
    private static final RequestDto paperRequest = new RequestDto();

    @BeforeEach
    public void createClientConfigurationDto() {
        Date defaultDate = new Date();

        var digitalEvent = new EventsDto();
        var digitalProgressStatusDto = new DigitalProgressStatusDto();
        digitalProgressStatusDto.setEventTimestamp(defaultDate);
        digitalProgressStatusDto.setStatus(PROGRESS);
        digitalProgressStatusDto.setEventCode(C000);
        digitalEvent.setDigProgrStatus(digitalProgressStatusDto);

        var paperEvent = new EventsDto();
        var paperProgressStatusDto = new PaperProgressStatusDto();
        paperProgressStatusDto.setProductType("");
        paperProgressStatusDto.statusCode("");
        paperProgressStatusDto.statusDescription("");
        paperProgressStatusDto.setStatusDateTime(defaultDate);
        paperEvent.setPaperProgrStatus(paperProgressStatusDto);

        digitalRequest.setRequestIdx(DEFAULT_ID_DIGITAL);
        var digitalRequestDto = new DigitalRequestDto();
        digitalRequestDto.setEventType("");
        digitalRequestDto.setQos(INTERACTIVE);
        digitalRequestDto.setReceiverDigitalAddress("");
        digitalRequestDto.setMessageText("");
        digitalRequestDto.setChannel(SMS);
        digitalRequestDto.setSubjectText("");
        digitalRequestDto.setMessageContentType(PLAIN);
        digitalRequest.setDigitalReq(digitalRequestDto);
        digitalRequest.setEvents(List.of(digitalEvent));

        paperRequest.setRequestIdx(DEFAULT_ID_PAPER);
        var paperRequestDto = new PaperRequestDto();
        paperRequestDto.setProductType("");
        var paperRequestDtoAttachments = new PaperRequestDtoAttachments();
        paperRequestDtoAttachments.setUri("");
        paperRequestDtoAttachments.setOrder(new BigDecimal(1));
        paperRequestDtoAttachments.setDocumentType("");
        paperRequestDtoAttachments.setSha256("");
        paperRequestDto.setAttachments(List.of(paperRequestDtoAttachments));
        paperRequestDto.setReceiverAddress("");
        paperRequestDto.setPrintType("");
        paperRequestDto.setReceiverName("");
        paperRequestDto.setReceiverCity("");
        paperRequestDto.setSenderName("");
        paperRequestDto.setSenderAddress("");
        paperRequestDto.senderCity("");
        paperRequestDto.setSenderPr("");
        paperRequest.setPaperReq(paperRequestDto);
        paperRequest.setEvents(List.of(paperEvent));
    }

    @Autowired
    private WebTestClient webClient;

    private static Stream<Arguments> provideDigitalAndPaperRequest() {
        return Stream.of(Arguments.of(digitalRequest, paperRequest), Arguments.of(paperRequest));
    }

    // test.100.1
    @ParameterizedTest
    @MethodSource("provideDigitalAndPaperRequest")
    @Order(1)
    void insertRequestTestSuccess(RequestDto requestDto) {
        webClient.post()
                 .uri(BASE_PATH)
                 .accept(APPLICATION_JSON)
                 .contentType(APPLICATION_JSON)
                 .body(BodyInserters.fromValue(requestDto))
                 .exchange()
                 .expectStatus()
                 .isOk();
    }
//
//	//test.100.2
//	@Test
//	@Order(2)
//	void insertRequestTestFailed() {
//		RequestDto requestDto = new RequestDto();
//		// RequestDto
//		DigitalRequestDto digitalRequestDto = new DigitalRequestDto();
//		PaperRequestDto paperRequestDto = new PaperRequestDto();
//		// PaperRequestDto
//		List<PaperEngageRequestAttachmentsDto> paperEngageRequestAttachmentsDtoList = new ArrayList<>();
//		PaperEngageRequestAttachmentsDto paperEngageRequestAttachmentsDto = new PaperEngageRequestAttachmentsDto();
//		List<EventsDto> eventsDtoList = new ArrayList<>();
//		EventsDto eventsDto = new EventsDto();
//		// Events
//		DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto();
//		// DigitalProgressStatusDto
//		GeneratedMessageDto generatedMessageDto = new GeneratedMessageDto();
//		PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto();
//		// PaperProgressStatusDto
//		List<PaperProgressStatusEventAttachmentsDto> paperProgressStatusEventAttachmentsDtoList = new ArrayList<>();
//		PaperProgressStatusEventAttachmentsDto paperProgressStatusEventAttachmentsDto = new PaperProgressStatusEventAttachmentsDto();
//		DiscoveredAddressDto discoveredAddressDto = new DiscoveredAddressDto();
//
//		OffsetDateTime odt = OffsetDateTime.now();
//
//		discoveredAddressDto.setName("name");
//		discoveredAddressDto.setNameRow2("name row 2");
//		discoveredAddressDto.setAddress("address");
//		discoveredAddressDto.setAddressRow2("address row 2");
//		discoveredAddressDto.setCap("cap");
//		discoveredAddressDto.setCity("city");
//		discoveredAddressDto.setCity2("city 2");
//		discoveredAddressDto.setPr("pr");
//		discoveredAddressDto.setCountry("country");
//
//		paperProgressStatusEventAttachmentsDto.setId("id");
//		paperProgressStatusEventAttachmentsDto.setDocumentType("document type");
//		paperProgressStatusEventAttachmentsDto.setUri("http://uri");
//		paperProgressStatusEventAttachmentsDto.setSha256("sha256");
//		paperProgressStatusEventAttachmentsDto.setDate(odt);
//
//		paperProgressStatusEventAttachmentsDtoList.add(paperProgressStatusEventAttachmentsDto);
//
//		paperProgressStatusDto.setRegisteredLetterCode("registered letter code");
//		paperProgressStatusDto.setStatusCode("status code");
//		paperProgressStatusDto.setStatusDescription("status description");
//		paperProgressStatusDto.setStatusDateTime(odt);
//		paperProgressStatusDto.setDeliveryFailureCause("delivery failure cause");
//		paperProgressStatusDto.setAttachments(paperProgressStatusEventAttachmentsDtoList);
//		paperProgressStatusDto.setDiscoveredAddress(discoveredAddressDto);
//		paperProgressStatusDto.setClientRequestTimeStamp(odt);
//
//		generatedMessageDto.setSystem("system");
//		generatedMessageDto.setId("id");
//		generatedMessageDto.setLocation("location");
//
//		digitalProgressStatusDto.setTimestamp(odt);
//		digitalProgressStatusDto.setStatus("status");
//		digitalProgressStatusDto.setCode("code");
//		digitalProgressStatusDto.setDetails("details");
//		digitalProgressStatusDto.setGenMess(generatedMessageDto);
//
//		eventsDto.setDigProgrStatus(digitalProgressStatusDto);
//		eventsDto.setPaperProgrStatus(paperProgressStatusDto);
//
//		eventsDtoList.add(eventsDto);
//
//		paperEngageRequestAttachmentsDto.setUri("http://uri");
//		paperEngageRequestAttachmentsDto.setOrder(new BigDecimal(1));
//		paperEngageRequestAttachmentsDto.setDocumentType("document type");
//		paperEngageRequestAttachmentsDto.setSha256("sha256");
//
//		paperEngageRequestAttachmentsDtoList.add(paperEngageRequestAttachmentsDto);
//
//		Map<String, String> vas = new HashMap<>();
//		vas.put("c","v");
//
//		paperRequestDto.setIun("iun");
//		paperRequestDto.setRequestPaid("Request paid");
//		paperRequestDto.setProductType("product type");
//		paperRequestDto.setAttachments(paperEngageRequestAttachmentsDtoList);
//		paperRequestDto.setPrintType("print type");
//		paperRequestDto.setReceiverName("receiver name");
//		paperRequestDto.setReceiverNameRow2("receiver name row 2");
//		paperRequestDto.setReceiverAddress("receiver address");
//		paperRequestDto.setReceiverAddressRow2("receiver address row 2");
//		paperRequestDto.setReceiverCap("receiver cap");
//		paperRequestDto.setReceiverCity("receiver city");
//		paperRequestDto.setReceiverCity2("receiver city2");
//		paperRequestDto.setReceiverPr("receiver pr");
//		paperRequestDto.setReceiverCountry("receiver country");
//		paperRequestDto.setReceiverFiscalCode("receiver fiscal code");
//		paperRequestDto.setSenderName("sender name");
//		paperRequestDto.setSenderAddress("sender address");
//		paperRequestDto.setSenderCity("sender city");
//		paperRequestDto.setSenderPr("sender pr");
//		paperRequestDto.setSenderDigitalAddress("sender digital address");
//		paperRequestDto.setArName("ar name");
//		paperRequestDto.setArCap("ar cap");
//		paperRequestDto.setArCity("ar city");
//		paperRequestDto.setVas(vas);
//
//		List<String> tagsList = new ArrayList<>();
//		String tag = "tag1";
//		tagsList.add(tag);
//
//		List<String> attList = new ArrayList<>();
//		String att = "http://allegato.pdf";
//		attList.add(att);
//
//		digitalRequestDto.setCorrelationId("coll id");
//		digitalRequestDto.setEventType("event type");
//		digitalRequestDto.setQos("qos");
//		digitalRequestDto.setTags(tagsList);
//		digitalRequestDto.setClientRequestTimeStamp(odt);
//		digitalRequestDto.setReceiverDigitalAddress("receiver digital address");
//		digitalRequestDto.setMessageText("message text");
//		digitalRequestDto.setSenderDigitalAddress("sender digital address");
//		digitalRequestDto.setChannel("channel");
//		digitalRequestDto.setSubjectText("subject text");
//		digitalRequestDto.setMessageContentType("message content type");
//		digitalRequestDto.setAttachmentsUrls(attList);
//
//		requestDto.setRequestId("1");
//		requestDto.setDigitalReq(digitalRequestDto);
//		requestDto.setPaperReq(paperRequestDto);
//		requestDto.setEvents(eventsDtoList);
//
//		webClient.post()
//				.uri("http://localhost:8080/requests")
//				.accept(APPLICATION_JSON)
//				.contentType(APPLICATION_JSON)
//				.body(BodyInserters.fromValue(requestDto))
//				.exchange()
//				.expectStatus()
//				.isForbidden();
//	}
//
//	//test.101.1
//	@Test
//	@Order(3)
//	void readRequestTestSuccess() {
//		webClient.get()
//				.uri("http://localhost:8080/requests/1")
//				.accept(APPLICATION_JSON)
//				.exchange()
//				.expectStatus()
//				.isOk()
//				.expectBody(RequestDto.class);
//	}
//
//	//test.101.2
//	@Test
//	@Order(4)
//	void readRequestTestFailed() {
//		webClient.get()
//				.uri("http://localhost:8080/requests/2")
//				.accept(APPLICATION_JSON)
//				.exchange()
//				.expectStatus()
//				.isBadRequest();
//	}
//
//	//test.102.1
//	@Test
//	@Order(5)
//	void testUpdateSuccess() {
//		UpdatedEventDto updatedEventDto = new UpdatedEventDto();
//
//		// Events
//		DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto();
//		// DigitalProgressStatusDto
//		GeneratedMessageDto generatedMessageDto = new GeneratedMessageDto();
//		PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto();
//		// PaperProgressStatusDto
//		List<PaperProgressStatusEventAttachmentsDto> paperProgressStatusEventAttachmentsDtoList = new ArrayList<>();
//		PaperProgressStatusEventAttachmentsDto paperProgressStatusEventAttachmentsDto = new PaperProgressStatusEventAttachmentsDto();
//		DiscoveredAddressDto discoveredAddressDto = new DiscoveredAddressDto();
//
//		OffsetDateTime odt = OffsetDateTime.now();
//
//		discoveredAddressDto.setName("name");
//		discoveredAddressDto.setNameRow2("name row 2");
//		discoveredAddressDto.setAddress("address");
//		discoveredAddressDto.setAddressRow2("address row 2");
//		discoveredAddressDto.setCap("cap");
//		discoveredAddressDto.setCity("city");
//		discoveredAddressDto.setCity2("city 2");
//		discoveredAddressDto.setPr("pr");
//		discoveredAddressDto.setCountry("country");
//
//		paperProgressStatusEventAttachmentsDto.setId("id");
//		paperProgressStatusEventAttachmentsDto.setDocumentType("document type");
//		paperProgressStatusEventAttachmentsDto.setUri("http://uri");
//		paperProgressStatusEventAttachmentsDto.setSha256("sha256");
//		paperProgressStatusEventAttachmentsDto.setDate(odt);
//
//		paperProgressStatusEventAttachmentsDtoList.add(paperProgressStatusEventAttachmentsDto);
//
//		paperProgressStatusDto.setRegisteredLetterCode("registered letter code");
//		paperProgressStatusDto.setStatusCode("stamp");
//		paperProgressStatusDto.setStatusDescription("ogg stamp");
//		paperProgressStatusDto.setStatusDateTime(odt);
//		paperProgressStatusDto.setDeliveryFailureCause("");
//		paperProgressStatusDto.setAttachments(paperProgressStatusEventAttachmentsDtoList);
//		paperProgressStatusDto.setDiscoveredAddress(discoveredAddressDto);
//		paperProgressStatusDto.setClientRequestTimeStamp(odt);
//
//		generatedMessageDto.setSystem("");
//		generatedMessageDto.setId("");
//		generatedMessageDto.setLocation("");
//
//		digitalProgressStatusDto.setTimestamp(null);
//		digitalProgressStatusDto.setStatus("");
//		digitalProgressStatusDto.setCode("");
//		digitalProgressStatusDto.setDetails("");
//		digitalProgressStatusDto.setGenMess(generatedMessageDto);
//
//		updatedEventDto.setDigProgrStatus(digitalProgressStatusDto);
//		updatedEventDto.setPaperProgrStatus(paperProgressStatusDto);
//
//		webClient.patch()
//				.uri("http://localhost:8080/requests/1")
//				.accept(APPLICATION_JSON)
//				.contentType(APPLICATION_JSON)
//				.body(BodyInserters.fromValue(updatedEventDto))
//				.exchange()
//				.expectStatus()
//				.isOk();
//	}
//
//	//test.102.2
//	@Test
//	@Order(6)
//	void testUpdateFailed() {
//
//		UpdatedEventDto updatedEventDto = new UpdatedEventDto();
//
//		// Events
//		DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto();
//		// DigitalProgressStatusDto
//		GeneratedMessageDto generatedMessageDto = new GeneratedMessageDto();
//		PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto();
//		// PaperProgressStatusDto
//		List<PaperProgressStatusEventAttachmentsDto> paperProgressStatusEventAttachmentsDtoList = new ArrayList<>();
//		PaperProgressStatusEventAttachmentsDto paperProgressStatusEventAttachmentsDto = new PaperProgressStatusEventAttachmentsDto();
//		DiscoveredAddressDto discoveredAddressDto = new DiscoveredAddressDto();
//
//		OffsetDateTime odt = OffsetDateTime.now();
//
//		discoveredAddressDto.setName("name");
//		discoveredAddressDto.setNameRow2("name row 2");
//		discoveredAddressDto.setAddress("address");
//		discoveredAddressDto.setAddressRow2("address row 2");
//		discoveredAddressDto.setCap("cap");
//		discoveredAddressDto.setCity("city");
//		discoveredAddressDto.setCity2("city 2");
//		discoveredAddressDto.setPr("pr");
//		discoveredAddressDto.setCountry("country");
//
//		paperProgressStatusEventAttachmentsDto.setId("id");
//		paperProgressStatusEventAttachmentsDto.setDocumentType("document type");
//		paperProgressStatusEventAttachmentsDto.setUri("http://uri");
//		paperProgressStatusEventAttachmentsDto.setSha256("sha256");
//		paperProgressStatusEventAttachmentsDto.setDate(odt);
//
//		paperProgressStatusEventAttachmentsDtoList.add(paperProgressStatusEventAttachmentsDto);
//
//		paperProgressStatusDto.setRegisteredLetterCode("registered letter code");
//		paperProgressStatusDto.setStatusCode("stamp");
//		paperProgressStatusDto.setStatusDescription("ogg stamp");
//		paperProgressStatusDto.setStatusDateTime(odt);
//		paperProgressStatusDto.setDeliveryFailureCause("");
//		paperProgressStatusDto.setAttachments(paperProgressStatusEventAttachmentsDtoList);
//		paperProgressStatusDto.setDiscoveredAddress(discoveredAddressDto);
//		paperProgressStatusDto.setClientRequestTimeStamp(odt);
//
//		generatedMessageDto.setSystem("");
//		generatedMessageDto.setId("");
//		generatedMessageDto.setLocation("");
//
//		digitalProgressStatusDto.setTimestamp(null);
//		digitalProgressStatusDto.setStatus("");
//		digitalProgressStatusDto.setCode("");
//		digitalProgressStatusDto.setDetails("");
//		digitalProgressStatusDto.setGenMess(generatedMessageDto);
//
//		updatedEventDto.setDigProgrStatus(digitalProgressStatusDto);
//		updatedEventDto.setPaperProgrStatus(paperProgressStatusDto);
//
//		webClient.patch()
//				.uri("http://localhost:8080/requests/100")
//				.accept(APPLICATION_JSON)
//				.contentType(APPLICATION_JSON)
//				.body(BodyInserters.fromValue(updatedEventDto))
//				.exchange()
//				.expectStatus()
//				.isBadRequest();
//	}
//
//	//test.103.1
//	@Test
//	@Order(7)
//	void deleteRequestTestSuccess() {
//		webClient.delete()
//				.uri("http://localhost:8080/requests/1")
//				.accept(APPLICATION_JSON)
//				.exchange()
//				.expectStatus()
//				.isOk();
//	}
//
//	//test.103.2
//	@Test
//	@Order(8)
//	void deleteRequestTestFailed() {
//		webClient.delete()
//				.uri("http://localhost:8080/requests/2")
//				.accept(APPLICATION_JSON)
//				.exchange()
//				.expectStatus()
//				.isBadRequest();
//	}

}
