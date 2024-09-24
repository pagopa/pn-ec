package it.pagopa.pn.ec.pec.rest;

import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.ClientNotAuthorizedException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.AttachmentNotAvailableException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCallImpl;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.pec.configurationproperties.PecSqsQueueName;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.commons.constant.Status.BOOKED;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.ChannelEnum.*;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient(timeout = "50000")
public class DigitalNotificationRequestApiControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @Autowired
    private PecSqsQueueName pecSqsQueueName;

    @MockBean
    private FileCall uriBuilderCall;

    @MockBean
    private GestoreRepositoryCallImpl gestoreRepositoryCall;

    @MockBean
    private AuthService authService;

    @SpyBean
    private SqsServiceImpl sqsService;

    public static final String SEND_PEC_ENDPOINT = "/external-channels/v1/digital-deliveries/legal-full-message-requests" + "/{requestIdx}";
    private static final DigitalNotificationRequest digitalNotificationRequest = new DigitalNotificationRequest();
    private static final ClientConfigurationInternalDto clientConfigurationInternalDto = new ClientConfigurationInternalDto();
    private static final String DEFAULT_ATTACHMENT_URL = "safestorage://prova.pdf";
    private static final String SERCQ_ADDRESS = "x-pagopa-pn-sercq:send-self:notification-already-delivered?timestamp=" + OffsetDateTime.now();
    private static final String PEC_ADDRESS = "pippo@pec.it";


    @BeforeEach
    public void createDigitalNotificationRequest() {

        List<String> defaultListAttachmentUrls = new ArrayList<>();
        defaultListAttachmentUrls.add(DEFAULT_ATTACHMENT_URL);

        digitalNotificationRequest.setRequestId("requestIdx");
        digitalNotificationRequest.eventType("string");
        digitalNotificationRequest.setClientRequestTimeStamp(OffsetDateTime.now());
        digitalNotificationRequest.setQos(INTERACTIVE);
        digitalNotificationRequest.setReceiverDigitalAddress(PEC_ADDRESS);
        digitalNotificationRequest.setMessageText("string");
        digitalNotificationRequest.channel(PEC);
        digitalNotificationRequest.setSubjectText("prova testo");
        digitalNotificationRequest.setMessageContentType(PLAIN);
        digitalNotificationRequest.setAttachmentUrls(defaultListAttachmentUrls);
    }

    private WebTestClient.ResponseSpec sendPecTestCall(BodyInserter<DigitalNotificationRequest, ReactiveHttpOutputMessage> bodyInserter,
                                                       String requestIdx) {

        return this.webTestClient.put()
                                 .uri(uriBuilder -> uriBuilder.path(SEND_PEC_ENDPOINT).build(requestIdx))
                                 .accept(APPLICATION_JSON)
                                 .contentType(APPLICATION_JSON)
                                 .body(bodyInserter)
                                 .header(ID_CLIENT_HEADER_NAME, DEFAULT_ID_CLIENT_HEADER_VALUE)
                                 .exchange();
    }

    // Per il momento le chiamate tra i vari microservizi di EC sono mocckate per evitare problemi di precondizioni nei vari ambienti

    //PECPIC.100.1 -> Test case positivo
    @ParameterizedTest(name = "Testing with channel: {0} and address: {1}")
    @MethodSource("getChannelAndAddress")
    void sendPecOk(DigitalNotificationRequest. ChannelEnum channel, String address) {
        digitalNotificationRequest.setChannel(channel);
        digitalNotificationRequest.setReceiverDigitalAddress(address);

        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(uriBuilderCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.just(new FileDownloadResponse()));
        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), DEFAULT_REQUEST_IDX).expectStatus().isOk();
    }

    //PECPIC.100.4 -> Request body non corretto
    @ParameterizedTest(name = "Testing with channel: {0} and address: {1}")
    @MethodSource("getChannelAndAddress")
    void sendPecBadBody(DigitalNotificationRequest.ChannelEnum channel, String address) {
        digitalNotificationRequest.setChannel(channel);
        digitalNotificationRequest.setReceiverDigitalAddress(address);

        sendPecTestCall(BodyInserters.empty(), DEFAULT_REQUEST_IDX).expectStatus().isBadRequest().expectBody(Problem.class);
    }

    //PECPIC.100.2 -> Validazione della regex sul path param requestIdx KO
    @ParameterizedTest(name = "Testing with channel: {0} and address: {1}")
    @MethodSource("getChannelAndAddress")
    void sendPecMalformedIdClient(DigitalNotificationRequest.ChannelEnum channel, String address) {
        digitalNotificationRequest.setChannel(channel);
        digitalNotificationRequest.setReceiverDigitalAddress(address);
        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), BAD_REQUEST_IDX_SHORT).expectStatus()
                                                                                           .isBadRequest()
                                                                                           .expectBody(Problem.class);
    }

    //PECPIC.100.3.2 -> idClient non autorizzato
    @ParameterizedTest(name = "Testing with channel: {0} and address: {1}")
    @MethodSource("getChannelAndAddress")
    void sendPecUnauthorizedIdClient(DigitalNotificationRequest.ChannelEnum channel, String address) {
        digitalNotificationRequest.setChannel(channel);
        digitalNotificationRequest.setReceiverDigitalAddress(address);

        //      Client auth call -> OK
//      Client non tornato dall'anagrafica client
        when(authService.clientAuth(anyString())).thenReturn(Mono.error(new ClientNotAuthorizedException(DEFAULT_ID_CLIENT_HEADER_VALUE)));

        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                 .isForbidden()
                                                                                                 .expectBody(Problem.class);
    }

    // Richiesta di invio SMS già effettuata, contenuto della richiesta uguale
    @ParameterizedTest(name = "Testing with channel: {0} and address: {1}")
    @MethodSource("getChannelAndAddress")
    void sendSmsRequestWithSameContentAlreadyMade(DigitalNotificationRequest.ChannelEnum channel, String address) {
        digitalNotificationRequest.setChannel(channel);
        digitalNotificationRequest.setReceiverDigitalAddress(address);



//      Client auth -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(uriBuilderCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.just(new FileDownloadResponse()));

//      Insert request -> Returns a 204 mapped to empty Mono, because a request with the same hash already exists
        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.empty());

        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                 .isEqualTo(OK)
                                                                                                 .expectBody(Problem.class);
    }

    //PECPIC.100.9 -> Richiesta di invio PEC già effettuata
    @ParameterizedTest(name = "Testing with channel: {0} and address: {1}")
    @MethodSource("getChannelAndAddress")
    void sendPecRequestAlreadyMade(DigitalNotificationRequest.ChannelEnum channel, String address) {
        digitalNotificationRequest.setChannel(channel);
        digitalNotificationRequest.setReceiverDigitalAddress(address);


//      Client auth -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(uriBuilderCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.just(new FileDownloadResponse()));

//      Insert request -> Returns a 409 mapped to RestCallException.ResourceAlreadyExistsException error signal, because a request with
//      same id but different hash already exists
        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.error(new RestCallException.ResourceAlreadyExistsException()));

        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                 .isEqualTo(CONFLICT)
                                                                                                 .expectBody(Problem.class);
    }

    //PECPIC.100.7 -> Pubblicazione sulla coda "Notification tracker stato PEC" -> KO
    @ParameterizedTest(name = "Testing with channel: {0} and address: {1}")
    @MethodSource("getChannelAndAddress")
    void sendPecNotificationTrackerKo(DigitalNotificationRequest.ChannelEnum channel, String address) {
        digitalNotificationRequest.setChannel(channel);
        digitalNotificationRequest.setReceiverDigitalAddress(address);


//      Client auth -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(uriBuilderCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.just(new FileDownloadResponse()));
        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

//      Mock dell'eccezione throwata dalla pubblicazione sulla coda
        when(sqsService.send(eq(notificationTrackerSqsName.statoPecName()),
                             argThat((NotificationTrackerQueueDto notificationTrackerQueueDto) -> Objects.equals(notificationTrackerQueueDto.getNextStatus(),
                                                                                                                 BOOKED.getStatusTransactionTableCompliant())))).thenReturn(
                Mono.error(new SqsClientException(notificationTrackerSqsName.statoPecName())));
        when(sqsService.send(eq("pn-ec-tracker-sercq-send-stato-queue.fifo"),
                             any(PresaInCaricoInfo.class))).thenReturn(Mono.error(new SqsClientException(notificationTrackerSqsName.statoPecName())));

        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                 .isEqualTo(SERVICE_UNAVAILABLE)
                                                                                                 .expectBody(Problem.class);
    }

    //PECPIC.100.8 -> Pubblicazione sulla coda "PEC" -> KO
    @ParameterizedTest(name = "Testing with channel: {0} and address: {1}")
    @MethodSource("getChannelAndAddress")
    void sendPecQueueKo(DigitalNotificationRequest.ChannelEnum channel, String address) {
        digitalNotificationRequest.setChannel(channel);
        digitalNotificationRequest.setReceiverDigitalAddress(address);


//      Client auth -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
        when(uriBuilderCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.just(new FileDownloadResponse()));
        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

//      Mock dell'eccezione throwata dalla pubblicazione sulla coda
        when(sqsService.send(eq(pecSqsQueueName.interactiveName()),
                             any(PresaInCaricoInfo.class))).thenReturn(Mono.error(new SqsClientException(pecSqsQueueName.interactiveName())));
        when(sqsService.send(eq("pn-ec-tracker-sercq-send-stato-queue.fifo"),
                             any(PresaInCaricoInfo.class))).thenReturn(Mono.error(new SqsClientException(pecSqsQueueName.interactiveName())));

        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                 .isEqualTo(SERVICE_UNAVAILABLE)
                                                                                                 .expectBody(Problem.class);
    }

    //PECPIC.100.5 -> Attachment non disponibile dentro pn-ss
    @ParameterizedTest(name = "Testing with channel: {0} and address: {1}")
    @MethodSource("getChannelAndAddress")
    void sendPecWithoutValidAttachment(DigitalNotificationRequest.ChannelEnum channel, String address) {
        digitalNotificationRequest.setChannel(channel);
        digitalNotificationRequest.setReceiverDigitalAddress(address);


//      Client auth call -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));

        when(uriBuilderCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.error(new AttachmentNotAvailableException(
                DEFAULT_ATTACHMENT_URL)));

        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                 .isEqualTo(BAD_REQUEST)
                                                                                                 .expectBody(Problem.class);
    }

    @Test
    void sendPecUnsupportedChannel() {
        digitalNotificationRequest.setChannel(REM);
        digitalNotificationRequest.setReceiverDigitalAddress("test@test.it");

        sendPecTestCall(BodyInserters.fromValue(digitalNotificationRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                    .isEqualTo(INTERNAL_SERVER_ERROR)
                                                                                                    .expectBody(Problem.class);
    }

private static Stream<Arguments> getChannelAndAddress() {
        return Stream.of(
                Arguments.of(PEC, PEC_ADDRESS),
                Arguments.of(SERCQ, SERCQ_ADDRESS)
        );
    }
}
