package it.pagopa.pn.ec.rest;

import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import it.pagopa.pn.ec.service.impl.AuthServiceImpl;
import it.pagopa.pn.ec.testutils.factory.EcRequestObjectFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;

import static it.pagopa.pn.ec.constant.EcRestApiConstant.ID_CLIENT_HEADER;
import static it.pagopa.pn.ec.utils.EndpointUtils.getSendSmsEndpoint;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@RunWith(SpringRunner.class)
@WebFluxTest(DigitalCourtesyMessagesApiController.class)
public class DigitalCourtesyMessagesApiControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    AuthServiceImpl authService;

    private final String defaultClientId = "CLIENT_ID_123";

    private WebTestClient.ResponseSpec sendSmsTestCall(BodyInserter<DigitalCourtesySmsRequest, ReactiveHttpOutputMessage> bodyInserter,
                                                       String clientId) {
        return this.webClient.put()
                             .uri(getSendSmsEndpoint("12345"))
                             .accept(APPLICATION_JSON)
                             .contentType(APPLICATION_JSON)
                             .body(bodyInserter)
                             .header(ID_CLIENT_HEADER, clientId)
                             .exchange();
    }

    //SMSPIC.107.1 -> Test case positivo
    @Test
    public void sendSmsOkTest() {
        sendSmsTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalCourtesySmsRequest()), defaultClientId).expectStatus()
                                                                                                                        .isOk();
    }

    //SMSPIC.107.2 -> Request body non corretto
    @Test
    public void sendSmsBadBodyTest() {
        sendSmsTestCall(BodyInserters.empty(), defaultClientId).expectStatus().isBadRequest().expectBody(Problem.class);
    }

    //SMSPIC.107.3 -> Validazione della regex sull'idClient KO
    @Test
    public void sendSmsMalformedIdClientTest() {
        sendSmsTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalCourtesySmsRequest()), "bad").expectStatus()
                                                                                                              .isBadRequest()
                                                                                                              .expectBody(Problem.class);
        // TODO: Lo status dovrebbe essere 400 ma la validazione automatica di OpenApi fa tornare 500
    }

    //SMSPIC.107.4 -> idClient non autorizzato
    @Test
    public void sendSmsUnauthorizedIdClientTest() {
        sendSmsTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalCourtesySmsRequest()), defaultClientId).expectStatus()
                                                                                                                        .isUnauthorized()
                                                                                                                        .expectBody(Problem.class);
    }

    //SMSPIC.107.5 -> Coda "Notification tracker" down
    @Test
    public void sendSmsNotificationTrackerDownTest() {
        sendSmsTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalCourtesySmsRequest()), defaultClientId).expectStatus()
                                                                                                                        .isEqualTo(
                                                                                                                                SERVICE_UNAVAILABLE)
                                                                                                                        .expectBody(Problem.class);
    }

    //SMSPIC.107.6 -> Coda "SMS" down
    @Test
    public void sendSmsSmsQueueDownTest() {
        sendSmsTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalCourtesySmsRequest()), defaultClientId).expectStatus()
                                                                                                                        .isEqualTo(
                                                                                                                                SERVICE_UNAVAILABLE)
                                                                                                                        .expectBody(Problem.class);
    }

    //SMSPIC.107.6 -> Richiesta di invio SMS già effettuata
    @Test
    public void sendSmsRequestAlreadyMadeTest() {
        sendSmsTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalCourtesySmsRequest()), defaultClientId).expectStatus()
                                                                                                                        .isEqualTo(CONFLICT)
                                                                                                                        .expectBody(Problem.class);
    }
}
