package it.pagopa.pn.ec.sms.service;


import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.service.SnsService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.util.LogSanitizer;
import org.apache.commons.lang3.StringUtils;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.util.retry.Retry;
import java.time.Duration;

import static it.pagopa.pn.ec.commons.constant.Status.RETRY;
import static it.pagopa.pn.ec.commons.constant.Status.SENT;
import static it.pagopa.pn.ec.sms.testutils.DigitalCourtesySmsRequestFactory.createSmsRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.mockito.Mockito.*;
import static it.pagopa.pn.ec.commons.utils.LogUtils.LAVORAZIONE_RICHIESTA_SMS;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTestWebEnv
class SmsServiceTest {

    @MockitoSpyBean
    private SmsService smsService;

    @Autowired
    private PnEcConfig pnEcConfig;

    private PnEcConfig.NotificationTracker.SqsQueue notificationTrackerSqsName() {
        return pnEcConfig.getNotificationTracker().getSqsQueue();
    }

    @MockitoSpyBean
    private SqsServiceImpl sqsService;

    @MockitoSpyBean
    private SnsService snsService;

    private static final SmsPresaInCaricoInfo SMS_PRESA_IN_CARICO_INFO = SmsPresaInCaricoInfo.builder()
            .requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalCourtesySmsRequest(createSmsRequest())
            .build();

