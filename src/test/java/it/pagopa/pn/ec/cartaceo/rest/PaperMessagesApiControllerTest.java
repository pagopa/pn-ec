package it.pagopa.pn.ec.cartaceo.rest;

import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.ClientNotAuthorizedException;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.AttachmentNotAvailableException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCallImpl;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;

import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.*;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;


@SpringBootTestWebEnv
@AutoConfigureWebTestClient
class PaperMessagesApiControllerTest {


    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CartaceoSqsQueueName cartaceoSqsQueueName;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @MockBean
    private GestoreRepositoryCallImpl gestoreRepositoryCall;

    @MockBean
    private AuthService authService;

    @SpyBean
    private SqsServiceImpl sqsService;

    @MockBean
    private FileCall uriBuilderCall;

    private static final String SEND_SMS_ENDPOINT = "/external-channel/v1/paper-deliveries-engagements"
            + "/{requestIdx}";

    private static final String DEFAULT_ID_PAPER = "PAPER";
    private static final String X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE = "CLIENT1";

    private static final PaperEngageRequest paperEngageRequest = new PaperEngageRequest();
    private static final RequestDto requestDto = new RequestDto();
    private static final String defaultAttachmentUrl = "safestorage://prova.pdf";
    @BeforeAll
    public static void createDigitalCourtesyCartaceoRequest() {

//      Mock an existing request. Set the requestIdx
        requestDto.setRequestIdx("requestIdx");
        requestDto.setRequestIdx(DEFAULT_ID_PAPER);
        requestDto.setxPagopaExtchCxId(X_PAGOPA_EXTERNALCHANNEL_CX_ID_VALUE);
        requestDto.setClientRequestTimeStamp(OffsetDateTime.now());
        requestDto.setRequestTimeStamp(OffsetDateTime.now());

        var paperRequestMetadataDto = new PaperRequestMetadataDto();
        paperRequestMetadataDto.setIun("iun");
        paperRequestMetadataDto.setRequestPaId("PagoPa");
        paperRequestMetadataDto.setProductType("product type");
        paperRequestMetadataDto.setPrintType("B/N");
        var vas = new HashMap<String,String>();
        paperRequestMetadataDto.setVas(vas);
        var requestMetadataDto2 = new RequestMetadataDto();
        requestMetadataDto2.setPaperRequestMetadata(paperRequestMetadataDto);
        requestDto.setRequestMetadata(requestMetadataDto2);

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
        requestDto.setRequestPersonal(requestPersonalDto2);
    }

    private WebTestClient.ResponseSpec sendCartaceoTestCall(
            BodyInserter<PaperEngageRequest, ReactiveHttpOutputMessage> bodyInserter, String requestIdx) {
        return this.webTestClient.put().uri(uriBuilder -> uriBuilder.path(SEND_SMS_ENDPOINT).build(requestIdx))
                .accept(APPLICATION_JSON).contentType(APPLICATION_JSON).body(bodyInserter)
                .header(ID_CLIENT_HEADER_NAME, DEFAULT_ID_CLIENT_HEADER_VALUE).exchange();
    }

    // Per il momento le chiamate tra i vari microservizi di EC sono mocckate per
    // evitare problemi di precondizioni nei vari ambienti


