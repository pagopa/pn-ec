package it.pagopa.pn.ec.sms.service;


import it.pagopa.pn.ec.sms.service.impl.SmsService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTestWebEnv
class SmsServiceTest {

    @Autowired
    private SmsService smsService;

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
