package it.pagopa.pn.ec.repositorymanager.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.repositorymanager.entity.RequestMetadata;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto.EventCodeEnum.C000;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestDto.ChannelEnum.SMS;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestDto.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestDto.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestStatus.BOOKED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
class RequestMetadataControllerTest {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    ObjectMapper objectMapper;

    private static final String BASE_PATH = "/gestoreRepository/requests";
    private static final String BASE_PATH_WITH_PARAM = String.format("%s/{requestIdx}", BASE_PATH);

    private static final String DEFAULT_ID_DIGITAL = "DIGITAL";
    private static final String DEFAULT_ID_PAPER = "PAPER";
    private static final RequestDto digitalRequest = new RequestDto();
    private static final RequestDto paperRequest = new RequestDto();

    private static DynamoDbTable<RequestMetadata> dynamoDbTable;

    private static void initializeRequestDto() {

        digitalRequest.setClientRequestTimeStamp(OffsetDateTime.now());
        paperRequest.setClientRequestTimeStamp(OffsetDateTime.now());

        var digitalEvent = new EventsDto();
        var digitalProgressStatusDto = new DigitalProgressStatusDto();
        digitalProgressStatusDto.setEventTimestamp(OffsetDateTime.now());
        digitalProgressStatusDto.setStatus(BOOKED);
        digitalProgressStatusDto.setEventCode(C000);
        digitalProgressStatusDto.setEventDetails("");
        var generatedMessage = new DigitalProgressStatusDtoGeneratedMessage();
        generatedMessage.setId("");
        generatedMessage.setSystem("");
        generatedMessage.setLocation("");
        digitalProgressStatusDto.setGeneratedMessage(generatedMessage);
        digitalEvent.setDigProgrStatus(digitalProgressStatusDto);

        var paperEvent = new EventsDto();
        var paperProgressStatusDto = new PaperProgressStatusDto();
        paperProgressStatusDto.setRegisteredLetterCode("");
        paperProgressStatusDto.setStatusCode("");
        paperProgressStatusDto.setStatusDescription("");
        paperProgressStatusDto.setStatusDateTime(OffsetDateTime.now());
        paperProgressStatusDto.setDeliveryFailureCause("");
        paperEvent.setPaperProgrStatus(paperProgressStatusDto);

//        todo
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

    // test.100.1
    @ParameterizedTest
    @MethodSource("provideDigitalAndPaperRequest")
    void insertRequestTestSuccess(RequestDto requestDto, String newId) {

        requestDto.setRequestIdx(newId);

        webClient.post()
                .uri(BASE_PATH)
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestDto))
                .exchange()
                .expectStatus()
                .isOk();
    }

}