    @Test
    void sendCartaceoOk() {

        when(authService.clientAuth(anyString())).thenReturn(Mono.empty());
        when(gestoreRepositoryCall.getRichiesta(anyString()))
                .thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));
        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

        sendCartaceoTestCall(BodyInserters.fromValue(paperEngageRequest), DEFAULT_REQUEST_IDX).expectStatus().isOk();
    }

    @Test
    void sendCartaceoBadBody() {
        sendCartaceoTestCall(BodyInserters.empty(), DEFAULT_REQUEST_IDX).expectStatus().isBadRequest()
                .expectBody(Problem.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { BAD_REQUEST_IDX_SHORT, BAD_REQUEST_IDX_CHAR_NOT_ALLOWED })
    void sendCartaceoMalformedIdClient(String badRequestIdx) {
        sendCartaceoTestCall(BodyInserters.fromValue(paperEngageRequest), badRequestIdx).expectStatus().isBadRequest()
                .expectBody(Problem.class);
    }


    // client -> KO
    @Test
    void callForClientAuthKo() {

//      Client auth call -> KO
        when(authService.clientAuth(anyString())).thenThrow(EcInternalEndpointHttpException.class);

        sendCartaceoTestCall(BodyInserters.fromValue(paperEngageRequest), DEFAULT_REQUEST_IDX).expectStatus()
                .isEqualTo(SERVICE_UNAVAILABLE).expectBody(Problem.class);
    }


    @Test
    void sendCartaceoUnauthorizedIdClient() {

//      Client auth call -> OK
//      Client non tornato dall'anagrafica client
        when(authService.clientAuth(anyString()))
                .thenReturn(Mono.error(new ClientNotAuthorizedException(DEFAULT_ID_CLIENT_HEADER_VALUE)));

        when(gestoreRepositoryCall.getRichiesta(anyString()))
                .thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));

        sendCartaceoTestCall(BodyInserters.fromValue(paperEngageRequest), DEFAULT_REQUEST_IDX).expectStatus()
                .isForbidden();
    }


    // corrente -> KO
    @Test
    void callToRetrieveCurrentStatusKo() {

//      Client auth call -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.empty());

//      Retrieve request -> KO
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenThrow(EcInternalEndpointHttpException.class);

        sendCartaceoTestCall(BodyInserters.fromValue(paperEngageRequest), DEFAULT_REQUEST_IDX).expectStatus()
                .isEqualTo(SERVICE_UNAVAILABLE).expectBody(Problem.class);
    }


    @Test
    void sendCartaceoRequestAlreadyMade() {

//      Client auth -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.empty());

//      Retrieve request -> Return an existent request, return 409 status
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.just(requestDto));

        sendCartaceoTestCall(BodyInserters.fromValue(paperEngageRequest), DEFAULT_REQUEST_IDX).expectStatus()
                .isEqualTo(CONFLICT).expectBody(Problem.class);
    }


    // KO
    @Test
    void sendCartaceoNotificationTrackerKo() {

//      Client auth -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.empty());

//      Retrieve request -> OK (If no request is found a RestCallException.ResourceNotFoundException is thrown)
        when(gestoreRepositoryCall.getRichiesta(anyString()))
                .thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));

        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

//      Mock dell'eccezione trhowata dalla pubblicazione sulla coda
        when(sqsService.send(eq(notificationTrackerSqsName.statoSmsName()), any(NotificationTrackerQueueDto.class)))
                .thenReturn(Mono.error(new SqsPublishException(notificationTrackerSqsName.statoSmsName())));

        sendCartaceoTestCall(BodyInserters.fromValue(paperEngageRequest), DEFAULT_REQUEST_IDX).expectStatus()
                .isEqualTo(SERVICE_UNAVAILABLE).expectBody(Problem.class);
    }


    @Test
    void sendCartaceoQueueKo() {

//      Client auth -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.empty());

//      Retrieve request -> OK (If no request is found a RestCallException.ResourceNotFoundException is thrown)
        when(gestoreRepositoryCall.getRichiesta(anyString()))
                .thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));

        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

//      Mock dell'eccezione trhowata dalla pubblicazione sulla coda
        when(sqsService.send(eq(cartaceoSqsQueueName.interactiveName()), any(CartaceoPresaInCaricoInfo.class)))
                .thenReturn(Mono.error(new SqsPublishException(cartaceoSqsQueueName.interactiveName())));

        sendCartaceoTestCall(BodyInserters.fromValue(paperEngageRequest), DEFAULT_REQUEST_IDX).expectStatus()
                .isEqualTo(SERVICE_UNAVAILABLE).expectBody(Problem.class);
    }


    @Test
    void sendCartaceoWithoutValidAttachment() {
        when(authService.clientAuth(anyString())).thenReturn(Mono.empty());

        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));

        when(uriBuilderCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.error(new AttachmentNotAvailableException(defaultAttachmentUrl)));

        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

        sendCartaceoTestCall(BodyInserters.fromValue(paperEngageRequest), DEFAULT_REQUEST_IDX).expectStatus()
                .isEqualTo(BAD_REQUEST)
                .expectBody(Problem.class);
    }

}