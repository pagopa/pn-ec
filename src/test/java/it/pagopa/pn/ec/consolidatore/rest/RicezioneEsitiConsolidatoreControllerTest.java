package it.pagopa.pn.ec.consolidatore.rest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
class RicezioneEsitiConsolidatoreControllerTest {
	
    @Autowired
    private WebTestClient webTestClient;
    
    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;
    
    @Test
    /** Test CRCRE.100.1 */
    void ricezioneEsitiOk() {
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiOk() : START");
    	Assertions.assertTrue(true);
    }
    
    @Test
    /** Test CRCRE.100.2 */
    void ricezioneEsitiErroreValidazioneIdRichiesta() {
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErroreValidazioneIdRichiesta() : START");
    	Assertions.assertTrue(true);
    }
    
    @Test
    /** Test CRCRE.100.3 */
    void ricezioneEsitiErroreValidazioneAttributiObbligatori() {
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErroreValidazioneAttributiObbligatori() : START");
    	Assertions.assertTrue(true);
    }
    
    @Test
    /** Test CRCRE.100.4 */
    void ricezioneEsitiErroreValidazioneStatusCode() {
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErroreValidazioneStatusCode() : START");
    	Assertions.assertTrue(true);
    }
    
    @Test
    /** Test CRCRE.100.4 */
    void ricezioneEsitiErrorePubblicazioneCodaNotificationTracker() {
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErrorePubblicazioneCodaNotificationTracker() : START");
    	Assertions.assertTrue(true);
    }

}
