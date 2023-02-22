package it.pagopa.pn.ec.sms.rest;

import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.ClientNotAuthorizedException;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCallImpl;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.sms.configurationproperties.SmsSqsQueueName;
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

import static it.pagopa.pn.ec.sms.testutils.DigitalCourtesySmsRequestFactory.createSmsRequest;
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

	private static final String SEND_SMS_ENDPOINT = "/external-channels/v1/digital-deliveries/courtesy-simple-message-requests"
			+ "/{requestIdx}";

	private static final DigitalCourtesySmsRequest digitalCourtesySmsRequest = createSmsRequest();
	private static final RequestDto requestDto = new RequestDto();

	@BeforeAll
	public static void createDigitalCourtesySmsRequest() {

//      Mock an existing request. Set the requestIdx
		requestDto.setRequestIdx("requestIdx");
	}

	private WebTestClient.ResponseSpec sendSmsTestCall(
			BodyInserter<DigitalCourtesySmsRequest, ReactiveHttpOutputMessage> bodyInserter, String requestIdx) {
		return this.webTestClient.put().uri(uriBuilder -> uriBuilder.path(SEND_SMS_ENDPOINT).build(requestIdx))
				.accept(APPLICATION_JSON).contentType(APPLICATION_JSON).body(bodyInserter)
				.header(ID_CLIENT_HEADER_NAME, DEFAULT_ID_CLIENT_HEADER_VALUE).exchange();
	}

	// Per il momento le chiamate tra i vari microservizi di EC sono mocckate per
	// evitare problemi di precondizioni nei vari ambienti

	// SMSPIC.107.1 -> Test case positivo
	@Test
	void sendSmsOk() {

		when(authService.clientAuth(anyString())).thenReturn(Mono.empty());
		when(gestoreRepositoryCall.getRichiesta(anyString()))
				.thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));
		when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

		sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus().isOk();
	}

	// SMSPIC.107.2 -> Request body non corretto
	@Test
	void sendSmsBadBody() {
		sendSmsTestCall(BodyInserters.empty(), DEFAULT_REQUEST_IDX).expectStatus().isBadRequest()
				.expectBody(Problem.class);
	}

	// SMSPIC.107.3 -> Validazione della regex sul path param requestIdx KO
	@ParameterizedTest
	@ValueSource(strings = { BAD_REQUEST_IDX_SHORT, BAD_REQUEST_IDX_CHAR_NOT_ALLOWED })
	void sendSmsMalformedIdClient(String badRequestIdx) {
		sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), badRequestIdx).expectStatus().isBadRequest()
				.expectBody(Problem.class);
	}

	// SMSPIC.107.4 -> Chiamata verso Anagrafica Client per l'autenticazione del
	// client -> KO
	@Test
	void callForClientAuthKo() {

//      Client auth call -> KO
		when(authService.clientAuth(anyString())).thenThrow(EcInternalEndpointHttpException.class);

		sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
				.isEqualTo(SERVICE_UNAVAILABLE).expectBody(Problem.class);
	}

	// SMSPIC.107.5 -> idClient non autorizzato
	@Test
	void sendSmsUnauthorizedIdClient() {

//      Client auth call -> OK
//      Client non tornato dall'anagrafica client
		when(authService.clientAuth(anyString()))
				.thenReturn(Mono.error(new ClientNotAuthorizedException(DEFAULT_ID_CLIENT_HEADER_VALUE)));

//      Retrieve request -> OK (If no request is found a RestCallException.ResourceNotFoundException is thrown)
		when(gestoreRepositoryCall.getRichiesta(anyString()))
				.thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));

		sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
				.isForbidden().expectBody(Problem.class);
	}

	// SMSPIC.107.6 -> Chiamata verso Gestore Repository per il recupero dello stato
	// corrente -> KO
	@Test
	void callToRetrieveCurrentStatusKo() {

//      Client auth call -> OK
		when(authService.clientAuth(anyString())).thenReturn(Mono.empty());

//      Retrieve request -> KO
		when(gestoreRepositoryCall.getRichiesta(anyString())).thenThrow(EcInternalEndpointHttpException.class);

		sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
				.isEqualTo(SERVICE_UNAVAILABLE).expectBody(Problem.class);
	}

	// SMSPIC.107.7 -> Richiesta di invio SMS già effettuata
	@Test
	void sendSmsRequestAlreadyMade() {

//      Client auth -> OK
		when(authService.clientAuth(anyString())).thenReturn(Mono.empty());

//      Retrieve request -> Return an existent request, return 409 status
		when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.just(requestDto));

		sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
				.isEqualTo(CONFLICT).expectBody(Problem.class);
	}

	// SMSPIC.107.8 -> Pubblicazione sulla coda "Notification tracker stato SMS" ->
	// KO
	@Test
	void sendSmsNotificationTrackerKo() {

//      Client auth -> OK
		when(authService.clientAuth(anyString())).thenReturn(Mono.empty());

//      Retrieve request -> OK (If no request is found a RestCallException.ResourceNotFoundException is thrown)
		when(gestoreRepositoryCall.getRichiesta(anyString()))
				.thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));

		when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

//      Mock dell'eccezione trhowata dalla pubblicazione sulla coda
		when(sqsService.send(eq(notificationTrackerSqsName.statoSmsName()), any(NotificationTrackerQueueDto.class)))
				.thenReturn(Mono.error(new SqsPublishException(notificationTrackerSqsName.statoSmsName())));

		sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
				.isEqualTo(SERVICE_UNAVAILABLE).expectBody(Problem.class);
	}

	// SMSPIC.107.9 -> Pubblicazione sulla coda "SMS" -> KO
	@Test
	void sendSmsSmsQueueKo() {

//      Client auth -> OK
		when(authService.clientAuth(anyString())).thenReturn(Mono.empty());

//      Retrieve request -> OK (If no request is found a RestCallException.ResourceNotFoundException is thrown)
		when(gestoreRepositoryCall.getRichiesta(anyString()))
				.thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));

		when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

//      Mock dell'eccezione trhowata dalla pubblicazione sulla coda
		when(sqsService.send(eq(smsSqsQueueName.interactiveName()), any(SmsPresaInCaricoInfo.class)))
				.thenReturn(Mono.error(new SqsPublishException(smsSqsQueueName.interactiveName())));

		sendSmsTestCall(BodyInserters.fromValue(digitalCourtesySmsRequest), DEFAULT_REQUEST_IDX).expectStatus()
				.isEqualTo(SERVICE_UNAVAILABLE).expectBody(Problem.class);
	}
}
