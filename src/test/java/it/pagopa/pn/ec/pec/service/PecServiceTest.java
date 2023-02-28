package it.pagopa.pn.ec.pec.service;

import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.pec.configurationproperties.PecSqsQueueName;
import it.pagopa.pn.ec.pec.model.pojo.PecField;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.pec.service.impl.PecService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static it.pagopa.pn.ec.pec.testutils.DigitalNotificationRequestFactory.createPecRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
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

//    ArubaService?

//    PublishResponse?

    private final ArgumentCaptor<NotificationTrackerQueueDto> trackerQueueDtoCaptor =
            ArgumentCaptor.forClass(NotificationTrackerQueueDto.class);

    private static final PecPresaInCaricoInfo PEC_PRESA_IN_CARICO_INFO =
            new PecPresaInCaricoInfo(DEFAULT_REQUEST_IDX, DEFAULT_ID_CLIENT_HEADER_VALUE, createPecRequest());

//    /**
//     * <h3>PECLR.100.1</h3>
//     *   <b>Precondizione:</b>
//     *     <ol>
//     *       <li>Pull payload from PEC Queue</li>
//     *       <li>Aruba is up</li>
//     *       <li>Connection with ss is up</li>
//     *       <li>Notification Tracker is up</li>
//     *     </ol>
//     *   <b>Passi aggiuntivi:</b>
//     *     <ol>
//     *       <li>Send request to Aruba (with attachments downloading)</li>
//     *     </ol>
//     *   <b>Risultato atteso:</b>Posting on Notification Tracker Queue --> ok</li>
//     */
//    @Test
//    void lavorazioneRichiestaOk() {
//        // TODO: Eliminare il mock una volta sistemato l'ambiente Localstack, togliere publish response
//        when(snsService.send(any(PecField.class)).thenReturn(Mono.just(PublishResponse.builder().build())));
//
//        StepVerifier.create(pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO)).expectNextCount(1).verifyComplete();
//
//        verify(snsService, times(1)).send(any(PecField.class));
//        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoSmsName()), trackerQueueDtoCaptor.capture());
//        assertEquals("sent", trackerQueueDtoCaptor.getValue().getNextStatus());
//    }
//
//    /**
//     * <h3>PECLR.100.2</h3>
//     *   <b>Precondizione:</b>
//     *     <ol>
//     *       <li>Pull payload from PEC Queue</li>
//     *       <li>Aruba is down</li>
//     *     </ol>
//     *   <b>Passi aggiuntivi:</b>
//     *     <ol>
//     *       <li>Send request to Aruba (ko) --> n° of retry allowed</li>
//     *     </ol>
//     *   <b>Risultato atteso:</b>Posting on Notification Tracker Queue --> ok</li>
//     */
//    @Test
//    void lavorazioneRichiestaOkWithRetry() {
//        when(snsService.send(any(PecField.class))).thenReturn(Mono.error(new SnsSendException()))
//                .thenReturn(Mono.just(PublishResponse.builder().build()));
//
//        StepVerifier.create(pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO)).expectNextCount(1).verifyComplete();
//
//        verify(snsService, times(2)).send(any(PecField.class));
//        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoSmsName()), any(NotificationTrackerQueueDto.class));
//    }
//
//    /**
//     * <h3>PECLR.100.3</h3>
//     *   <b>Precondizione:</b>
//     *     <ol>
//     *       <li>Pull payload from PEC Queue</li>
//     *       <li>Aruba is down</li>
//     *     </ol>
//     *   <b>Passi aggiuntivi:</b>
//     *     <ol>
//     *       <li>Send request to Aruba (ko) --> n° of retry > allowed</li>
//     *     </ol>
//     *   <b>Risultato atteso:</b>Posting on Error Queue --> ok</li>
//     */
//    @Test
//    void lavorazioneRichiestaArubaKo() {
//        boolean testImplemented = false;
//        assertTrue(testImplemented);
//    }
//
//    /**
//     * <h3>PECLR.100.4</h3>
//     *   <b>Precondizione:</b>
//     *     <ol>
//     *       <li>Pull payload from PEC Queue</li>
//     *       <li>Connection with ss is down</li>
//     *     </ol>
//     *   <b>Passi aggiuntivi:</b>
//     *     <ol>
//     *       <li>Error to recover attachment</li>
//     *     </ol>
//     *   <b>Risultato atteso:</b>Posting on Error Queue --> ok</li>
//     */
//    @Test
//    void lavorazioneRichiestaSsKo() {
//        boolean testImplemented = false;
//        assertTrue(testImplemented);
//    }
//
//    /**
//     * <h3>PECLR.100.6</h3>
//     *   <b>Precondizione:</b>
//     *     <ol>
//     *       <li>Pull payload from PEC Queue</li>
//     *       <li>Notification Tracker is down</li>
//     *     </ol>
//     *   <b>Passi aggiuntivi:</b>
//     *     <ol>
//     *       <li>Send request to Aruba (ok) --> posting on queue notification tracker (ko) --> n° of retry > allowed</li>
//     *     </ol>
//     *   <b>Risultato atteso:</b>Posting on Error Queue --> ok</li>
//     */
//    @Test
//    void lavorazioneRichiestaNtKo() {
//        // TODO: Eliminare il mock una volta sistemato l'ambiente Localstack, togliere publish response
//        when(snsService.send(any(PecField.class))).thenReturn(Mono.just(PublishResponse.builder().build()));
//
//        when(sqsService.send(eq(notificationTrackerSqsName.statoSmsName()), any(NotificationTrackerQueueDto.class))).thenReturn(Mono.error(
//                new SqsPublishException(notificationTrackerSqsName.statoSmsName())));
//
//        StepVerifier.create(pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO)).expectNextCount(1).verifyComplete();
//
//        verify(snsService, times(1)).send(any(PecField.class));
//        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoSmsName()), any(NotificationTrackerQueueDto.class));
//        verify(sqsService, times(1)).send(eq(pecSqsQueueName.errorName()), any(PecPresaInCaricoInfo.class));
//    }
//
//    @Test // da fare?
//    void testRetryPec() {
//        when(snsService.send(any(PecField.class))).thenReturn(Mono.error(new SnsSendException()));
//
//        StepVerifier.create(pecService.lavorazioneRichiesta(PEC_PRESA_IN_CARICO_INFO)).expectNextCount(1).verifyComplete();
//
//        verify(snsService, times(1)).send(any(PecField.class));
//        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoSmsName()), trackerQueueDtoCaptor.capture());
//        assertEquals("retry", trackerQueueDtoCaptor.getValue().getNextStatus());
//        verify(sqsService, times(1)).send(eq(pecSqsQueueName.errorName()), any(PecPresaInCaricoInfo.class));
//    }
//
//    @Test // da fare?
//    void testLavorazioneRichiesta() {
//        StepVerifier.create(TestPublisher.create()
//                        .next(SNS_DEFAULT_PUBLISH_RESPONSE)
//                        .mono()
//                        .retryWhen(DEFAULT_RETRY_STRATEGY))
//                .expectNext(SNS_DEFAULT_PUBLISH_RESPONSE)
//                .verifyComplete();
//    }


}
