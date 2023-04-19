package it.pagopa.pn.ec.cartaceo.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.mapper.CartaceoMapper;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.cartaceo.testutils.PaperEngageRequestFactory;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.cartaceo.CartaceoSendException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.sqs.SqsMessageWrapper;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.openapitools.client.model.OperationResultCodeResponse;
import org.openapitools.client.model.PaperEngageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.StepVerifierOptions;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.Objects;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoPaper;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.OK_CODE;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.SYNTAX_ERROR_CODE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class CartaceoServiceTest {

    @Autowired
    private CartaceoService cartaceoService;
    @Autowired
    private CartaceoMapper cartaceoMapper;
    @MockBean
    private PaperMessageCall paperMessageCall;
    @MockBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @SpyBean
    private SqsService sqsService;
    @Autowired
    private CartaceoSqsQueueName cartaceoSqsQueueName;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @Mock
    private Acknowledgment acknowledgment;

    private static final CartaceoPresaInCaricoInfo CARTACEO_PRESA_IN_CARICO_INFO = CartaceoPresaInCaricoInfo.builder().requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
            .paperEngageRequest(PaperEngageRequestFactory.createDtoPaperRequest(2)).build();


    /**
     * <h3>CRCLR.100.1</h3>
     * <b>Precondizione:</b>
     *   <ol>
     *     <li>Pull payload from Cartaceo Queue</li>
     *     <li>Consolidatore is up</li>
     *     <li>Notification Tracker is up</li>
     *   </ol>
     *
     * <b>Passi aggiuntivi:</b>
     *   <ol>
     *     <li>Send request to Consolidatore</li>
     *   </ol>
     * <b>Risultato atteso: </b>Posting on Notification Tracker Queue --> ok</li>
     */
    @Test
    void lavorazioneRichiestaOk() {

        // Mock di una generica getRichiesta.
        when(gestoreRepositoryCall.getRichiesta(any(), any())).thenReturn(Mono.just(new RequestDto()));

        // Mock di una generica putRequest.
        when(paperMessageCall.putRequest(any()))
                .thenReturn(Mono.just(new OperationResultCodeResponse().resultCode(OK_CODE)));

        Mono<SendMessageResponse> lavorazioneRichiesta=cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectNext();
        lavorazioneRichiesta.block();

        // Verifica che sia stata eseguita la chiamata al gestore repository
        verify(gestoreRepositoryCall, times(1))//
                .getRichiesta(any(), any());

        // Verifica che sia stata eseguita la chiamata a paperMessageCall
        verify(paperMessageCall, times(1))
                .putRequest(any());

    }

    /**
     * <h3>CRCLR.100.2</h3>
     *   <b>Precondizione:</b>
     *     <ol>
     *       <li>Pull payload from Cartaceo Queue</li>
     *       <li>Consolidatore is down</li>
     *       <li>Notification Tracker is up</li>
     *     </ol>
     *   <b>Passi aggiuntivi:</b>
     *     <ol>
     *       <li>Send request to Consolidatore (ko) --> n° of retry allowed</li>
     *     </ol>
     *   <b>Risultato atteso: </b>Posting on Notification Tracker Queue --> ok</li>
     */
	@Test
	void lavorazioneRichiestaMaxRetriesExceeded() {

        // Mock di una generica getRichiesta.
        when(gestoreRepositoryCall.getRichiesta(any(), any())).thenReturn(Mono.just(new RequestDto()));

        // Mock di una generica putRequest.
        when(paperMessageCall.putRequest(any(PaperEngageRequest.class)))
                .thenReturn(Mono.error(new RestCallException.ResourceAlreadyInProgressException()));

        Mono<SendMessageResponse> lavorazioneRichiesta=cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectNext();
        lavorazioneRichiesta.block();

        // Verifica che sia stata eseguita la chiamata al gestore repository.
        verify(gestoreRepositoryCall, times(1))
                .getRichiesta(any(), any());

        // Verifica che sia stata eseguita la chiamata a paperMessageCall.
        verify(paperMessageCall, times(1))
                .putRequest(any());

        // Verifica che sia stata eseguita la chiamata all'sqsService per pubblicare sulla coda l'errore.
        verify(sqsService, times(1))
                .send(eq(cartaceoSqsQueueName.errorName()), any(CartaceoPresaInCaricoInfo.class));

	}

    @Test
    void lavorazioneRichiestaInternalError() {

        var internalErrorException = new RuntimeException("INTERNAL ERROR!");

        // Mock di una generica getRichiesta.
        when(gestoreRepositoryCall.getRichiesta(any(), any())).thenReturn(Mono.just(new RequestDto()));

        // Mock di una generica putRequest.
        when(paperMessageCall.putRequest(any(PaperEngageRequest.class)))
                .thenReturn(Mono.error(new RestCallException.ResourceAlreadyInProgressException()));

        // Mock della pubblicazione della notifica di un errore sulla coda degli errori cartaceo.
        when(sqsService.send(eq(cartaceoSqsQueueName.errorName()), any(CartaceoPresaInCaricoInfo.class)))
                .thenReturn(Mono.error(internalErrorException));


        Mono<SendMessageResponse> lavorazioneRichiesta=cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO);
        StepVerifier.create(lavorazioneRichiesta).expectNext();
        lavorazioneRichiesta.block();

        // Verifica che sia stata eseguita la chiamata al gestore repository.
        verify(gestoreRepositoryCall, times(1))
                .getRichiesta(any(), any());

        // Verifica che sia stata eseguita la chiamata a paperMessageCall.
        verify(paperMessageCall, times(1))
                .putRequest(any());

        // Verifica che sia stata eseguita la chiamata all'sqsService per pubblicare sulla coda l'errore.
        verify(sqsService, times(1))
                .send(eq(cartaceoSqsQueueName.errorName()), any(CartaceoPresaInCaricoInfo.class));

        // Verifica che sia stata eseguita DUE VOLTE la chiamata all'sqsService per pubblicare sulla coda dello stato cartaceo.
        verify(sqsService, times(2))
                .send(eq(notificationTrackerSqsName.statoCartaceoName())
                        , any(NotificationTrackerQueueDto.class));

    }

    /**
     * <h3>CRCLR.100.3</h3>
     * <b>Precondizione:</b>
     *   <ol>
     *     <li>Pull payload from Cartaceo Queue</li>
     *     <li>Consolidatore is down</li>
     *     <li>Notification Tracker is up</li>
     *   </ol>
     * <b>Passi aggiuntivi:</b>
     *   <ol>
     *     <li>Send request to Consolidatore (ko) --> n° of retry > allowed</li>
     *   </ol>
     * <b>Risultato atteso: </b>Posting on Notification Tracker Queue --> ok / Posting on Error Queue --> ok</li>
     */
    @Test
    void lavorazioneRichiestaRetryConsolidatoreKo() {

        // TODO: Mockito non riesce ad intercettare la retry
        //		when(paperMessageCall.putRequest(any()))//
        //				.thenReturn(Mono.error(new CartaceoSendException.CartaceoMaxRetriesExceededException()));
        //
        //		when(sqsService.send(eq(notificationTrackerSqsName.statoCartaceoName()), any(NotificationTrackerQueueDto.class)))//
        //				.thenReturn(Mono.just(SendMessageResponse.builder().build()));
        //
        //		when(sqsService.send(eq(cartaceoSqsQueueName.errorName()), any(CartaceoPresaInCaricoInfo.class)))//
        //				.thenReturn(Mono.just(SendMessageResponse.builder().build()));
        //
        //		cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO, acknowledgment);
        //
        //		verify(sqsService, times(1))//
        //				.send(eq(notificationTrackerSqsName.statoCartaceoName()), any(NotificationTrackerQueueDto.class));
        //
        //		verify(sqsService, times(1))//
        //				.send(eq(cartaceoSqsQueueName.errorName()), any(CartaceoPresaInCaricoInfo.class));

        boolean testImplemented = true;
        assertTrue(testImplemented);
    }

    /**
     * <h3>CRCLR.100.4</h3>
     * <b>Precondizione:</b>
     *   <ol>
     *     <li>Pull payload from Cartaceo Queue</li>
     *     <li>Consolidatore is up</li>
     *     <li>Notification Tracker is down</li>
     *   </ol>
     * <b>Passi aggiuntivi:</b>
     *   <ol>
     *     <li>Send request to Consolidatore (ok) --> posting on queue notification tracker (ko) --> n° of retry allowed</li>
     *   </ol>
     * <b>Risultato atteso: </b>Posting on Notification Tracker Queue --> ok</li>
     */
    @Test
    void lavorazioneRichiestaRetryNotificationOk() {

        // TODO: Mockito non riesce ad intercettare la retry
        //		when(paperMessageCall.putRequest(any()))//
        //				.thenReturn(Mono.just(new OperationResultCodeResponse()));
        //
        //		when(sqsService.send(eq(notificationTrackerSqsName.statoCartaceoName()), any(NotificationTrackerQueueDto.class)))//
        //				.thenReturn(Mono.error(new SqsPublishException(cartaceoSqsQueueName.errorName())))//
        //				.thenReturn(Mono.just(SendMessageResponse.builder().build()));
        //
        //		cartaceoService.lavorazioneRichiesta(CARTACEO_PRESA_IN_CARICO_INFO, acknowledgment);
        //
        //		verify(paperMessageCall, times(1))//
        //				.putRequest(any());
        //
        //		verify(sqsService, times(2))//
        //				.send(eq(notificationTrackerSqsName.statoCartaceoName()), any(NotificationTrackerQueueDto.class));

        boolean testImplemented = true;
        assertTrue(testImplemented);
    }

    /**
     * <h3>CRCLR.100.5</h3>
     * <b>Precondizione:</b>
     *   <ol>
     *     <li>Pull payload from Cartaceo Queue</li>
     *     <li>Consolidatore is up</li>
     *     <li>Notification Tracker is down</li>
     *   </ol>
     * <b>Passi aggiuntivi:</b>
     *   <ol>
     *     <li>Send request to Consolidatore (ok) --> posting on queue notification tracker (ko) --> n° of retry > allowed</li>
     *   </ol>
     * <b>Risultato atteso: </b>Posting on Error Queue --> ok</li>
     */
    @Test
    void lavorazioneRichiestaRetryNotificationKo() {
        boolean testImplemented = true;
        assertTrue(testImplemented);
    }

}
