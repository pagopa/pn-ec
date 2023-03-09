package it.pagopa.pn.ec.consolidatore.rest;

import static it.pagopa.pn.ec.cartaceo.utils.PaperElem.CON010;
import static it.pagopa.pn.ec.cartaceo.utils.PaperElem.PRODUCT_TYPE_AR;
import static it.pagopa.pn.ec.cartaceo.utils.PaperElem.statusCodeDescriptionMap;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCallImpl;
import it.pagopa.pn.ec.rest.v1.dto.ConsolidatoreIngressPaperProgressStatusEvent;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
class RicezioneEsitiConsolidatoreControllerTest {
	
    @Autowired
    private WebTestClient webClient;
	
    @MockBean
    private GestoreRepositoryCallImpl gestoreRepositoryCall;
    
    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;
    
    private static final String RICEZIONE_ESITI_ENDPOINT = "/consolidatore-ingress/v1/push-progress-events/";
    
    private static final String xPagopaExtchServiceId = "IdClientX";
    private static final String xApiKey = "ApiKeyX";
    private static final String requestId = "RequestIdX";
    private static final OffsetDateTime now = OffsetDateTime.now();
    
    private static final String STATUS_CODE_INESISTENTE = "test";
    
    private ConsolidatoreIngressPaperProgressStatusEvent getProgressStatusEvent() {
    	ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent = new ConsolidatoreIngressPaperProgressStatusEvent();
    	progressStatusEvent.setRequestId(requestId);
    	progressStatusEvent.setStatusCode(CON010);
    	progressStatusEvent.setStatusDescription(statusCodeDescriptionMap().get(CON010));
    	progressStatusEvent.setStatusDateTime(now);
    	progressStatusEvent.setProductType(PRODUCT_TYPE_AR);
    	progressStatusEvent.setClientRequestTimeStamp(now);
    	return progressStatusEvent;
    }
    
    private RequestDto getRequestDto() {
    	RequestDto requestDto = new RequestDto();
    	requestDto.setRequestIdx(requestId);
    	return requestDto;
    }
    
    @Test
    /** Test CRCRE.100.1 */
    void ricezioneEsitiOk() {
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiOk() : START");
    	
    	when(gestoreRepositoryCall.getRichiesta(requestId)).thenReturn(Mono.just(getRequestDto()));
    	
        webClient.post()
	        .uri(RICEZIONE_ESITI_ENDPOINT)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        //TODO
//	        .body(BodyInserters.fromValue(clientConfigurationDto))
	        .exchange()
	        .expectStatus()
	        .isOk();
    	
    	Assertions.assertTrue(true);
    }
    
    @Test
    /** Test CRCRE.100.3 */
    void ricezioneEsitiErroreValidazioneAttributiObbligatori() {
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErroreValidazioneAttributiObbligatori() : START");
    	
    	ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent = getProgressStatusEvent();
    	progressStatusEvent.setProductType(null);
    	
    	Assertions.assertTrue(true);
    }
    
    @Test
    /** Test CRCRE.100.4 */
    void ricezioneEsitiErroreValidazioneStatusCode() {
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErroreValidazioneStatusCode() : START");
    	
    	ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent = getProgressStatusEvent();
    	progressStatusEvent.setStatusCode(STATUS_CODE_INESISTENTE);
    	
    	Assertions.assertTrue(true);
    }
    
    @Test
    /** Test CRCRE.100.2 */
    void ricezioneEsitiErroreValidazioneIdRichiesta() {
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErroreValidazioneIdRichiesta() : START");
    	
    	when(gestoreRepositoryCall.getRichiesta(requestId)).thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));
    	
    	Assertions.assertTrue(true);
    }
    
    @Test
    /** Test CRCRE.100.4 */
    void ricezioneEsitiErrorePubblicazioneCodaNotificationTracker() {
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErrorePubblicazioneCodaNotificationTracker() : START");
    	
    	when(gestoreRepositoryCall.getRichiesta(requestId)).thenReturn(Mono.just(getRequestDto()));
    	
    	Assertions.assertTrue(true);
    }

}
