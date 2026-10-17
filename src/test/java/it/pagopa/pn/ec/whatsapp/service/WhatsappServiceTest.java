package it.pagopa.pn.ec.whatsapp.service;


import it.pagopa.pn.ec.commons.exception.whatsapp.WhatsappSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.service.EumSocialService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.WhatsappTemplate;
import it.pagopa.pn.ec.whatsapp.model.pojo.WhatsappPresaInCaricoInfo;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import static it.pagopa.pn.ec.commons.constant.Status.RETRY;
import static it.pagopa.pn.ec.commons.constant.Status.SENT;
import static it.pagopa.pn.ec.whatsapp.testutils.DigitalCourtesyWhatsappRequestFactory.createWhatsappRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.mockito.Mockito.*;


@SpringBootTestWebEnv
class WhatsappServiceTest {

    @MockitoSpyBean
    private WhatsappService whatsappService;

    @Autowired
    private PnEcConfig pnEcConfig;

    private PnEcConfig.NotificationTracker.SqsQueue notificationTrackerSqsName() {
        return pnEcConfig.getNotificationTracker().getSqsQueue();
    }

    @MockitoSpyBean
    private SqsServiceImpl sqsService;

    @MockitoBean
    private EumSocialService eumSocialService;

    private static final WhatsappPresaInCaricoInfo WHATSAPP_PRESA_IN_CARICO_INFO = WhatsappPresaInCaricoInfo.builder()
            .requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalCourtesyWhatsappRequest(createWhatsappRequest())
            .build();

    /**
     * <h3>WALR.107.1</h3>
     * <b>Precondizione:</b> Pull di un payload dalla coda "WHATSAPP"
     * <br>
     * <b>Passi aggiuntivi:</b> Invio WhatsApp con End User Messaging Social -> OK
     * <br>
     * <b>Risultato atteso:</b> Pubblicazione sulla coda Notification Tracker -> OK
     */
    @Test
    void lavorazioneRichiestaOk() {
        when(eumSocialService.sendTemplateMessage(anyString(), any(WhatsappTemplate.class), anyString())).thenReturn(Mono.just("msg-1"));

        Mono<SendMessageResponse> response = whatsappService.lavorazioneRichiesta(WHATSAPP_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(whatsappService, times(1)).sendNotificationOnStatusQueue(eq(WHATSAPP_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    /**
     * <h3>WALR.107.3</h3>
     * <ul>
     *   <li><b>Precondizione:</b> Pull di un payload dalla coda "WHATSAPP"</li>
     *   <li><b>Passi aggiuntivi:</b>
     *     <ol>
     *       <li>Invio WhatsApp con End User Messaging Social -> OK</li>
     *       <li>Pubblicazione sulla coda Notification Tracker -> KO</li>
     *     </ol>
     *   </li>
     *   <li><b>Risultato atteso:</b> Pubblicazione sulla coda Errori WhatsApp -> OK</li>
     *   <li><b>Note:</b> Il payload pubblicato sulla coda notificherà al flow di retry di riprovare solamente la pubblicazione sulla coda
     *   "Notification Tracker"
     *   </li>
     * </ul>
     */
    @Test
    void lavorazioneRichiestaNtKo() {
        when(eumSocialService.sendTemplateMessage(anyString(), any(WhatsappTemplate.class), anyString())).thenReturn(Mono.just("msg-1"));

        when(sqsService.send(eq(notificationTrackerSqsName().getStatoWhatsappName()), any(NotificationTrackerQueueDto.class))).thenReturn(Mono.error(
                new SqsClientException(notificationTrackerSqsName().getStatoWhatsappName())));

        Mono<SendMessageResponse> response = whatsappService.lavorazioneRichiesta(WHATSAPP_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(whatsappService, times(1)).sendNotificationOnStatusQueue(eq(WHATSAPP_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        verify(whatsappService, times(1)).sendNotificationOnErrorQueue(eq(WHATSAPP_PRESA_IN_CARICO_INFO));
    }

    /**
     * <h3>WALR.107.3</h3>
     * <ul>
     *   <li><b>Precondizione:</b> Pull di un payload dalla coda "WHATSAPP"</li>
     *   <li><b>Passi aggiuntivi:</b>
     *     <ol>
     *       <li>Invio WhatsApp con End User Messaging Social -> KO</li>
     *       <li>Pubblicazione sulla coda Notification Tracker -> OK</li>
     *     </ol>
     *   </li>
     *   <li><b>Risultato atteso:</b> Pubblicazione sulla coda Errori WhatsApp -> OK</li>
     *   <li><b>Note:</b> Il payload pubblicato sulla coda notificherà al flow di retry di riprovare solamente la pubblicazione sulla coda
     *   "Notification Tracker"
     *   </li>
     * </ul>
     */
    @Test
    void lavorazioneRichiestaEumSocialKo()
    {
        when(eumSocialService.sendTemplateMessage(anyString(), any(WhatsappTemplate.class), anyString())).thenReturn(Mono.error(new WhatsappSendException()));

        Mono<SendMessageResponse> response = whatsappService.lavorazioneRichiesta(WHATSAPP_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(whatsappService, times(1)).sendNotificationOnStatusQueue(eq(WHATSAPP_PRESA_IN_CARICO_INFO), eq(RETRY.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        verify(whatsappService, times(1)).sendNotificationOnErrorQueue(eq(WHATSAPP_PRESA_IN_CARICO_INFO));
    }

    @Test
    void callbackDataIsTheConcatenatedRequestId() {
        org.junit.jupiter.api.Assertions.assertEquals(it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId(DEFAULT_ID_CLIENT_HEADER_VALUE, DEFAULT_REQUEST_IDX),
                WhatsappService.callbackData(WHATSAPP_PRESA_IN_CARICO_INFO));
    }

}