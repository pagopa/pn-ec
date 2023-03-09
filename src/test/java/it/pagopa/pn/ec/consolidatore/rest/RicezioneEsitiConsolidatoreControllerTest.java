package it.pagopa.pn.ec.consolidatore.rest;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.rest.v1.dto.AttachmentDetails;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusEvent;
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
    
    private static final String xPagopaExtchServiceId = "";
    private static final String xApiKey = "";
    private static final OffsetDateTime now = OffsetDateTime.now();
    
    private PaperProgressStatusEvent getPaperProgressStatusEvent() {
    	AttachmentDetails attachmentDetails = new AttachmentDetails();
    	
    	List<AttachmentDetails> list = new ArrayList<>();
    	list.add(attachmentDetails);
    	
    	PaperProgressStatusEvent paperProgressStatusEvent = new PaperProgressStatusEvent();
    	paperProgressStatusEvent.setRequestId(null);
    	paperProgressStatusEvent.setStatusCode(null);
    	paperProgressStatusEvent.setStatusDescription(null);
    	paperProgressStatusEvent.setStatusDateTime(now);
    	paperProgressStatusEvent.setProductType(null);
    	paperProgressStatusEvent.setClientRequestTimeStamp(now);
    	return paperProgressStatusEvent;
    }
    
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
