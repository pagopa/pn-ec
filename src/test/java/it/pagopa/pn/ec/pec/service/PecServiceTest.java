package it.pagopa.pn.ec.pec.service;

import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;

@SpringBootTestWebEnv
class PecServiceTest {

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
    void lavorazioneRichiestaOk() {
        boolean testImplemented = false;
        assertTrue(testImplemented);
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
    void lavorazioneRichiestaOkWithRetry() {
        boolean testImplemented = false;
        assertTrue(testImplemented);
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
    void lavorazioneRichiestaNtKo() {
        boolean testImplemented = false;
        assertTrue(testImplemented);
    }


}
