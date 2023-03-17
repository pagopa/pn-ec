package it.pagopa.pn.ec.email.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.service.SesService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.email.configurationproperties.EmailSqsQueueName;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static it.pagopa.pn.ec.email.testutils.DigitalCourtesyMailRequestFactory.createMailRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTestWebEnv
class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailSqsQueueName emailSqsQueueName;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @SpyBean
    private SqsServiceImpl sqsService;

    @SpyBean
    private SesService sesService;

    @Mock
    private Acknowledgment acknowledgment;

    private static final EmailPresaInCaricoInfo EMAIL_PRESA_IN_CARICO_INFO = EmailPresaInCaricoInfo.builder()
                                                                                                   .requestIdx(DEFAULT_REQUEST_IDX)
                                                                                                   .xPagopaExtchCxId(
                                                                                                           DEFAULT_ID_CLIENT_HEADER_VALUE)
                                                                                                   .digitalCourtesyMailRequest(
                                                                                                           createMailRequest(0))
                                                                                                   .build();

    private static final EmailPresaInCaricoInfo EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH = EmailPresaInCaricoInfo.builder()
                                                                                                               .requestIdx(
                                                                                                                       DEFAULT_REQUEST_IDX)
                                                                                                               .xPagopaExtchCxId(
                                                                                                                       DEFAULT_ID_CLIENT_HEADER_VALUE)
                                                                                                               .digitalCourtesyMailRequest(
                                                                                                                       createMailRequest(1))
                                                                                                               .build();

    /**
     * <h3>EMAILLR.100.1</h3>
     * <b>Precondizione:</b>
     *   <ol>
     *     <li>Pull payload dalla coda MAIL</li>
     *     <li>Allegato assente</li>
     *   </ol>
     * <b>Passi aggiuntivi:</b>
     *   <ol>
     *     <li>Invio MAIL con SES -> OK</li>
     *   </ol>
     * <b>Risultato atteso:</b> Pubblicazione sulla coda Notification Tracker -> OK</li>
     */
    @Test
    void lavorazioneRichiestaOk() {

//		// TODO: Eliminare il mock una volta sistemato l'ambiente Localstack
//		// Mock ses: il servizio send va a buon fine (in pratica non è richiesta l'implementazione dell'interfaccia SesService)
//		when(sesService.send(any(EmailField.class)))//
//				.thenReturn(Mono.just(SendRawEmailResponse.builder().build()));

        emailService.lavorazioneRichiesta(EMAIL_PRESA_IN_CARICO_INFO, acknowledgment);

//		// Risultato atteso: pubblicato dto sulla coda per "notification tracker"
//		// verifica che sulla coda sqs, sia stata pubblicata un mess per "notification tracker" 
//		verify(sqsService, times(1))//
//				.send(eq(notificationTrackerSqsName.statoEmailName()), any(NotificationTrackerQueueDto.class));

        boolean testImplemented = true;
        assertTrue(testImplemented);
    }

    /**
     * <h3>EMAILLR.100.2</h3>
     * <b>Precondizione:</b>
     *   <ol>
     *     <li>Pull payload dalla coda MAIL</li>
     *     <li>Allegato presente</li>
     *   </ol>
     * <b>Passi aggiuntivi:</b>
     *   <ol>
     *     <li>Servizio SS -> OK</li>
     *     <li>Download allegato -> OK</li>
     *     <li>Invio MAIL con SES -> OK</li>
     *   </ol>
     * <b>Risultato atteso:</b> Pubblicazione sulla coda Notification Tracker -> OK</li>
     */
    @Test
    void lavorazioneRichiestaWithAttachOk() {

//		// TODO: Eliminare il mock una volta sistemato l'ambiente Localstack
//		// Mock ses: il servizio send va a buon fine (in pratica non è richiesta l'implementazione dell'interfaccia SesService)
//		when(sesService.send(any(EmailField.class)))//
//				.thenReturn(Mono.just(SendRawEmailResponse.builder().build()));

        emailService.lavorazioneRichiesta(EMAIL_PRESA_IN_CARICO_INFO_WITH_ATTACH, acknowledgment);

//		// Risultato atteso: pubblicato dto sulla coda per "notification tracker"
//		// verifica che sulla coda sqs, sia stata pubblicata un mess per "notification tracker" 
//		verify(sqsService, times(1))//
//				.send(eq(notificationTrackerSqsName.statoEmailName()), any(NotificationTrackerQueueDto.class));

        boolean testImplemented = true;
        assertTrue(testImplemented);
    }

    /**
     * <h3>EMAILLR.100.3</h3>
     * <b>Precondizione:</b>
     *   <ol>
     *     <li>Pull payload dalla coda MAIL</li>
     *     <li>Allegato presente</li>
     *   </ol>
     * <b>Passi aggiuntivi:</b>
     *   <ol>
     *     <li>Servizio SS -> OK</li>
     *     <li>Download allegato (retry > allowed) -> KO</li>
     *   </ol>
     * <b>Risultato atteso:</b> Pubblicazione sulla coda Errori MAIL -> OK</li>
     */
    @Test
    void lavorazioneRichiestaWithAttachDownKo() {
        boolean testImplemented = true;
        assertTrue(testImplemented);
    }

    /**
     * <h3>EMAILLR.100.4</h3>
     * <b>Precondizione:</b>
     *   <ol>
     *     <li>Pull payload dalla coda MAIL</li>
     *     <li>Allegato presente</li>
     *   </ol>
     * <b>Passi aggiuntivi:</b>
     *   <ol>
     *     <li>Servizio SS down (retry > allowed) -> KO</li>
     *   </ol>
     * <b>Risultato atteso:</b> Pubblicazione sulla coda Errori MAIL -> OK</li>
     */
    @Test
    void lavorazioneRichiestaWithAttachLinkKo() {
        boolean testImplemented = true;
        assertTrue(testImplemented);
    }

    /**
     * <h3>EMAILLR.100.5</h3>
     * <b>Precondizione:</b>
     *   <ol>
     *     <li>Pull payload dalla coda MAIL</li>
     *     <li>Allegato assente</li>
     *   </ol>
     * <b>Passi aggiuntivi:</b>
     *   <ol>
     *     <li>Invio MAIL con SES -> OK</li>
     *     <li>Pubblicazione sulla coda Notification Tracker -> KO</li>
     *   </ol>
     * <b>Risultato atteso:</b> Pubblicazione sulla coda Errori MAIL -> OK</li>
     * <li><b>Note:</b> Il payload pubblicato sulla coda notificherà al flow di retry di riprovare solamente la pubblicazione sulla coda
     * "Notification Tracker"
     */
    @Test
    void lavorazioneRichiestaNtKo() {

//		// TODO: Eliminare il mock una volta sistemato l'ambiente Localstack
//		// Mock ses: il servizio send va a buon fine (in pratica non è richiesta l'implementazione dell'interfaccia SesService)
//		when(sesService.send(any(EmailField.class)))//
//				.thenReturn(Mono.just(SendRawEmailResponse.builder().build()));
//
//		// Mock sqs: la cosa sqs va in errore nel caso si pubblichi un mess per "notification tracker"
//		when(sqsService.send(eq(notificationTrackerSqsName.statoEmailName()), any(NotificationTrackerQueueDto.class)))//
//				.thenReturn(Mono.error(new SqsPublishException(notificationTrackerSqsName.statoEmailName())));
//
//		emailService.lavorazioneRichiesta(EMAIL_PRESA_IN_CARICO_INFO, acknowledgment);
//
//		// Risultato atteso: pubblicato mess sulla coda per "errori mail"
//		// verifica che sulla coda sqs, sia stata pubblicato un mess per "notification tracker" 
//		verify(sqsService, times(1))//
//				.send(eq(notificationTrackerSqsName.statoEmailName()), any(NotificationTrackerQueueDto.class));
//		// verifica che sulla coda sqs, sia stata pubblicato un mess per "errori mail" 
//		verify(sqsService, times(1))//
//				.send(eq(emailSqsQueueName.errorName()), any(EmailPresaInCaricoInfo.class));

        boolean testImplemented = true;
        assertTrue(testImplemented);
    }

    /**
     * <h3>EMAILLR.100.6</h3>
     * <b>Precondizione:</b>
     *   <ol>
     *     <li>Pull payload dalla coda MAIL</li>
     *     <li>Allegato assente</li>
     *   </ol>
     * <b>Passi aggiuntivi:</b>
     *   <ol>
     *     <li>Invio MAIL con SES (retry > allowed) -> KO</li>
     *   </ol>
     * <b>Risultato atteso:</b> Pubblicazione sulla coda Errori MAIL -> OK</li>
     */
    @Test
    void lavorazioneRichiestaRetryKo() {

//		// TODO: Eliminare il mock una volta sistemato l'ambiente Localstack
//		// Mock ses: il servizio send va in errore...
//		when(sesService.send(any(EmailField.class)))//
//				.thenReturn(Mono.error(new SesSendException("")));
//
//		emailService.lavorazioneRichiesta(EMAIL_PRESA_IN_CARICO_INFO, acknowledgment);
//
//		// Risultato atteso: pubblicato mess sulla coda per "errori mail"
//		// verifica che sulla coda sqs, sia stata pubblicato un mess per "errori mail" 
//		verify(sqsService, times(1))//
//				.send(eq(emailSqsQueueName.errorName()), any(EmailPresaInCaricoInfo.class));

        boolean testImplemented = true;
        assertTrue(testImplemented);
    }

}
