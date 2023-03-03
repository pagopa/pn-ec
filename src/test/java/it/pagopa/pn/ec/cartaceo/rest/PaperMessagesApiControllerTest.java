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
import java.util.List;

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

    private static final String SEND_CARTACEO_ENDPOINT = "/external-channel/v1/paper-deliveries-engagements"
            + "/{requestIdx}";

    private static final ClientConfigurationDto clientConfigurationDto = new ClientConfigurationDto();
    private static final PaperEngageRequest paperEngageRequest = new PaperEngageRequest();
    private static final RequestDto requestDto = new RequestDto();

    private static  PaperEngageRequestAttachments paperEngageRequestAttachments = new PaperEngageRequestAttachments();
    private static final String defaultAttachmentUrl = "safestorage://prova.pdf";
    @BeforeAll
    public static void createDigitalCourtesyCartaceoRequest() {

//      Mock an existing request. Set the requestIdx
        requestDto.setRequestIdx("requestIdx");
        paperEngageRequestAttachments.setUri(defaultAttachmentUrl);
        paperEngageRequestAttachments.setOrder(BigDecimal.valueOf(1));
        paperEngageRequestAttachments.setDocumentType("TEST");
        paperEngageRequestAttachments.setSha256("stringstringstringstringstringstringstri");
        List<PaperEngageRequestAttachments> paperEngageRequestAttachmentsList = new ArrayList<>();
        paperEngageRequestAttachmentsList.add(paperEngageRequestAttachments);
        paperEngageRequest.setAttachments(paperEngageRequestAttachmentsList);
        paperEngageRequest.setReceiverName("");
        paperEngageRequest.setReceiverNameRow2("");
        paperEngageRequest.setReceiverAddress("");
        paperEngageRequest.setReceiverAddressRow2("");
        paperEngageRequest.setReceiverCap("");
        paperEngageRequest.setReceiverCity("");
        paperEngageRequest.setReceiverCity2("");
        paperEngageRequest.setReceiverPr("");
        paperEngageRequest.setReceiverCountry("");
        paperEngageRequest.setReceiverFiscalCode("");
        paperEngageRequest.setSenderName("");
        paperEngageRequest.setSenderAddress("");
        paperEngageRequest.setSenderCity("");
        paperEngageRequest.setSenderPr("");
        paperEngageRequest.setSenderDigitalAddress("");
        paperEngageRequest.setArName("");
        paperEngageRequest.setArAddress("");
        paperEngageRequest.setArCap("");
        paperEngageRequest.setArCity("");
        var vas = new HashMap<String,String>();
        paperEngageRequest.setVas(vas);
        paperEngageRequest.setIun("iun");
        paperEngageRequest.setRequestPaId("PagoPa");
        paperEngageRequest.setProductType("product type");
        paperEngageRequest.setPrintType("B/N");
        paperEngageRequest.setRequestId("requestIdx");
        paperEngageRequest.setClientRequestTimeStamp(OffsetDateTime.now());
    }

    private WebTestClient.ResponseSpec sendCartaceoTestCall(BodyInserter<PaperEngageRequest, ReactiveHttpOutputMessage> bodyInserter
            , String requestIdx) {

        return this.webTestClient.put()
                .uri(uriBuilder -> uriBuilder.path(SEND_CARTACEO_ENDPOINT).build(requestIdx))
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .body(bodyInserter)
                .header(ID_CLIENT_HEADER_NAME, DEFAULT_ID_CLIENT_HEADER_VALUE)
                .exchange();
    }

    // Per il momento le chiamate tra i vari microservizi di EC sono mocckate per
    // evitare problemi di precondizioni nei vari ambienti


    @Test
    void sendCartaceoOk() {

        when(authService.clientAuth(anyString())).thenReturn(Mono.empty());
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));
        when(uriBuilderCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.just(new FileDownloadResponse()));
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
                .isEqualTo(SERVICE_UNAVAILABLE).expectBody(RequestDto.class);
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


        when(authService.clientAuth(anyString())).thenThrow(EcInternalEndpointHttpException.class);
//      Client auth -> OK
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.just(clientConfigurationDto));

//      Retrieve request -> OK (If no request is found an exception of type RestCallException.ResourceNotFoundException is thrown)
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));

//      Mock dell'eccezione trhowata dalla pubblicazione sulla coda
        when(sqsService.send(eq(notificationTrackerSqsName.statoCartaceoName()), any(NotificationTrackerQueueDto.class))).thenReturn(Mono.error(
                new SqsPublishException(notificationTrackerSqsName.statoCartaceoName())));

        sendCartaceoTestCall(BodyInserters.fromValue(paperEngageRequest), DEFAULT_REQUEST_IDX).expectStatus()
                .isEqualTo(SERVICE_UNAVAILABLE)
                .expectBody(Problem.class);
    }




    @Test
    void sendCartaceoQueueKo() {
        //      Client auth -> OK
        when(authService.clientAuth(anyString())).thenThrow(EcInternalEndpointHttpException.class);
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.just(clientConfigurationDto));

//      Retrieve request -> OK (If no request is found an exception of type RestCallException.ResourceNotFoundException is thrown)
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));

//      Mock dell'eccezione trhowata dalla pubblicazione sulla coda
        when(sqsService.send(eq(cartaceoSqsQueueName.batchName()),
                any(DigitalNotificationRequest.class))).thenReturn(Mono.error(new SqsPublishException(cartaceoSqsQueueName.batchName())));

        sendCartaceoTestCall(BodyInserters.fromValue(paperEngageRequest), DEFAULT_REQUEST_IDX).expectStatus()
                .isEqualTo(SERVICE_UNAVAILABLE)
                .expectBody(Problem.class);
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