package it.pagopa.pn.ec.rest;

import it.pagopa.pn.ec.localstack.LocalStackTestConfig;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pn.ec.testutils.factory.EcRequestObjectFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;

import static it.pagopa.pn.ec.constant.EcRestApiConstant.ID_CLIENT_HEADER;
import static it.pagopa.pn.ec.testutils.constant.RestApiTestConstants.*;
import static it.pagopa.pn.ec.utils.EndpointUtils.getSendSmsEndpoint;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Import(LocalStackTestConfig.class)
class DigitalCourtesyMessagesApiControllerTest {

    @Autowired
    private WebTestClient webClient;

    private WebTestClient.ResponseSpec sendSmsTestCall(BodyInserter<DigitalCourtesySmsRequest, ReactiveHttpOutputMessage> bodyInserter,
                                                       String requestIdx) {
        return this.webClient.put()
                             .uri(getSendSmsEndpoint(requestIdx))
                             .accept(APPLICATION_JSON)
                             .contentType(APPLICATION_JSON)
                             .body(bodyInserter)
                             .header(ID_CLIENT_HEADER, DEFAULT_ID_CLIENT_HEADER)
                             .exchange();
    }

    //SMSPIC.107.1 -> Test case positivo
    @Test
    void sendSmsOkTest() {
        sendSmsTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalCourtesySmsRequest()), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                                            .isOk();
    }

    //SMSPIC.107.2 -> Request body non corretto
    @Test
    void sendSmsBadBodyTest() {
        sendSmsTestCall(BodyInserters.empty(), DEFAULT_REQUEST_IDX).expectStatus().isBadRequest().expectBody(Problem.class);
    }

    //SMSPIC.107.3 -> Validazione della regex sul path param requestIdx KO
    @ParameterizedTest
    @ValueSource(strings = {BAD_REQUEST_IDX_SHORT, BAD_REQUEST_IDX_CHAR_NOT_ALLOWED})
    void sendSmsMalformedIdClientTest(String badRequestIdx) {
        sendSmsTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalCourtesySmsRequest()), badRequestIdx).expectStatus()
                                                                                                                      .isBadRequest()
                                                                                                                      .expectBody(Problem.class);
    }

    //SMSPIC.107.4 -> idClient non autorizzato
    @Test
    void sendSmsUnauthorizedIdClientTest() {
        sendSmsTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalCourtesySmsRequest()), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                                            .isUnauthorized()
                                                                                                                            .expectBody(
                                                                                                                                    Problem.class);
    }

    //SMSPIC.107.5 -> Coda "Notification tracker" down
    @Test
    void sendSmsNotificationTrackerDownTest() {
        sendSmsTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalCourtesySmsRequest()), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                                            .isEqualTo(
                                                                                                                                    SERVICE_UNAVAILABLE)
                                                                                                                            .expectBody(
                                                                                                                                    Problem.class);
    }

    //SMSPIC.107.6 -> Coda "SMS" down
    @Test
    void sendSmsSmsQueueDownTest() {
        sendSmsTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalCourtesySmsRequest()), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                                            .isEqualTo(
                                                                                                                                    SERVICE_UNAVAILABLE)
                                                                                                                            .expectBody(
                                                                                                                                    Problem.class);
    }

    //SMSPIC.107.6 -> Richiesta di invio SMS già effettuata
    @Test
    void sendSmsRequestAlreadyMadeTest() {
        sendSmsTestCall(BodyInserters.fromValue(EcRequestObjectFactory.getDigitalCourtesySmsRequest()), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                                            .isEqualTo(
                                                                                                                                    CONFLICT)
                                                                                                                            .expectBody(
                                                                                                                                    Problem.class);
    }
}
