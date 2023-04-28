package it.pagopa.pn.ec.email.rest;


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

import it.pagopa.pn.ec.email.configurationproperties.EmailSqsQueueName;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


import static it.pagopa.pn.ec.commons.constant.Status.BOOKED;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.ChannelEnum.EMAIL;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;


@SpringBootTestWebEnv
@AutoConfigureWebTestClient
class DigitalCourtesyMessagesEmailApiControllerTest {


    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @Autowired
    private EmailSqsQueueName emailSqsQueueName;

    @MockBean
    private FileCall uriBuilderCall;

    @MockBean
    private GestoreRepositoryCallImpl gestoreRepositoryCall;

    @SpyBean
    private SqsServiceImpl sqsService;

    @MockBean
    private AuthService authService;

    public static final String SEND_EMAIL_ENDPOINT =
            "/external-channels/v1/digital-deliveries/courtesy-full-message-requests" + "/{requestIdx}";
    private static final DigitalCourtesyMailRequest digitalCourtesyMailRequest = new DigitalCourtesyMailRequest();
    private static final ClientConfigurationDto clientConfigurationDto = new ClientConfigurationDto();
    private static final String defaultAttachmentUrl = "safestorage://prova.pdf";

    @BeforeAll
    public static void createDigitalCourtesyMailRequest() {

        List<String> defaultListAttachmentUrls = new ArrayList<>();
        defaultListAttachmentUrls.add(defaultAttachmentUrl);

        digitalCourtesyMailRequest.setRequestId("requestIdx");
        digitalCourtesyMailRequest.eventType("string");
        digitalCourtesyMailRequest.setClientRequestTimeStamp(OffsetDateTime.now());
        digitalCourtesyMailRequest.setQos(INTERACTIVE);
        digitalCourtesyMailRequest.setReceiverDigitalAddress("");
        digitalCourtesyMailRequest.setMessageText("");
        digitalCourtesyMailRequest.channel(EMAIL);
        digitalCourtesyMailRequest.setSubjectText("Test");
        digitalCourtesyMailRequest.setMessageContentType(PLAIN);
        digitalCourtesyMailRequest.setAttachmentsUrls(defaultListAttachmentUrls);
    }

    private WebTestClient.ResponseSpec sendEmailTestCall(BodyInserter<DigitalCourtesyMailRequest, ReactiveHttpOutputMessage> bodyInserter
            , String requestIdx) {

        return this.webTestClient.put()
                                 .uri(uriBuilder -> uriBuilder.path(SEND_EMAIL_ENDPOINT).build(requestIdx))
                                 .accept(APPLICATION_JSON)
                                 .contentType(APPLICATION_JSON)
                                 .body(bodyInserter)
                                 .header(ID_CLIENT_HEADER_NAME, DEFAULT_ID_CLIENT_HEADER_VALUE)
                                 .exchange();
    }

