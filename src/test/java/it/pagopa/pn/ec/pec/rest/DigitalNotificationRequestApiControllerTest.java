package it.pagopa.pn.ec.pec.rest;

import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.anagraficaclient.AnagraficaClientCallImpl;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.richieste.RichiesteCallImpl;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.pec.model.dto.NtStatoPecQueueDto;
import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pn.ec.testutils.factory.EcRequestObjectFactory;
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

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.NT_STATO_PEC_QUEUE_NAME;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.PEC_QUEUE_NAME;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
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
    private AnagraficaClientCallImpl anagraficaClientCall;

    @MockBean
    private RichiesteCallImpl richiesteCall;

    @SpyBean
    private SqsServiceImpl sqsService;

//    private SmsEndpointUtils() {
//        throw new IllegalStateException("EndpointUtils is a utility class");
//    }

    public static final String SEND_PEC_ENDPOINT = "/external-channels/v1/digital-deliveries/legal-full-message-requests/%s";

    public static String getSendPecEndpoint(String requestIdx) {
        return String.format(SEND_PEC_ENDPOINT, requestIdx);
    }

    private WebTestClient.ResponseSpec sendPecTestCall(BodyInserter<DigitalNotificationRequest, ReactiveHttpOutputMessage> bodyInserter, String requestIdx) {

        return this.webTestClient.put()
                .uri(getSendPecEndpoint(requestIdx))
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
        when(anagraficaClientCall.getClient(anyString())).thenReturn(Mono.just(DEFAULT_ID_CLIENT_HEADER_VALUE));
        when(richiesteCall.getRichiesta(anyString())).thenReturn(Mono.just(Status.STATUS_1));

        sendPecTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalNotificationRequest()), DEFAULT_REQUEST_IDX).expectStatus()
                .isOk();
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
        sendPecTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalNotificationRequest()), badRequestIdx).expectStatus()
                                                                                                                       .isBadRequest()
                                                                                                                       .expectBody(Problem.class);
    }

    //PECPIC.100.3.1 -> Chiamata verso Anagrafica Client per l'autenticazione del client -> KO
    @Test
    void callForClientAuthKo() {

//      Client auth call -> KO
        when(anagraficaClientCall.getClient(anyString())).thenThrow(EcInternalEndpointHttpException.class);

        sendPecTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalNotificationRequest()), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                                             .isEqualTo(
                                                                                                                                     SERVICE_UNAVAILABLE)
                                                                                                                             .expectBody(
                                                                                                                                     Problem.class);
    }

    //PECPIC.100.3.2 -> idClient non autorizzato
    @Test
    void sendPecUnauthorizedIdClient() {

//      Client auth call -> OK
//      Client non tornato dall'anagrafica client
        when(anagraficaClientCall.getClient(anyString())).thenReturn(Mono.empty());

//      Retrieve status -> OK
        when(richiesteCall.getRichiesta(anyString())).thenReturn(Mono.just(Status.STATUS_1));

        sendPecTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalNotificationRequest()), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                                             .isUnauthorized()
                                                                                                                             .expectBody(
                                                                                                                                     Problem.class);
    }

    //PECPIC.100.6 -> Chiamata verso Gestore Repository per il recupero dello stato corrente -> KO
    @Test
    void callToRetrieveCurrentStatusKo() {

//      Client auth call -> OK
        when(anagraficaClientCall.getClient(anyString())).thenReturn(Mono.just(DEFAULT_ID_CLIENT_HEADER_VALUE));

//      Retrieve status -> KO
        when(richiesteCall.getRichiesta(anyString())).thenThrow(EcInternalEndpointHttpException.class);

        sendPecTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalNotificationRequest()), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                                             .isEqualTo(
                                                                                                                                     SERVICE_UNAVAILABLE)
                                                                                                                             .expectBody(
                                                                                                                                     Problem.class);
    }

    //PECPIC.100.9 -> Richiesta di invio PEC già effettuata
    @Test
    void sendPecRequestAlreadyMade() {

//      Client auth -> OK
        when(anagraficaClientCall.getClient(anyString())).thenReturn(Mono.just(DEFAULT_ID_CLIENT_HEADER_VALUE));

//      Status della richiesta tornato dall'anagrafica client -> IN_LAVORAZIONE
        when(richiesteCall.getRichiesta(anyString())).thenReturn(Mono.just(Status.IN_LAVORAZIONE));

        sendPecTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalNotificationRequest()), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                                             .isEqualTo(
                                                                                                                                     CONFLICT)
                                                                                                                             .expectBody(
                                                                                                                                     Problem.class);
    }

    //PECPIC.100.7 -> Pubblicazione sulla coda "Notification tracker stato PEC" -> KO
    @Test
    void sendPecNotificationTrackerKo() {

//      Client auth -> OK
        when(anagraficaClientCall.getClient(anyString())).thenReturn(Mono.just(DEFAULT_ID_CLIENT_HEADER_VALUE));

//      Retrieve status -> OK
        when(richiesteCall.getRichiesta(anyString())).thenReturn(Mono.just(Status.STATUS_1));

//      Mock dell'eccezione trhowata dalla pubblicazione sulla coda
        doThrow(SqsPublishException.class).when(sqsService).send(eq(NT_STATO_PEC_QUEUE_NAME), any(NtStatoPecQueueDto.class));

        sendPecTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalNotificationRequest()), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                                             .isEqualTo(
                                                                                                                                     SERVICE_UNAVAILABLE)
                                                                                                                             .expectBody(
                                                                                                                                     Problem.class);
    }

    //PECPIC.100.8 -> Pubblicazione sulla coda "PEC" -> KO
    @Test
    void sendPecQueueKo() {

//      Client auth -> OK
        when(anagraficaClientCall.getClient(anyString())).thenReturn(Mono.just(DEFAULT_ID_CLIENT_HEADER_VALUE));

//      Retrieve status -> OK
        when(richiesteCall.getRichiesta(anyString())).thenReturn(Mono.just(Status.STATUS_1));

//      Mock dell'eccezione trhowata dalla pubblicazione sulla coda
        doThrow(SqsPublishException.class).when(sqsService).send(eq(PEC_QUEUE_NAME), any(DigitalNotificationRequest.class));

        sendPecTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalNotificationRequest()), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                                             .isEqualTo(
                                                                                                                                     SERVICE_UNAVAILABLE)
                                                                                                                             .expectBody(
                                                                                                                                     Problem.class);
    }

//    PECPIC.100.5 -> Attachment non disponibile dentro pn-ss
//    @Test
//    void sendPecWithoutValidAttachment() {
//
//    }

}
