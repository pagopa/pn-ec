package it.pagopa.pn.ec.pec.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.aruba.ArubaSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.aruba.ArubaCall;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.pec.configurationproperties.PecSqsQueueName;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.pec.service.impl.PecService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pec.bridgews.SendMail;
import it.pec.bridgews.SendMailResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import javax.mail.MessagingException;

import static it.pagopa.pn.ec.pec.testutils.DigitalNotificationRequestFactory.createPecRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class PecServiceTest {

    @Autowired
    private PecService pecService;

    @Autowired
    private PecSqsQueueName  pecSqsQueueName;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @SpyBean
    private SqsServiceImpl sqsService;

    @SpyBean
    private ArubaCall arubaCall;

    @Mock
    private Acknowledgment acknowledgment;

    private final ArgumentCaptor<NotificationTrackerQueueDto> trackerQueueDtoCaptor =
            ArgumentCaptor.forClass(NotificationTrackerQueueDto.class);

    private static final PecPresaInCaricoInfo PEC_PRESA_IN_CARICO_INFO =
            new PecPresaInCaricoInfo(DEFAULT_REQUEST_IDX, DEFAULT_ID_CLIENT_HEADER_VALUE, createPecRequest());

    /**
     * <h3>PECLR.100.1</h3>
     *   <b>Precondizione:</b>
     *     <ol>
     *       <li>Pull payload from PEC Queue</li>
     *       <li>Aruba is up</li>
     *       <li>Connection with ss is up</li>
     *       <li>Notification Tracker is up</li>
     *     </ol>

     *   <b>Passi aggiuntivi:</b>
     *     <ol>
     *       <li>Send request to Aruba (with attachments downloading)</li>
     *     </ol>
     *   <b>Risultato atteso:</b>Posting on Notification Tracker Queue --> ok</li>
     */
    @Test
    void lavorazioneRichiestaOk() throws MessagingException {
        SendMail sendMail = Mockito.mock(SendMail.class);
//        sendMail.setUser("TEST");
//        sendMail.setPass("123");
//        sendMail.setData("ashd");
        // TODO: Eliminare il mock una volta sistemato l'ambiente Localstack, togliere publish response
        when(arubaCall.sendMail(sendMail).thenReturn(Mono.just(SendMailResponse.class)));

        pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO, acknowledgment);


        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoPecName()), any(NotificationTrackerQueueDto.class));
    }

    /**
     * <h3>PECLR.100.2</h3>
     *   <b>Precondizione:</b>
     *     <ol>
     *       <li>Pull payload from PEC Queue</li>
     *       <li>Aruba is down</li>
     *     </ol>
     *   <b>Passi aggiuntivi:</b>
     *     <ol>
     *       <li>Send request to Aruba (ko) --> n° of retry allowed</li>
     *     </ol>
     *   <b>Risultato atteso:</b>Posting on Notification Tracker Queue --> ok</li>
     */
    @Test
    void lavorazioneRichiestaOkWithRetry() throws MessagingException {
        SendMail sendMail = new SendMail();
        sendMail.setUser("TEST");
        sendMail.setIp("");
        sendMail.setData("");
        sendMail.setPass("");
        when(arubaCall.sendMail(sendMail).thenReturn(Mono.error(new ArubaSendException()))
                                                    .thenReturn(Mono.just(SendMailResponse.class)));

        pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO, acknowledgment);

        verify(arubaCall, times(2)).sendMail(any(SendMail.class));
        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoPecName()), any(NotificationTrackerQueueDto.class));
    }

    /**
     * <h3>PECLR.100.3</h3>
     *   <b>Precondizione:</b>
     *     <ol>
     *       <li>Pull payload from PEC Queue</li>
     *       <li>Aruba is down</li>
     *     </ol>
     *   <b>Passi aggiuntivi:</b>
     *     <ol>
     *       <li>Send request to Aruba (ko) --> n° of retry > allowed</li>
     *     </ol>
     *   <b>Risultato atteso:</b>Posting on Error Queue --> ok</li>
     */
    @Test
    void lavorazioneRichiestaArubaKo() {
        boolean testImplemented = false;
        assertTrue(testImplemented);
    }

    /**
     * <h3>PECLR.100.4</h3>
     *   <b>Precondizione:</b>
     *     <ol>
     *       <li>Pull payload from PEC Queue</li>
     *       <li>Connection with ss is down</li>
     *     </ol>
     *   <b>Passi aggiuntivi:</b>
     *     <ol>
     *       <li>Error to recover attachment</li>
     *     </ol>
     *   <b>Risultato atteso:</b>Posting on Error Queue --> ok</li>
     */
    @Test
    void lavorazioneRichiestaSsKo() {
        boolean testImplemented = false;
        assertTrue(testImplemented);
    }

    /**
     * <h3>PECLR.100.6</h3>
     *   <b>Precondizione:</b>
     *     <ol>
     *       <li>Pull payload from PEC Queue</li>
     *       <li>Notification Tracker is down</li>
     *     </ol>
     *   <b>Passi aggiuntivi:</b>
     *     <ol>
     *       <li>Send request to Aruba (ok) --> posting on queue notification tracker (ko) --> n° of retry > allowed</li>
     *     </ol>
     *   <b>Risultato atteso:</b>Posting on Error Queue --> ok</li>
     */
    @Test
    void lavorazioneRichiestaNtKo() throws MessagingException {
        // TODO: Eliminare il mock una volta sistemato l'ambiente Localstack, togliere publish response
        when(arubaCall.sendMail(any(SendMail.class)).thenReturn(Mono.just(SendMailResponse.class)));

        when(sqsService.send(eq(notificationTrackerSqsName.statoPecName()), any(NotificationTrackerQueueDto.class))).thenReturn(Mono.error(
                new SqsPublishException(notificationTrackerSqsName.statoPecName())));

        pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO, acknowledgment);

        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoPecName()), any(NotificationTrackerQueueDto.class));
        verify(sqsService, times(1)).send(eq(pecSqsQueueName.errorName()), any(PecPresaInCaricoInfo.class));
    }

    @Test // da fare?
    void testRetryPec() throws MessagingException {
        when(arubaCall.sendMail(any(SendMail.class)).thenReturn(Mono.error(new ArubaSendException())));

        pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO, acknowledgment);

        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoPecName()), trackerQueueDtoCaptor.capture());
        assertEquals("retry", trackerQueueDtoCaptor.getValue().getNextStatus());
        verify(sqsService, times(1)).send(eq(pecSqsQueueName.errorName()), any(PecPresaInCaricoInfo.class));
    }

}
