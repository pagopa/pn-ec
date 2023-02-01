package it.pagopa.pn.ec.pec.rest;

import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCallImpl;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.pec.model.dto.NtStatoPecQueueDto;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
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

import java.time.OffsetDateTime;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.NT_STATO_PEC_QUEUE_NAME;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.PEC_INTERACTIVE_QUEUE_NAME;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.ChannelEnum.PEC;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
public class DigitalNotificationRequestApiControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private GestoreRepositoryCallImpl gestoreRepositoryCall;

    @SpyBean
    private SqsServiceImpl sqsService;

    public static final String SEND_PEC_ENDPOINT = "/external-channels/v1/digital-deliveries/legal-full-message-requests" + "/{requestIdx}";
    private static final DigitalNotificationRequest digitalNotificationRequest = new DigitalNotificationRequest();
    private static final ClientConfigurationDto clientConfigurationDto = new ClientConfigurationDto();
    private static final RequestDto requestDto = new RequestDto();

    @BeforeAll
    public static void createDigitalNotificationRequest() {
        String defaultStringInit = "string";

        digitalNotificationRequest.setRequestId(defaultStringInit);
        digitalNotificationRequest.eventType(defaultStringInit);
        digitalNotificationRequest.setClientRequestTimeStamp(OffsetDateTime.now());
        digitalNotificationRequest.setQos(INTERACTIVE);
        digitalNotificationRequest.setReceiverDigitalAddress(defaultStringInit);
        digitalNotificationRequest.setMessageText(defaultStringInit);
        digitalNotificationRequest.channel(PEC);
        digitalNotificationRequest.setSubjectText(defaultStringInit);
        digitalNotificationRequest.setMessageContentType(PLAIN);
    }

    private WebTestClient.ResponseSpec sendPecTestCall(BodyInserter<DigitalNotificationRequest, ReactiveHttpOutputMessage> bodyInserter, String requestIdx) {

        return this.webTestClient.put()
                .uri(uriBuilder -> uriBuilder.path(SEND_PEC_ENDPOINT).build(requestIdx))
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .body(bodyInserter)
                .header(ID_CLIENT_HEADER_NAME, DEFAULT_ID_CLIENT_HEADER_VALUE)
                .exchange();
    }

    //PECPIC.100.1 -> Test case positivo
    @Test
    void sendPecOk() {
//      Per il momento gli esiti positivi delle chiamate interne sono mocckati dato che non sono ancora stati implementati gli endpoint
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.just(clientConfigurationDto));
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.empty());

        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), DEFAULT_REQUEST_IDX).expectStatus().isOk();
    }

    //PECPIC.100.4 -> Request body non corretto
    @Test
    void sendPecBadBody() {
        sendPecTestCall(BodyInserters.empty(), DEFAULT_REQUEST_IDX).expectStatus().isBadRequest().expectBody(Problem.class);
    }

    //PECPIC.100.2 -> Validazione della regex sul path param requestIdx KO
    @ParameterizedTest
    @ValueSource(strings = {BAD_REQUEST_IDX_SHORT, BAD_REQUEST_IDX_CHAR_NOT_ALLOWED})
    void sendPecMalformedIdClient(String badRequestIdx) {
        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), badRequestIdx).expectStatus()
                                                                                           .isBadRequest()
                                                                                           .expectBody(Problem.class);
    }

    //PECPIC.100.3.1 -> Chiamata verso Anagrafica Client per l'autenticazione del client -> KO
    @Test
    void callForClientAuthKo() {

//      Client auth call -> KO
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenThrow(EcInternalEndpointHttpException.class);

        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                 .isEqualTo(SERVICE_UNAVAILABLE)
                                                                                                 .expectBody(Problem.class);
    }

    //PECPIC.100.3.2 -> idClient non autorizzato
    @Test
    void sendPecUnauthorizedIdClient() {

//      Client auth call -> OK
//      Client non tornato dall'anagrafica client
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.error(new RestCallException.ResourceNotFoundException(
                "Client not " + "found")));

//      Retrieve status -> OK
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.just(requestDto));

        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                 .isUnauthorized()
                                                                                                 .expectBody(Problem.class);
    }

    //PECPIC.100.6 -> Chiamata verso Gestore Repository per il recupero dello stato corrente -> KO
    @Test
    void callToRetrieveCurrentStatusKo() {

//      Client auth call -> OK
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.just(clientConfigurationDto));

//      Retrieve status -> KO
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenThrow(EcInternalEndpointHttpException.class);

        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                 .isEqualTo(SERVICE_UNAVAILABLE)
                                                                                                 .expectBody(Problem.class);
    }


    //PECPIC.100.9 -> Richiesta di invio PEC già effettuata
    @Test
    void sendPecRequestAlreadyMade() {

//      Client auth -> OK
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.just(clientConfigurationDto));

//      Status della richiesta tornato dall'anagrafica client -> IN_LAVORAZIONE
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.just(requestDto));

        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                 .isEqualTo(CONFLICT)
                                                                                                 .expectBody(Problem.class);
    }

    //PECPIC.100.7 -> Pubblicazione sulla coda "Notification tracker stato PEC" -> KO
    @Test
    void sendPecNotificationTrackerKo() {

//      Client auth -> OK
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.just(clientConfigurationDto));

//      Retrieve status -> OK
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.empty());

//      Mock dell'eccezione trhowata dalla pubblicazione sulla coda
        when(sqsService.send(eq(NT_STATO_PEC_QUEUE_NAME), any(NtStatoPecQueueDto.class))).thenReturn(Mono.error(new SqsPublishException(NT_STATO_PEC_QUEUE_NAME)));

        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                 .isEqualTo(SERVICE_UNAVAILABLE)
                                                                                                 .expectBody(Problem.class);
    }

    //PECPIC.100.8 -> Pubblicazione sulla coda "PEC" -> KO
    @Test
    void sendPecQueueKo() {

//      Client auth -> OK
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.just(clientConfigurationDto));

//      Retrieve status -> OK
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.empty());

//      Mock dell'eccezione trhowata dalla pubblicazione sulla coda
        when(sqsService.send(eq(PEC_INTERACTIVE_QUEUE_NAME), any(DigitalNotificationRequest.class))).thenReturn(Mono.error(new SqsPublishException(PEC_INTERACTIVE_QUEUE_NAME)));

        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                 .isEqualTo(SERVICE_UNAVAILABLE)
                                                                                                 .expectBody(Problem.class);
    }

//    PECPIC.100.5 -> Attachment non disponibile dentro pn-ss
//    @Test
//    void sendPecWithoutValidAttachment() {
//
//    }

}
