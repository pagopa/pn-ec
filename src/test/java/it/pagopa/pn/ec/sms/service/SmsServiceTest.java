package it.pagopa.pn.ec.sms.service;


import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.service.SnsService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.sms.configurationproperties.SmsSqsQueueName;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static it.pagopa.pn.ec.sms.testutils.DigitalCourtesySmsRequestFactory.createSmsRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.mockito.Mockito.*;


@SpringBootTestWebEnv
class SmsServiceTest {

    @Autowired
    private SmsService smsService;

    @Autowired
    private SmsSqsQueueName smsSqsQueueName;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @SpyBean
    private SqsServiceImpl sqsService;

    @SpyBean
    private SnsService snsService;

    private static final SmsPresaInCaricoInfo SMS_PRESA_IN_CARICO_INFO =
            new SmsPresaInCaricoInfo(DEFAULT_REQUEST_IDX, DEFAULT_ID_CLIENT_HEADER_VALUE, createSmsRequest());


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

        // TODO: Eliminare il mock una volta sistemato l'ambiente Localstack
        when(snsService.send(anyString(), anyString())).thenReturn(Mono.just(PublishResponse.builder().build()));

        smsService.lavorazioneRichiesta(SMS_PRESA_IN_CARICO_INFO);

        verify(snsService, times(1)).send(anyString(), anyString());
        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoSmsName()), any(NotificationTrackerQueueDto.class));
    }

    /**
     * <h3>SMSLR.107.2</h3>
     * <ul>
     *   <li><b>Precondizione:</b> Pull di un payload dalla coda "SMS"</li>
     *   <li><b>Passi aggiuntivi:</b>
     *     <ol>
     *       <li>Invio SMS con SNS -> KO</li>
     *       <li>Numero di retry minore dei max retry</li>
     *     </ol>
     *   </li>
     *   <li><b>Risultato atteso:</b> Pubblicazione sulla coda Notification Tracker -> OK</li>
     * </ul>
     */
    @Test
    void lavorazioneRichiestaOkWithRetry() {

        when(snsService.send(anyString(), anyString())).thenReturn(Mono.error(new SnsSendException()))
                                                       .thenReturn(Mono.just(PublishResponse.builder().build()));

        smsService.lavorazioneRichiesta(SMS_PRESA_IN_CARICO_INFO);

        verify(snsService, times(2)).send(anyString(), anyString());
        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoSmsName()), any(NotificationTrackerQueueDto.class));
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

        // TODO: Eliminare il mock una volta sistemato l'ambiente Localstack
        when(snsService.send(anyString(), anyString())).thenReturn(Mono.just(PublishResponse.builder().build()));

        when(sqsService.send(eq(notificationTrackerSqsName.statoSmsName()), any(NotificationTrackerQueueDto.class))).thenReturn(Mono.error(
                new SqsPublishException(notificationTrackerSqsName.statoSmsName())));

        smsService.lavorazioneRichiesta(SMS_PRESA_IN_CARICO_INFO);

        verify(snsService, times(1)).send(anyString(), anyString());
        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoSmsName()), any(NotificationTrackerQueueDto.class));
        verify(sqsService, times(1)).send(eq(smsSqsQueueName.errorName()), any(SmsPresaInCaricoInfo.class));
    }
}