    //EMIALPIC.100.1 -> Test case positivo
    @Test
    void sendEmailOk() {

        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationDto));
        when(uriBuilderCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.just(new FileDownloadResponse()));
        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

        sendEmailTestCall(BodyInserters.fromValue(digitalCourtesyMailRequest), DEFAULT_REQUEST_IDX).expectStatus().isOk();
    }

    //EMIALPIC.100.4 -> Request body non corretto
    @Test
    void sendEmailBadBody() {
        sendEmailTestCall(BodyInserters.empty(), DEFAULT_REQUEST_IDX).expectStatus().isBadRequest().expectBody(Problem.class);
    }

    //PECPIC.100.2 -> Validazione della regex sul path param requestIdx KO
    @ParameterizedTest
    @ValueSource(strings = {BAD_REQUEST_IDX_SHORT, BAD_REQUEST_IDX_CHAR_NOT_ALLOWED})
    void sendEmailMalformedIdClient(String badRequestIdx) {
        sendEmailTestCall(BodyInserters.fromValue(digitalCourtesyMailRequest), badRequestIdx).expectStatus()
                                                                                             .isBadRequest()
                                                                                             .expectBody(Problem.class);
    }

    //EMIALPIC.100.3.2 -> idClient non autorizzato
    @Test
    void sendEmailUnauthorizedIdClient() {

//      Client non tornato dall'anagrafica client
        when(authService.clientAuth(anyString())).thenReturn(Mono.error(new ClientNotAuthorizedException(DEFAULT_ID_CLIENT_HEADER_VALUE)));

        sendEmailTestCall(BodyInserters.fromValue(digitalCourtesyMailRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                   .isForbidden()
                                                                                                   .expectBody(Problem.class);
    }

    //EMIALPIC.100.9 -> Richiesta di invio EMAIL già effettuata, contenuto della richiesta diverso
    @Test
    void sendEmailRequestAlreadyMade() {
//      Client auth -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationDto));

        when(uriBuilderCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.just(new FileDownloadResponse()));

//      Insert request -> Returns a 409 mapped to RestCallException.ResourceAlreadyExistsException error signal, because a request with
//      same id but different hash already exists
        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.error(new RestCallException.ResourceAlreadyExistsException()));

        sendEmailTestCall(BodyInserters.fromValue(digitalCourtesyMailRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                   .isEqualTo(CONFLICT)
                                                                                                   .expectBody(Problem.class);
    }

    // Richiesta di invio EMAIL già effettuata, contenuto della richiesta uguale
    @Test
    void sendSmsRequestWithSameContentAlreadyMade() {

//      Client auth -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationDto));

        when(uriBuilderCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.just(new FileDownloadResponse()));

//      Insert request -> Returns a 204 mapped to empty Mono, because a request with the same hash already exists
        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.empty());

        sendEmailTestCall(BodyInserters.fromValue(digitalCourtesyMailRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                .isEqualTo(OK)
                                                                                                .expectBody(Problem.class);
    }

    //EMIALPIC.100.7 -> Pubblicazione sulla coda "Notification tracker stato EMAIL" -> KO
    @Test
    void sendEmailNotificationTrackerKo() {
//      Client auth -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationDto));

        when(uriBuilderCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.just(new FileDownloadResponse()));

        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

//      Mock dell'eccezione trhowata dalla pubblicazione sulla coda
        when(sqsService.send(eq(notificationTrackerSqsName.statoEmailName()),
                             argThat((NotificationTrackerQueueDto notificationTrackerQueueDto) -> Objects.equals(notificationTrackerQueueDto.getNextStatus(),
                                                                                                                 BOOKED.getStatusTransactionTableCompliant())))).thenReturn(
                Mono.error(new SqsClientException(notificationTrackerSqsName.statoSmsName())));

        sendEmailTestCall(BodyInserters.fromValue(digitalCourtesyMailRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                   .isEqualTo(SERVICE_UNAVAILABLE)
                                                                                                   .expectBody(Problem.class);
    }

    //EMIALPIC.100.8 -> Pubblicazione sulla coda "EMAIL" -> KO
    @Test
    void sendEmailQueueKo() {

//      Client auth -> OK
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationDto));

        when(uriBuilderCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.just(new FileDownloadResponse()));

        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

//      Mock dell'eccezione trhowata dalla pubblicazione sulla coda
        when(sqsService.send(eq(emailSqsQueueName.interactiveName()),
                             any(EmailPresaInCaricoInfo.class))).thenReturn(Mono.error(new SqsClientException(emailSqsQueueName.interactiveName())));

        sendEmailTestCall(BodyInserters.fromValue(digitalCourtesyMailRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                   .isEqualTo(SERVICE_UNAVAILABLE)
                                                                                                   .expectBody(Problem.class);
    }

    //EMIALPIC.100.5 -> Attachment non disponibile dentro pn-ss
    @Test
    void sendEmailWithoutValidAttachment() {

        when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationDto));
        when(gestoreRepositoryCall.getRichiesta(anyString(),
                                                anyString())).thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));
        when(uriBuilderCall.getFile(anyString(), anyString(), anyBoolean())).thenReturn(Mono.error(new AttachmentNotAvailableException(
                defaultAttachmentUrl)));
        when(gestoreRepositoryCall.insertRichiesta(any(RequestDto.class))).thenReturn(Mono.just(new RequestDto()));

        sendEmailTestCall(BodyInserters.fromValue(digitalCourtesyMailRequest), DEFAULT_REQUEST_IDX).expectStatus()
                                                                                                   .isEqualTo(BAD_REQUEST)
                                                                                                   .expectBody(Problem.class);
    }
}