    /**
     * <h3>SMSLR.107.1</h3>
     * <b>Precondizione:</b> Pull di un payload dalla coda "SMS"
     * <br>
     * <b>Passi aggiuntivi:</b> Invio SMS con SNS -> OK
     * <br>
     * <b>Risultato atteso:</b> Pubblicazione sulla coda Notification Tracker -> OK
     */
    @Test
    void lavorazioneRichiestaOk() {

        Mono<SendMessageResponse> response = smsService.lavorazioneRichiesta(SMS_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(smsService, times(1)).sendNotificationOnStatusQueue(eq(SMS_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    /**
     * <h3>SMSLR.107.3</h3>
     * <ul>
     *   <li><b>Precondizione:</b> Pull di un payload dalla coda "SMS"</li>
     *   <li><b>Passi aggiuntivi:</b>
     *     <ol>
     *       <li>Invio SMS con SNS -> OK</li>
     *       <li>Pubblicazione sulla coda Notification Tracker -> KO</li>
     *     </ol>
     *   </li>
     *   <li><b>Risultato atteso:</b> Pubblicazione sulla coda Errori SMS -> OK</li>
     *   <li><b>Note:</b> Il payload pubblicato sulla coda notificherà al flow di retry di riprovare solamente la pubblicazione sulla coda
     *   "Notification Tracker"
     *   </li>
     * </ul>
     */
    @Test
    void lavorazioneRichiestaNtKo() {

        when(sqsService.send(eq(notificationTrackerSqsName().getStatoSmsName()), any(NotificationTrackerQueueDto.class))).thenReturn(Mono.error(
                new SqsClientException(notificationTrackerSqsName().getStatoSmsName())));

        Mono<SendMessageResponse> response = smsService.lavorazioneRichiesta(SMS_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(smsService, times(1)).sendNotificationOnStatusQueue(eq(SMS_PRESA_IN_CARICO_INFO), eq(SENT.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        verify(smsService, times(1)).sendNotificationOnErrorQueue(eq(SMS_PRESA_IN_CARICO_INFO));
    }

    /**
     * <h3>SMSLR.107.3</h3>
     * <ul>
     *   <li><b>Precondizione:</b> Pull di un payload dalla coda "SMS"</li>
     *   <li><b>Passi aggiuntivi:</b>
     *     <ol>
     *       <li>Invio SMS con SNS -> KO</li>
     *       <li>Pubblicazione sulla coda Notification Tracker -> OK</li>
     *     </ol>
     *   </li>
     *   <li><b>Risultato atteso:</b> Pubblicazione sulla coda Errori SMS -> OK</li>
     *   <li><b>Note:</b> Il payload pubblicato sulla coda notificherà al flow di retry di riprovare solamente la pubblicazione sulla coda
     *   "Notification Tracker"
     *   </li>
     * </ul>
     */
    @Test
    void lavorazioneRichiestaSnsKo()
    {
        when(snsService.send(anyString(), anyString())).thenReturn(Mono.error(new SnsSendException()));

        Mono<SendMessageResponse> response = smsService.lavorazioneRichiesta(SMS_PRESA_IN_CARICO_INFO);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(smsService, times(1)).sendNotificationOnStatusQueue(eq(SMS_PRESA_IN_CARICO_INFO), eq(RETRY.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
        verify(smsService, times(1)).sendNotificationOnErrorQueue(eq(SMS_PRESA_IN_CARICO_INFO));
    }

    private static SmsPresaInCaricoInfo newSmsPresaInCaricoInfo() {
        return SmsPresaInCaricoInfo.builder()
                .requestIdx(DEFAULT_REQUEST_IDX)
                .xPagopaExtchCxId(DEFAULT_ID_CLIENT_HEADER_VALUE)
                .digitalCourtesySmsRequest(createSmsRequest())
                .build();
    }

    private DigitalProgressStatusDto captureStatusDto(SmsPresaInCaricoInfo info, Status status) {
        ArgumentCaptor<DigitalProgressStatusDto> captor = ArgumentCaptor.forClass(DigitalProgressStatusDto.class);
        verify(smsService, times(1)).sendNotificationOnStatusQueue(eq(info), eq(status.getStatusTransactionTableCompliant()), captor.capture());
        return captor.getValue();
    }

    @Test
    void lavorazioneRichiestaSnsKo_retryEventHasSanitizedErrorDetails() {
        SmsPresaInCaricoInfo info = newSmsPresaInCaricoInfo();
        when(snsService.send(anyString(), anyString())).thenReturn(Mono.error(new SnsSendException("sns provider failure for +393331234567")));

        StepVerifier.create(smsService.lavorazioneRichiesta(info)).expectNextCount(1).verifyComplete();

        String errorDetails = captureStatusDto(info, RETRY).getErrorDetails();
        assertNotNull(errorDetails);
        assertTrue(errorDetails.contains(LAVORAZIONE_RICHIESTA_SMS));
        assertTrue(errorDetails.contains("sns provider failure"));
        assertEquals(1, StringUtils.countMatches(errorDetails, LogSanitizer.DEFAULT_PHONE));
        assertFalse(errorDetails.contains("+39 000 +39"));
        assertFalse(errorDetails.contains("3331234567"));
        assertFalse(errorDetails.contains("\tat "));
    }

    @Test
    void lavorazioneRichiestaSnsKo_errorDetailsContainRetryExhaustedProcessAndSnsCause() {
        SmsPresaInCaricoInfo info = newSmsPresaInCaricoInfo();
        when(snsService.send(anyString(), anyString())).thenReturn(Mono.error(new SnsSendException("sns provider failure", new RuntimeException("throttling"))));

        StepVerifier.create(smsService.lavorazioneRichiesta(info)).expectNextCount(1).verifyComplete();

        String errorDetails = captureStatusDto(info, RETRY).getErrorDetails();
        assertNotNull(errorDetails);
        assertTrue(errorDetails.contains("SnsMaxRetriesExceededException"));
        assertTrue(errorDetails.contains("sns provider failure"));
    }

    @Test
    void lavorazioneRichiestaOk_sentEventHasNoErrorDetails() {
        SmsPresaInCaricoInfo info = newSmsPresaInCaricoInfo();

        StepVerifier.create(smsService.lavorazioneRichiesta(info)).expectNextCount(1).verifyComplete();

        assertNull(captureStatusDto(info, SENT).getErrorDetails());
    }

    @Test
    void lavorazioneRichiestaNtKo_doesNotPublishRetryNorErrorDetails() {
        SmsPresaInCaricoInfo info = newSmsPresaInCaricoInfo();
        when(sqsService.send(eq(notificationTrackerSqsName().getStatoSmsName()), any(NotificationTrackerQueueDto.class))).thenReturn(Mono.error(
                new SqsClientException(notificationTrackerSqsName().getStatoSmsName())));

        StepVerifier.create(smsService.lavorazioneRichiesta(info)).expectNextCount(1).verifyComplete();

        assertNull(captureStatusDto(info, SENT).getErrorDetails());
        verify(smsService, never()).sendNotificationOnStatusQueue(eq(info), eq(RETRY.getStatusTransactionTableCompliant()), any(DigitalProgressStatusDto.class));
    }

    @Test
    void lavorazioneRichiestaSnsKo_retryStrategyPropagatesLastFailureAsCause() {
        Retry retryStrategy = (Retry) ReflectionTestUtils.getField(SmsService.class, "LAVORAZIONE_RICHIESTA_RETRY_STRATEGY");
        SnsSendException lastFailure = new SnsSendException("sns provider failure");

        StepVerifier.withVirtualTime(() -> Mono.<String>error(lastFailure).retryWhen(retryStrategy))
                    .thenAwait(Duration.ofMinutes(5))
                    .expectErrorSatisfies(error -> {
                        assertInstanceOf(SnsSendException.SnsMaxRetriesExceededException.class, error);
                        assertSame(lastFailure, error.getCause());
                    })
                    .verify();
    }

}
