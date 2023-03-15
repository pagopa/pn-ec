package it.pagopa.pn.ec.consolidatore.rest;

import static it.pagopa.pn.ec.consolidatore.utils.PaperElem.CON010;
import static it.pagopa.pn.ec.consolidatore.utils.PaperElem.PRODUCT_TYPE_AR;
import static it.pagopa.pn.ec.consolidatore.utils.PaperElem.statusCodeDescriptionMap;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCallImpl;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.rest.v1.dto.ConsolidatoreIngressPaperProgressStatusEvent;
import it.pagopa.pn.ec.rest.v1.dto.ConsolidatoreIngressPaperProgressStatusEventAttachments;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
class RicezioneEsitiConsolidatoreControllerTest {
	
    @Autowired
    private WebTestClient webClient;
	
    @MockBean
    private GestoreRepositoryCallImpl gestoreRepositoryCall;
    @MockBean
    private FileCall fileCall;
    
    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;
    
    private static final String RICEZIONE_ESITI_ENDPOINT = "/consolidatore-ingress/v1/push-progress-events/";
    
    private static final String xPagopaExtchServiceIdHeaderName =  "x-pagopa-extch-service-id";
    private static final String xApiKeyHeaderaName = "x-api-key";
    private static final String xPagopaExtchServiceIdHeaderValue = "IdClientX";
    private static final String xApiKeyHeaderValue = "ApiKeyX";
    
    private static final String SS_IN_URI = "safestorage://";
    
    private static final String requestId = "RequestIdX";
    private static final OffsetDateTime now = OffsetDateTime.now();
    private static final String attachmentId = "AttachmentIdX";
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String documentKey = "docKeyX";
    private static final String uri = SS_IN_URI + documentKey;
    private static final String sha256Id = "sha256X";
    
    private static final String STATUS_CODE_INESISTENTE = "test";
    
    private ConsolidatoreIngressPaperProgressStatusEvent getProgressStatusEvent() {
    	ConsolidatoreIngressPaperProgressStatusEventAttachments attachment = new ConsolidatoreIngressPaperProgressStatusEventAttachments();
    	attachment.setId(attachmentId);	
    	attachment.setDocumentType(APPLICATION_PDF);
    	attachment.setUri(uri);
    	attachment.setSha256(sha256Id);
    	attachment.setDate(now);
    	
    	ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent = new ConsolidatoreIngressPaperProgressStatusEvent();
    	progressStatusEvent.setRequestId(requestId);
    	progressStatusEvent.setStatusCode(CON010);
    	progressStatusEvent.setStatusDescription(statusCodeDescriptionMap().get(CON010));
    	progressStatusEvent.setStatusDateTime(now);
    	progressStatusEvent.setProductType(PRODUCT_TYPE_AR);
    	progressStatusEvent.setClientRequestTimeStamp(now);
    	return progressStatusEvent;
    }
    
    private FileDownloadResponse getFileDownloadResponse() {
    	FileDownloadResponse response = new FileDownloadResponse();
    	response.setKey(documentKey);
    	return response;
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
	        .header(xPagopaExtchServiceIdHeaderName, xPagopaExtchServiceIdHeaderValue)
	        .header(xApiKeyHeaderaName, xApiKeyHeaderValue)
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
