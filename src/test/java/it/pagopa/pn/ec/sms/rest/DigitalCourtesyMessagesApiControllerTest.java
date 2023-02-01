package it.pagopa.pn.ec.sms.rest;

import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCallImpl;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.sms.model.dto.NtStatoSmsQueueDto;
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

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.NT_STATO_SMS_QUEUE_NAME;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.SMS_INTERACTIVE_QUEUE_NAME;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.ChannelEnum.SMS;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON;


@SpringBootTestWebEnv
@AutoConfigureWebTestClient
class DigitalCourtesyMessagesApiControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private GestoreRepositoryCallImpl gestoreRepositoryCall;

    @SpyBean
    private SqsServiceImpl sqsService;

    private static final String SEND_SMS_ENDPOINT =
            "/external-channels/v1/digital-deliveries/courtesy-simple-message-requests" + "/{requestIdx}";

    private static final DigitalCourtesySmsRequest digitalCourtesySmsRequest = new DigitalCourtesySmsRequest();
    private static final ClientConfigurationDto clientConfigurationDto = new ClientConfigurationDto();
    private static final RequestDto requestDto = new RequestDto();

    @BeforeAll
    public static void createDigitalCourtesySmsRequest() {
        String defaultStringInit = "string";

        digitalCourtesySmsRequest.setRequestId(defaultStringInit);
        digitalCourtesySmsRequest.eventType(defaultStringInit);
        digitalCourtesySmsRequest.setClientRequestTimeStamp(OffsetDateTime.now());
        digitalCourtesySmsRequest.setQos(INTERACTIVE);
        digitalCourtesySmsRequest.setReceiverDigitalAddress(defaultStringInit);
        digitalCourtesySmsRequest.setMessageText(defaultStringInit);
        digitalCourtesySmsRequest.channel(SMS);
    }

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

    //SMSPIC.107.1 -> Test case positivo
    @Test
    void sendSmsOk() {
//      Per il momento gli esiti positivi delle chiamate interne sono mocckati dato che non sono ancora stati implementati gli endpoint
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.just(clientConfigurationDto));
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.empty());

        sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus().isOk();
    }

    //SMSPIC.107.2 -> Request body non corretto
    @Test
    void sendSmsBadBody() {
        sendSmsTestCall(BodyInserters.empty(), DEFAULT_REQUEST_IDX).expectStatus().isBadRequest().expectBody(Problem.class);
    }

    //SMSPIC.107.3 -> Validazione della regex sul path param requestIdx KO
    @ParameterizedTest
    @ValueSource(strings = {BAD_REQUEST_IDX_SHORT, BAD_REQUEST_IDX_CHAR_NOT_ALLOWED})
    void sendSmsMalformedIdClient(String badRequestIdx) {
        sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), badRequestIdx).expectStatus()
                                                                                          .isBadRequest()
                                                                                          .expectBody(Problem.class);
    }

    //SMSPIC.107.4 -> Chiamata verso Anagrafica Client per l'autenticazione del client -> KO
    @Test
    void callForClientAuthKo() {

//      Client auth call -> KO
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenThrow(EcInternalEndpointHttpException.class);

        sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                .isEqualTo(SERVICE_UNAVAILABLE)
                                                                                                .expectBody(Problem.class);
    }

    //SMSPIC.107.5 -> idClient non autorizzato
    @Test
    void sendSmsUnauthorizedIdClient() {

//      Client auth call -> OK
//      Client non tornato dall'anagrafica client
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.error(new RestCallException.ResourceNotFoundException(
                "Client not " + "found")));

//      Retrieve status -> OK
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.just(requestDto));

        sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                .isUnauthorized()
                                                                                                .expectBody(Problem.class);
    }

    //SMSPIC.107.6 -> Chiamata verso Gestore Repository per il recupero dello stato corrente -> KO
    @Test
    void callToRetrieveCurrentStatusKo() {

//      Client auth call -> OK
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.just(clientConfigurationDto));

//      Retrieve status -> KO
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenThrow(EcInternalEndpointHttpException.class);

        sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                .isEqualTo(SERVICE_UNAVAILABLE)
                                                                                                .expectBody(Problem.class);
    }


    //SMSPIC.107.7 -> Richiesta di invio SMS già effettuata
    @Test
    void sendSmsRequestAlreadyMade() {

//      Client auth -> OK
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.just(clientConfigurationDto));

//      Status della richiesta tornato dall'anagrafica client -> IN_LAVORAZIONE
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.just(requestDto));

        sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                .isEqualTo(CONFLICT)
                                                                                                .expectBody(Problem.class);
    }

    //SMSPIC.107.8 -> Pubblicazione sulla coda "Notification tracker stato SMS" -> KO
    @Test
    void sendSmsNotificationTrackerKo() {

//      Client auth -> OK
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.just(clientConfigurationDto));

//      Retrieve status -> OK
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.empty());

//      Mock dell'eccezione trhowata dalla pubblicazione sulla coda
        when(sqsService.send(eq(NT_STATO_SMS_QUEUE_NAME), any(NtStatoSmsQueueDto.class))).thenReturn(Mono.error(new SqsPublishException(NT_STATO_SMS_QUEUE_NAME)));

        sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                .isEqualTo(SERVICE_UNAVAILABLE)
                                                                                                .expectBody(Problem.class);
    }

    //SMSPIC.107.9 -> Pubblicazione sulla coda "SMS" -> KO
    @Test
    void sendSmsSmsQueueKo() {

//      Client auth -> OK
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.just(clientConfigurationDto));

//      Retrieve status -> OK
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.empty());

//      Mock dell'eccezione trhowata dalla pubblicazione sulla coda
        when(sqsService.send(eq(SMS_INTERACTIVE_QUEUE_NAME), any(DigitalCourtesySmsRequest.class))).thenReturn(Mono.error(new SqsPublishException(SMS_INTERACTIVE_QUEUE_NAME)));

        sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                .isEqualTo(SERVICE_UNAVAILABLE)
                                                                                                .expectBody(Problem.class);
    }
}
