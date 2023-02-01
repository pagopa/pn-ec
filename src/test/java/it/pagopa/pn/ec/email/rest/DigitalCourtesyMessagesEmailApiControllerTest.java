package it.pagopa.pn.ec.email.rest;

import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.anagraficaclient.AnagraficaClientCallImpl;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.richieste.RichiesteCallImpl;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.email.model.dto.NtStatoEmailQueueDto;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
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
import java.util.Date;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.*;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.ChannelEnum.EMAIL;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON;


@SpringBootTestWebEnv
@AutoConfigureWebTestClient
class DigitalCourtesyMessagesEmailApiControllerTest {


    @Autowired
    private WebTestClient webTClient;

    @MockBean
    private AnagraficaClientCallImpl anagraficaClientCall;

    @MockBean
    private RichiesteCallImpl richiesteCall;

    @SpyBean
    private SqsServiceImpl sqsService;

    private static final String SEND_EMAIL_ENDPOINT =
            "/external-channels/v1/digital-deliveries/courtesy-full-message-requests" + "/{requestIdx}";

    private static final DigitalCourtesyMailRequest digitalCourtesyEmailRequest = new DigitalCourtesyMailRequest();

    @BeforeAll
    public static void createDigitalCourtesyEmailRequest() {
        String defaultStringInit = "string";

        digitalCourtesyEmailRequest.setRequestId(defaultStringInit);
        digitalCourtesyEmailRequest.eventType(defaultStringInit);
        digitalCourtesyEmailRequest.setClientRequestTimeStamp(new Date());
        digitalCourtesyEmailRequest.setQos(INTERACTIVE);
        digitalCourtesyEmailRequest.setReceiverDigitalAddress(defaultStringInit);
        digitalCourtesyEmailRequest.setMessageText(defaultStringInit);
        digitalCourtesyEmailRequest.channel(EMAIL);
    }

    private WebTestClient.ResponseSpec sendEmailTestCall(BodyInserter<DigitalCourtesyMailRequest, ReactiveHttpOutputMessage> bodyInserter,
                                                       String requestIdx) {
        return this.webTClient.put()
                .uri(uriBuilder -> uriBuilder.path(SEND_EMAIL_ENDPOINT).build(requestIdx))
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .body(bodyInserter)
                .header(ID_CLIENT_HEADER_NAME, DEFAULT_ID_CLIENT_HEADER_VALUE)
                .exchange();
    }


    @Test
    void sendEmailOk() {
        when(anagraficaClientCall.getClient(anyString())).thenReturn(Mono.just(DEFAULT_ID_CLIENT_HEADER_VALUE));
        when(richiesteCall.getRichiesta(anyString())).thenReturn(Mono.just(Status.STATUS_1));

        sendEmailTestCall(BodyInserters.fromValue(digitalCourtesyEmailRequest), DEFAULT_REQUEST_IDX).expectStatus().isOk();
    }



}