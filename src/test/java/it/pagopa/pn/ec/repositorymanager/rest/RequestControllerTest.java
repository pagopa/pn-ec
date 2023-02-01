package it.pagopa.pn.ec.repositorymanager.rest;

import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto.EventCodeEnum.C000;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto.StatusEnum.OK;
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

        var digitalEvent = new EventsDto();
        var digitalProgressStatusDto = new DigitalProgressStatusDto();
        digitalProgressStatusDto.setStatus(PROGRESS);
        digitalProgressStatusDto.setEventCode(C000);
        digitalEvent.setDigProgrStatus(digitalProgressStatusDto);

        var paperEvent = new EventsDto();
        var paperProgressStatusDto = new PaperProgressStatusDto();
        paperProgressStatusDto.setProductType("");
        paperProgressStatusDto.statusCode("");
        paperProgressStatusDto.statusDescription("");
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
        return Stream.of(Arguments.of(digitalRequest), Arguments.of(paperRequest));
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

	//test.101.1
	@ParameterizedTest
    @ValueSource(strings = {DEFAULT_ID_DIGITAL, DEFAULT_ID_PAPER})
	@Order(3)
	void readRequestTestSuccess(String id) {
		webClient.get()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(id))
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
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build("idNotExist"))
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	//test.102.1
	@Test
	@Order(5)
	void testUpdateSuccess() {

        var newEvent = new EventsDto();
        var newDigitalProgressStatusDto = new DigitalProgressStatusDto();
        newDigitalProgressStatusDto.setStatus(OK);
        newDigitalProgressStatusDto.setEventCode(C000);
        newEvent.setDigProgrStatus(newDigitalProgressStatusDto);

		webClient.patch()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(DEFAULT_ID_DIGITAL))
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.body(BodyInserters.fromValue(newEvent))
				.exchange()
				.expectStatus()
				.isOk();
	}

	//test.102.2
	@Test
	@Order(6)
	void testUpdateFailed() {

        var newEvent = new EventsDto();
        var newDigitalProgressStatusDto = new DigitalProgressStatusDto();
        newDigitalProgressStatusDto.setStatus(OK);
        newDigitalProgressStatusDto.setEventCode(C000);
        newEvent.setDigProgrStatus(newDigitalProgressStatusDto);

		webClient.patch()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build("idCheNonEsiste"))
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.body(BodyInserters.fromValue(newEvent))
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	//test.103.1
    @ParameterizedTest
    @ValueSource(strings = {DEFAULT_ID_DIGITAL, DEFAULT_ID_PAPER})
	@Order(7)
	void deleteRequestTestSuccess(String id) {
		webClient.delete()
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(id))
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
				.uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build("idCheNonEsiste"))
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	//test.100.2
	@Test
	@Order(99)
	void insertRequestTestFailed() {
		webClient.post()
				 .uri(BASE_PATH)
				 .accept(APPLICATION_JSON)
				 .contentType(APPLICATION_JSON)
				 .body(BodyInserters.fromValue(digitalRequest))
				 .exchange()
				 .expectStatus()
				 .isForbidden();
	}
}
