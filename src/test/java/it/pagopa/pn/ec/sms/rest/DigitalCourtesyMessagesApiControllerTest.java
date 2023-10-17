package it.pagopa.pn.ec.sms.rest;

import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.ClientNotAuthorizedException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCallImpl;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.sms.configurationproperties.SmsSqsQueueName;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static it.pagopa.pn.ec.commons.constant.Status.BOOKED;
import static it.pagopa.pn.ec.sms.testutils.DigitalCourtesySmsRequestFactory.createSmsRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient(timeout = "50000")
class DigitalCourtesyMessagesApiControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private SmsSqsQueueName smsSqsQueueName;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @MockBean
    private GestoreRepositoryCallImpl gestoreRepositoryCall;

    @MockBean
    private AuthService authService;

    @SpyBean
    private SqsServiceImpl sqsService;

    private static final String SEND_SMS_ENDPOINT =
            "/external-channels/v1/digital-deliveries/courtesy-simple-message-requests" + "/{requestIdx}";

    private static final DigitalCourtesySmsRequest digitalCourtesySmsRequest = createSmsRequest();
    private static final ClientConfigurationDto clientConfigurationDto = new ClientConfigurationDto();
    private static final ClientConfigurationInternalDto clientConfigurationInternalDto = new ClientConfigurationInternalDto();

    private WebTestClient.ResponseSpec sendSmsTestCall(BodyInserter<DigitalCourtesySmsRequest, ReactiveHttpOutputMessage> bodyInserter,
                                                       String requestIdx) {
        return this.webTestClient.put()
                                 .uri(uriBuilder -> uriBuilder.path(SEND_SMS_ENDPOINT).build(requestIdx))
                                 .accept(APPLICATION_JSON)
                                 .contentType(APPLICATION_JSON)
                                 .body(bodyInserter)
                                 .header(ID_CLIENT_HEADER_NAME, DEFAULT_ID_CLIENT_HEADER_VALUE)
                                 .exchange();
    }

    // Per il momento le chiamate tra i vari microservizi di EC sono mocckate per
    // evitare problemi di precondizioni nei vari ambienti

    // SMSPIC.107.1 -> Test case positivo
    @Test
    void sendSmsOk() {

        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

        sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus().isOk();
    }

    // SMSPIC.107.2 -> Request body non corretto
    @Test
    void sendSmsBadBody() {
        sendSmsTestCall(BodyInserters.empty(), DEFAULT_REQUEST_IDX).expectStatus().isBadRequest().expectBody(Problem.class);
    }

    // SMSPIC.107.3 -> Validazione della regex sul path param requestIdx KO
    @ParameterizedTest
    @ValueSource(strings = {BAD_REQUEST_IDX_SHORT})
    void sendSmsMalformedIdClient(String badRequestIdx) {
        sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), badRequestIdx).expectStatus()
                                                                                          .isBadRequest()
                                                                                          .expectBody(Problem.class);
    }

	// SMSPIC.107.5 -> idClient non autorizzato
	@Test
	void sendSmsUnauthorizedIdClient() {

//      Client auth call -> OK
//      Client non tornato dall'anagrafica client
        when(authService.clientAuth(anyString())).thenReturn(Mono.error(new ClientNotAuthorizedException(DEFAULT_ID_CLIENT_HEADER_VALUE)));

        sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                .isForbidden()
                                                                                                .expectBody(Problem.class);
    }

    // Richiesta di invio SMS già effettuata, contenuto della richiesta uguale
    @Test
    void sendSmsRequestWithSameContentAlreadyMade() {

//      Client auth -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));

//      Insert request -> Returns a 204 mapped to empty Mono, because a request with the same hash already exists
        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.empty());

        sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                .isEqualTo(OK)
                                                                                                .expectBody(Problem.class);
    }

    //  SMSPIC.107.7 -> Richiesta di invio SMS già effettuata, contenuto della richiesta diverso
    @Test
    void sendSmsRequestWithDifferentContentAlreadyMade() {

//      Client auth -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));

//      Insert request -> Returns a 409 mapped to RestCallException.ResourceAlreadyExistsException error signal, because a request with
//      same id but different hash already exists
        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.error(new RestCallException.ResourceAlreadyExistsException()));

        sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                .isEqualTo(CONFLICT)
                                                                                                .expectBody(Problem.class);
    }

    // SMSPIC.107.8 -> Pubblicazione sulla coda "Notification tracker stato SMS" -> KO
    @Test
    void sendSmsNotificationTrackerKo() {

//      Client auth -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));

        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

//      Mock dell'eccezione throwata dalla pubblicazione sulla coda
        when(sqsService.send(eq(notificationTrackerSqsName.statoSmsName()),
                             argThat((NotificationTrackerQueueDto notificationTrackerQueueDto) -> Objects.equals(notificationTrackerQueueDto.getNextStatus(),
                                                                                                                 BOOKED.getStatusTransactionTableCompliant())))).thenReturn(
                Mono.error(new SqsClientException(notificationTrackerSqsName.statoSmsName())));

        sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                .isEqualTo(SERVICE_UNAVAILABLE)
                                                                                                .expectBody(Problem.class);
    }

    // SMSPIC.107.9 -> Pubblicazione sulla coda "SMS" -> KO
    @Test
    void sendSmsSmsQueueKo() {

//      Client auth -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));

        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

//      Mock dell'eccezione throwata dalla pubblicazione sulla coda
        when(sqsService.send(eq(smsSqsQueueName.interactiveName()),
                             any(SmsPresaInCaricoInfo.class))).thenReturn(Mono.error(new SqsClientException(smsSqsQueueName.interactiveName())));

        sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                .isEqualTo(SERVICE_UNAVAILABLE)
                                                                                                .expectBody(Problem.class);
    }
}
