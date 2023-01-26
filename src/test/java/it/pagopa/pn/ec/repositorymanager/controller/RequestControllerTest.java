package it.pagopa.pn.ec.repositorymanager.controller;

import it.pagopa.pn.ec.repositorymanager.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RequestControllerTest {

	@Autowired
	private WebTestClient webClient;

	// test.100.1
	@Test
	@Order(1)
	void insertRequestTestSuccess() {

		RequestDto requestDto = new RequestDto();
			// RequestDto
			DigitalRequestDto digitalRequestDto = new DigitalRequestDto();
			PaperRequestDto paperRequestDto = new PaperRequestDto();
				// PaperRequestDto
				List<PaperEngageRequestAttachmentsDto> paperEngageRequestAttachmentsDtoList = new ArrayList<>();
        		PaperEngageRequestAttachmentsDto paperEngageRequestAttachmentsDto = new PaperEngageRequestAttachmentsDto();
			List<EventsDto> eventsDtoList = new ArrayList<>();
			EventsDto eventsDto = new EventsDto();
				// Events
				DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto();
					// DigitalProgressStatusDto
        			GeneratedMessageDto generatedMessageDto = new GeneratedMessageDto();
				PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto();
					// PaperProgressStatusDto
					List<PaperProgressStatusEventAttachmentsDto> paperProgressStatusEventAttachmentsDtoList = new ArrayList<>();
					PaperProgressStatusEventAttachmentsDto paperProgressStatusEventAttachmentsDto = new PaperProgressStatusEventAttachmentsDto();
					DiscoveredAddressDto discoveredAddressDto = new DiscoveredAddressDto();

		OffsetDateTime odt = OffsetDateTime.now();

		discoveredAddressDto.setName("name");
		discoveredAddressDto.setNameRow2("name row 2");
		discoveredAddressDto.setAddress("address");
		discoveredAddressDto.setAddressRow2("address row 2");
		discoveredAddressDto.setCap("cap");
		discoveredAddressDto.setCity("city");
		discoveredAddressDto.setCity2("city 2");
		discoveredAddressDto.setPr("pr");
		discoveredAddressDto.setCountry("country");

		paperProgressStatusEventAttachmentsDto.setId("id");
		paperProgressStatusEventAttachmentsDto.setDocumentType("document type");
		paperProgressStatusEventAttachmentsDto.setUri("http://uri");
		paperProgressStatusEventAttachmentsDto.setSha256("sha256");
		paperProgressStatusEventAttachmentsDto.setDate(odt);

		paperProgressStatusEventAttachmentsDtoList.add(paperProgressStatusEventAttachmentsDto);

		paperProgressStatusDto.setRegisteredLetterCode("registered letter code");
		paperProgressStatusDto.setStatusCode("sped");
		paperProgressStatusDto.setStatusDescription("ogg sped");
		paperProgressStatusDto.setStatusDateTime(odt);
		paperProgressStatusDto.setDeliveryFailureCause("");
		paperProgressStatusDto.setAttachments(paperProgressStatusEventAttachmentsDtoList);
		paperProgressStatusDto.setDiscoveredAddress(discoveredAddressDto);
		paperProgressStatusDto.setClientRequestTimeStamp(odt);

		generatedMessageDto.setSystem("");
		generatedMessageDto.setId("");
		generatedMessageDto.setLocation("");

		digitalProgressStatusDto.setTimestamp(null);
		digitalProgressStatusDto.setStatus("");
		digitalProgressStatusDto.setCode("");
		digitalProgressStatusDto.setDetails("");
		digitalProgressStatusDto.setGenMess(generatedMessageDto);

		eventsDto.setDigProgrStatus(digitalProgressStatusDto);
		eventsDto.setPaperProgrStatus(paperProgressStatusDto);

		eventsDtoList.add(eventsDto);

		paperEngageRequestAttachmentsDto.setUri("http://uri");
		paperEngageRequestAttachmentsDto.setOrder(new BigDecimal(1));
		paperEngageRequestAttachmentsDto.setDocumentType("document type");
		paperEngageRequestAttachmentsDto.setSha256("sha256");

		paperEngageRequestAttachmentsDtoList.add(paperEngageRequestAttachmentsDto);

		Map<String, String> vas = new HashMap<>();
		vas.put("c","v");

		paperRequestDto.setIun("iun");
		paperRequestDto.setRequestPaid("Request paid");
		paperRequestDto.setProductType("product type");
		paperRequestDto.setAttachments(paperEngageRequestAttachmentsDtoList);
		paperRequestDto.setPrintType("print type");
		paperRequestDto.setReceiverName("receiver name");
		paperRequestDto.setReceiverNameRow2("receiver name row 2");
		paperRequestDto.setReceiverAddress("receiver address");
		paperRequestDto.setReceiverAddressRow2("receiver address row 2");
		paperRequestDto.setReceiverCap("receiver cap");
		paperRequestDto.setReceiverCity("receiver city");
		paperRequestDto.setReceiverCity2("receiver city2");
		paperRequestDto.setReceiverPr("receiver pr");
		paperRequestDto.setReceiverCountry("receiver country");
		paperRequestDto.setReceiverFiscalCode("receiver fiscal code");
		paperRequestDto.setSenderName("sender name");
		paperRequestDto.setSenderAddress("sender address");
		paperRequestDto.setSenderCity("sender city");
		paperRequestDto.setSenderPr("sender pr");
		paperRequestDto.setSenderDigitalAddress("sender digital address");
		paperRequestDto.setArName("ar name");
		paperRequestDto.setArCap("ar cap");
		paperRequestDto.setArCity("ar city");
		paperRequestDto.setVas(vas);

		List<String> tagsList = new ArrayList<>();
		String tag = "";
		tagsList.add(tag);

		List<String> attList = new ArrayList<>();
		String att = "";
		attList.add(att);

		digitalRequestDto.setCorrelationId("");
		digitalRequestDto.setEventType("");
		digitalRequestDto.setQos("");
		digitalRequestDto.setTags(tagsList);
		digitalRequestDto.setClientRequestTimeStamp(null);
		digitalRequestDto.setReceiverDigitalAddress("");
		digitalRequestDto.setMessageText("");
		digitalRequestDto.setSenderDigitalAddress("");
		digitalRequestDto.setChannel("");
		digitalRequestDto.setSubjectText("");
		digitalRequestDto.setMessageContentType("");
		digitalRequestDto.setAttachmentsUrls(attList);

		requestDto.setRequestId("1");
		requestDto.setDigitalReq(digitalRequestDto);
		requestDto.setPaperReq(paperRequestDto);
		requestDto.setEvents(eventsDtoList);

		webClient.post()
				.uri("http://localhost:8080/requests")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.body(BodyInserters.fromValue(requestDto))
				.exchange()
				.expectStatus()
				.isOk();
	}
	
	//test.100.2
	@Test
	@Order(2)
	void insertRequestTestFailed() {
		RequestDto requestDto = new RequestDto();
		// RequestDto
		DigitalRequestDto digitalRequestDto = new DigitalRequestDto();
		PaperRequestDto paperRequestDto = new PaperRequestDto();
		// PaperRequestDto
		List<PaperEngageRequestAttachmentsDto> paperEngageRequestAttachmentsDtoList = new ArrayList<>();
		PaperEngageRequestAttachmentsDto paperEngageRequestAttachmentsDto = new PaperEngageRequestAttachmentsDto();
		List<EventsDto> eventsDtoList = new ArrayList<>();
		EventsDto eventsDto = new EventsDto();
		// Events
		DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto();
		// DigitalProgressStatusDto
		GeneratedMessageDto generatedMessageDto = new GeneratedMessageDto();
		PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto();
		// PaperProgressStatusDto
		List<PaperProgressStatusEventAttachmentsDto> paperProgressStatusEventAttachmentsDtoList = new ArrayList<>();
		PaperProgressStatusEventAttachmentsDto paperProgressStatusEventAttachmentsDto = new PaperProgressStatusEventAttachmentsDto();
		DiscoveredAddressDto discoveredAddressDto = new DiscoveredAddressDto();

		OffsetDateTime odt = OffsetDateTime.now();

		discoveredAddressDto.setName("name");
		discoveredAddressDto.setNameRow2("name row 2");
		discoveredAddressDto.setAddress("address");
		discoveredAddressDto.setAddressRow2("address row 2");
		discoveredAddressDto.setCap("cap");
		discoveredAddressDto.setCity("city");
		discoveredAddressDto.setCity2("city 2");
		discoveredAddressDto.setPr("pr");
		discoveredAddressDto.setCountry("country");

		paperProgressStatusEventAttachmentsDto.setId("id");
		paperProgressStatusEventAttachmentsDto.setDocumentType("document type");
		paperProgressStatusEventAttachmentsDto.setUri("http://uri");
		paperProgressStatusEventAttachmentsDto.setSha256("sha256");
		paperProgressStatusEventAttachmentsDto.setDate(odt);

		paperProgressStatusEventAttachmentsDtoList.add(paperProgressStatusEventAttachmentsDto);

		paperProgressStatusDto.setRegisteredLetterCode("registered letter code");
		paperProgressStatusDto.setStatusCode("status code");
		paperProgressStatusDto.setStatusDescription("status description");
		paperProgressStatusDto.setStatusDateTime(odt);
		paperProgressStatusDto.setDeliveryFailureCause("delivery failure cause");
		paperProgressStatusDto.setAttachments(paperProgressStatusEventAttachmentsDtoList);
		paperProgressStatusDto.setDiscoveredAddress(discoveredAddressDto);
		paperProgressStatusDto.setClientRequestTimeStamp(odt);

		generatedMessageDto.setSystem("system");
		generatedMessageDto.setId("id");
		generatedMessageDto.setLocation("location");

		digitalProgressStatusDto.setTimestamp(odt);
		digitalProgressStatusDto.setStatus("status");
		digitalProgressStatusDto.setCode("code");
		digitalProgressStatusDto.setDetails("details");
		digitalProgressStatusDto.setGenMess(generatedMessageDto);

		eventsDto.setDigProgrStatus(digitalProgressStatusDto);
		eventsDto.setPaperProgrStatus(paperProgressStatusDto);

		eventsDtoList.add(eventsDto);

		paperEngageRequestAttachmentsDto.setUri("http://uri");
		paperEngageRequestAttachmentsDto.setOrder(new BigDecimal(1));
		paperEngageRequestAttachmentsDto.setDocumentType("document type");
		paperEngageRequestAttachmentsDto.setSha256("sha256");

		paperEngageRequestAttachmentsDtoList.add(paperEngageRequestAttachmentsDto);

		Map<String, String> vas = new HashMap<>();
		vas.put("c","v");

		paperRequestDto.setIun("iun");
		paperRequestDto.setRequestPaid("Request paid");
		paperRequestDto.setProductType("product type");
		paperRequestDto.setAttachments(paperEngageRequestAttachmentsDtoList);
		paperRequestDto.setPrintType("print type");
		paperRequestDto.setReceiverName("receiver name");
		paperRequestDto.setReceiverNameRow2("receiver name row 2");
		paperRequestDto.setReceiverAddress("receiver address");
		paperRequestDto.setReceiverAddressRow2("receiver address row 2");
		paperRequestDto.setReceiverCap("receiver cap");
		paperRequestDto.setReceiverCity("receiver city");
		paperRequestDto.setReceiverCity2("receiver city2");
		paperRequestDto.setReceiverPr("receiver pr");
		paperRequestDto.setReceiverCountry("receiver country");
		paperRequestDto.setReceiverFiscalCode("receiver fiscal code");
		paperRequestDto.setSenderName("sender name");
		paperRequestDto.setSenderAddress("sender address");
		paperRequestDto.setSenderCity("sender city");
		paperRequestDto.setSenderPr("sender pr");
		paperRequestDto.setSenderDigitalAddress("sender digital address");
		paperRequestDto.setArName("ar name");
		paperRequestDto.setArCap("ar cap");
		paperRequestDto.setArCity("ar city");
		paperRequestDto.setVas(vas);

		List<String> tagsList = new ArrayList<>();
		String tag = "tag1";
		tagsList.add(tag);

		List<String> attList = new ArrayList<>();
		String att = "http://allegato.pdf";
		attList.add(att);

		digitalRequestDto.setCorrelationId("coll id");
		digitalRequestDto.setEventType("event type");
		digitalRequestDto.setQos("qos");
		digitalRequestDto.setTags(tagsList);
		digitalRequestDto.setClientRequestTimeStamp(odt);
		digitalRequestDto.setReceiverDigitalAddress("receiver digital address");
		digitalRequestDto.setMessageText("message text");
		digitalRequestDto.setSenderDigitalAddress("sender digital address");
		digitalRequestDto.setChannel("channel");
		digitalRequestDto.setSubjectText("subject text");
		digitalRequestDto.setMessageContentType("message content type");
		digitalRequestDto.setAttachmentsUrls(attList);

		requestDto.setRequestId("1");
		requestDto.setDigitalReq(digitalRequestDto);
		requestDto.setPaperReq(paperRequestDto);
		requestDto.setEvents(eventsDtoList);

		webClient.post()
				.uri("http://localhost:8080/requests")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.body(BodyInserters.fromValue(requestDto))
				.exchange()
				.expectStatus()
				.isForbidden();
	}

	//test.101.1
	@Test
	@Order(3)
	void readRequestTestSuccess() {
		webClient.get()
				.uri("http://localhost:8080/requests/1")
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(RequestDto.class);
	}

	//test.101.2
	@Test
	@Order(4)
	void readRequestTestFailed() {
		webClient.get()
				.uri("http://localhost:8080/requests/2")
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	//test.102.1
	@Test
	@Order(5)
	void testUpdateSuccess() {
		UpdatedEventDto updatedEventDto = new UpdatedEventDto();

		// Events
		DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto();
		// DigitalProgressStatusDto
		GeneratedMessageDto generatedMessageDto = new GeneratedMessageDto();
		PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto();
		// PaperProgressStatusDto
		List<PaperProgressStatusEventAttachmentsDto> paperProgressStatusEventAttachmentsDtoList = new ArrayList<>();
		PaperProgressStatusEventAttachmentsDto paperProgressStatusEventAttachmentsDto = new PaperProgressStatusEventAttachmentsDto();
		DiscoveredAddressDto discoveredAddressDto = new DiscoveredAddressDto();

		OffsetDateTime odt = OffsetDateTime.now();

		discoveredAddressDto.setName("name");
		discoveredAddressDto.setNameRow2("name row 2");
		discoveredAddressDto.setAddress("address");
		discoveredAddressDto.setAddressRow2("address row 2");
		discoveredAddressDto.setCap("cap");
		discoveredAddressDto.setCity("city");
		discoveredAddressDto.setCity2("city 2");
		discoveredAddressDto.setPr("pr");
		discoveredAddressDto.setCountry("country");

		paperProgressStatusEventAttachmentsDto.setId("id");
		paperProgressStatusEventAttachmentsDto.setDocumentType("document type");
		paperProgressStatusEventAttachmentsDto.setUri("http://uri");
		paperProgressStatusEventAttachmentsDto.setSha256("sha256");
		paperProgressStatusEventAttachmentsDto.setDate(odt);

		paperProgressStatusEventAttachmentsDtoList.add(paperProgressStatusEventAttachmentsDto);

		paperProgressStatusDto.setRegisteredLetterCode("registered letter code");
		paperProgressStatusDto.setStatusCode("stamp");
		paperProgressStatusDto.setStatusDescription("ogg stamp");
		paperProgressStatusDto.setStatusDateTime(odt);
		paperProgressStatusDto.setDeliveryFailureCause("");
		paperProgressStatusDto.setAttachments(paperProgressStatusEventAttachmentsDtoList);
		paperProgressStatusDto.setDiscoveredAddress(discoveredAddressDto);
		paperProgressStatusDto.setClientRequestTimeStamp(odt);

		generatedMessageDto.setSystem("");
		generatedMessageDto.setId("");
		generatedMessageDto.setLocation("");

		digitalProgressStatusDto.setTimestamp(null);
		digitalProgressStatusDto.setStatus("");
		digitalProgressStatusDto.setCode("");
		digitalProgressStatusDto.setDetails("");
		digitalProgressStatusDto.setGenMess(generatedMessageDto);

		updatedEventDto.setDigProgrStatus(digitalProgressStatusDto);
		updatedEventDto.setPaperProgrStatus(paperProgressStatusDto);

		webClient.patch()
				.uri("http://localhost:8080/requests/1")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.body(BodyInserters.fromValue(updatedEventDto))
				.exchange()
				.expectStatus()
				.isOk();
	}

	//test.102.2
	@Test
	@Order(6)
	void testUpdateFailed() {

		UpdatedEventDto updatedEventDto = new UpdatedEventDto();

		// Events
		DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto();
		// DigitalProgressStatusDto
		GeneratedMessageDto generatedMessageDto = new GeneratedMessageDto();
		PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto();
		// PaperProgressStatusDto
		List<PaperProgressStatusEventAttachmentsDto> paperProgressStatusEventAttachmentsDtoList = new ArrayList<>();
		PaperProgressStatusEventAttachmentsDto paperProgressStatusEventAttachmentsDto = new PaperProgressStatusEventAttachmentsDto();
		DiscoveredAddressDto discoveredAddressDto = new DiscoveredAddressDto();

		OffsetDateTime odt = OffsetDateTime.now();

		discoveredAddressDto.setName("name");
		discoveredAddressDto.setNameRow2("name row 2");
		discoveredAddressDto.setAddress("address");
		discoveredAddressDto.setAddressRow2("address row 2");
		discoveredAddressDto.setCap("cap");
		discoveredAddressDto.setCity("city");
		discoveredAddressDto.setCity2("city 2");
		discoveredAddressDto.setPr("pr");
		discoveredAddressDto.setCountry("country");

		paperProgressStatusEventAttachmentsDto.setId("id");
		paperProgressStatusEventAttachmentsDto.setDocumentType("document type");
		paperProgressStatusEventAttachmentsDto.setUri("http://uri");
		paperProgressStatusEventAttachmentsDto.setSha256("sha256");
		paperProgressStatusEventAttachmentsDto.setDate(odt);

		paperProgressStatusEventAttachmentsDtoList.add(paperProgressStatusEventAttachmentsDto);

		paperProgressStatusDto.setRegisteredLetterCode("registered letter code");
		paperProgressStatusDto.setStatusCode("stamp");
		paperProgressStatusDto.setStatusDescription("ogg stamp");
		paperProgressStatusDto.setStatusDateTime(odt);
		paperProgressStatusDto.setDeliveryFailureCause("");
		paperProgressStatusDto.setAttachments(paperProgressStatusEventAttachmentsDtoList);
		paperProgressStatusDto.setDiscoveredAddress(discoveredAddressDto);
		paperProgressStatusDto.setClientRequestTimeStamp(odt);

		generatedMessageDto.setSystem("");
		generatedMessageDto.setId("");
		generatedMessageDto.setLocation("");

		digitalProgressStatusDto.setTimestamp(null);
		digitalProgressStatusDto.setStatus("");
		digitalProgressStatusDto.setCode("");
		digitalProgressStatusDto.setDetails("");
		digitalProgressStatusDto.setGenMess(generatedMessageDto);

		updatedEventDto.setDigProgrStatus(digitalProgressStatusDto);
		updatedEventDto.setPaperProgrStatus(paperProgressStatusDto);

		webClient.patch()
				.uri("http://localhost:8080/requests/100")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.body(BodyInserters.fromValue(updatedEventDto))
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	//test.103.1
	@Test
	@Order(7)
	void deleteRequestTestSuccess() {
		webClient.delete()
				.uri("http://localhost:8080/requests/1")
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus()
				.isOk();
	}

	//test.103.2
	@Test
	@Order(8)
	void deleteRequestTestFailed() {
		webClient.delete()
				.uri("http://localhost:8080/requests/2")
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

}
