package it.pagopa.pn.ec.sms.service;


import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCallImpl;
import it.pagopa.pn.ec.commons.service.SnsService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static it.pagopa.pn.ec.commons.constant.ProcessId.INVIO_SMS;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.NT_STATO_SMS_QUEUE_NAME;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.SMS_ERROR_QUEUE_NAME;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestStatus.*;
import static it.pagopa.pn.ec.sms.testutils.DigitalCourtesySmsRequestFactory.createSmsRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.mockito.Mockito.*;


@SpringBootTestWebEnv
class SmsServiceTest {

    @Autowired
    private SmsService smsService;

    @SpyBean
    private SqsServiceImpl sqsService;

    @SpyBean
    private SnsService snsService;

    @MockBean
    private GestoreRepositoryCallImpl gestoreRepositoryCall;

    private static final SmsPresaInCaricoInfo SMS_PRESA_IN_CARICO_INFO =
            new SmsPresaInCaricoInfo(DEFAULT_REQUEST_IDX, DEFAULT_ID_CLIENT_HEADER_VALUE, createSmsRequest());

    private static final RequestDto REQUEST_IN_BOOKED_STATUS = new RequestDto();
    private static final RequestDto REQUEST_IN_RETRY_STATUS = new RequestDto();

    private static final DigitalCourtesySmsRequest DIGITAL_COURTESY_SMS_REQUEST = SMS_PRESA_IN_CARICO_INFO.getDigitalCourtesySmsRequest();

    private static final NotificationTrackerQueueDto NT_DTO_BOOKED_SENT =
            new NotificationTrackerQueueDto(SMS_PRESA_IN_CARICO_INFO.getRequestIdx(),
                                            SMS_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId(),
                                            INVIO_SMS,
                                            BOOKED.getValue(),
                                            SENT.getValue());

    private static final NotificationTrackerQueueDto NT_DTO_BOOKED_RETRY =
            new NotificationTrackerQueueDto(SMS_PRESA_IN_CARICO_INFO.getRequestIdx(),
                                            SMS_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId(),
                                            INVIO_SMS,
                                            BOOKED.getValue(),
                                            RETRY.getValue());

    private static final NotificationTrackerQueueDto NT_DTO_RETRY_SENT =
            new NotificationTrackerQueueDto(SMS_PRESA_IN_CARICO_INFO.getRequestIdx(),
                                            SMS_PRESA_IN_CARICO_INFO.getXPagopaExtchCxId(),
                                            INVIO_SMS,
                                            RETRY.getValue(),
                                            SENT.getValue());

    @BeforeAll
    public static void setRequestStatus() {
        REQUEST_IN_BOOKED_STATUS.setStatusRequest(BOOKED.getValue());
        REQUEST_IN_RETRY_STATUS.setStatusRequest(RETRY.getValue());
    }


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
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.just(REQUEST_IN_BOOKED_STATUS));

        smsService.lavorazioneRichiesta(SMS_PRESA_IN_CARICO_INFO);

        verify(gestoreRepositoryCall, times(1)).getRichiesta(SMS_PRESA_IN_CARICO_INFO.getRequestIdx());
        verify(sqsService, times(1)).send(NT_STATO_SMS_QUEUE_NAME, NT_DTO_BOOKED_SENT);
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
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.just(REQUEST_IN_BOOKED_STATUS));

        smsService.lavorazioneRichiesta(SMS_PRESA_IN_CARICO_INFO);

        verify(gestoreRepositoryCall, times(1)).getRichiesta(SMS_PRESA_IN_CARICO_INFO.getRequestIdx());
        verify(snsService, times(2)).send(DIGITAL_COURTESY_SMS_REQUEST.getReceiverDigitalAddress(),
                                          DIGITAL_COURTESY_SMS_REQUEST.getMessageText());
        verify(sqsService, times(1)).send(NT_STATO_SMS_QUEUE_NAME, NT_DTO_BOOKED_RETRY);
        verify(sqsService, times(1)).send(NT_STATO_SMS_QUEUE_NAME, NT_DTO_RETRY_SENT);
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
        when(gestoreRepositoryCall.getRichiesta(anyString())).thenReturn(Mono.just(REQUEST_IN_BOOKED_STATUS));
        when(sqsService.send(NT_STATO_SMS_QUEUE_NAME, NT_DTO_BOOKED_SENT)).thenReturn(Mono.error(new SqsPublishException(
                NT_STATO_SMS_QUEUE_NAME)));

        smsService.lavorazioneRichiesta(SMS_PRESA_IN_CARICO_INFO);

        verify(gestoreRepositoryCall, times(1)).getRichiesta(SMS_PRESA_IN_CARICO_INFO.getRequestIdx());
        verify(sqsService, times(1)).send(NT_STATO_SMS_QUEUE_NAME, NT_DTO_BOOKED_SENT);
        verify(sqsService, times(1)).send(SMS_ERROR_QUEUE_NAME, SMS_PRESA_IN_CARICO_INFO);
    }
}
