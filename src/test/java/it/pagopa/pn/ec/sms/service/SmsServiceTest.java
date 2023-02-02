package it.pagopa.pn.ec.sms.service;


import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCallImpl;
import it.pagopa.pn.ec.commons.service.SnsService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static it.pagopa.pn.ec.commons.constant.ProcessId.INVIO_SMS;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.NT_STATO_SMS_QUEUE_NAME;
import static it.pagopa.pn.ec.commons.constant.status.CommonStatus.BOOKED;
import static it.pagopa.pn.ec.commons.constant.status.CommonStatus.SENT;
import static it.pagopa.pn.ec.sms.testutils.DigitalCourtesySmsRequestFactory.createSmsRequest;
import static org.mockito.Mockito.*;


@SpringBootTestWebEnv
@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @Autowired
    private SmsService smsService;

    @SpyBean
    private SqsServiceImpl sqsService;

    @SpyBean
    private SnsService snsService;

    @MockBean
    private GestoreRepositoryCallImpl gestoreRepositoryCall;

    private static final DigitalCourtesySmsRequest digitalCourtesySmsRequest = createSmsRequest();

    /**
     * <h3>SMSLR.107.1</h3>
     * <b>Precondizione:</b> Pull di un payload dalla coda "SMS"
     * <br>
     * <b>Passi aggiuntivi:</b> Invio SMS con SNS -> OK
     * <br>
     * <b>Risultato atteso:</b> Pubblicazione sulla coda Notification Tracker -> OK
     */
    @Test
    void lavorazioneRichiestaOk(){

        when(snsService.send(anyString(), anyString())).thenReturn(Mono.just(PublishResponse.builder().build()));
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.just(new RequestDto()));

        smsService.lavorazioneRichiesta(digitalCourtesySmsRequest);

        verify(snsService, times(1)).send(digitalCourtesySmsRequest.getReceiverDigitalAddress(), digitalCourtesySmsRequest.getMessageText());
        verify(gestoreRepositoryCall, times(1)).getRichiesta(digitalCourtesySmsRequest.getRequestId());
        verify(sqsService, times(1)).send(NT_STATO_SMS_QUEUE_NAME, new NotificationTrackerQueueDto(digitalCourtesySmsRequest.getRequestId(), null, INVIO_SMS, BOOKED, SENT));
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

    }

    /**
     * <h3>SMSLR.107.4</h3>
     * <ul>
     *   <li><b>Precondizione:</b> Pull di un payload dalla coda "SMS"</li>
     *   <li><b>Passi aggiuntivi:</b>
     *     <ol>
     *       <li>Invio SMS con SNS -> KO</li>
     *       <li>Numero di retry  maggiore dei max retry</li>
     *     </ol>
     *   </li>
     *   <li><b>Risultato atteso:</b> Pubblicazione sulla coda Errori SMS -> OK</li>
     *   <li><b>Note:</b> Il payload pubblicato sulla coda notificherà al flow di retry di riprovare l'invio di un SMS tramite SNS</li>
     * </ul>
     */
    @Test
    void lavorazioneRichiestaSnsKo() {

    }
}
