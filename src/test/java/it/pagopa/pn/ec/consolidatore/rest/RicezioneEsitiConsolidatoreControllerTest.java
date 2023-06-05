package it.pagopa.pn.ec.consolidatore.rest;

import static it.pagopa.pn.ec.consolidatore.utils.PaperElem.ATTACHMENT_DOCUMENT_TYPE_ARCAD;
import static it.pagopa.pn.ec.consolidatore.utils.PaperElem.CON010;
import static it.pagopa.pn.ec.consolidatore.utils.PaperElem.PRODUCT_TYPE_AR;
import static it.pagopa.pn.ec.consolidatore.utils.PaperElem.statusCodeDescriptionMap;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.AttachmentNotAvailableException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCallImpl;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
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
	private AuthService authService;
    @MockBean
    private GestoreRepositoryCallImpl gestoreRepositoryCall;
    @MockBean
    private FileCall fileCall;

	@MockBean
	private StatusPullService statusPullService;
	@SpyBean
	private SqsServiceImpl sqsService;
    
    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;
    
    private static final String RICEZIONE_ESITI_ENDPOINT = "/consolidatore-ingress/v1/push-progress-events/";
    
    private static final String xPagopaExtchServiceIdHeaderName =  "x-pagopa-extch-service-id";
    private static final String xApiKeyHeaderaName = "x-api-key";
    private static final String xPagopaExtchServiceIdHeaderValue = "IdClientX";
    private static final String xApiKeyHeaderValue = "ApiKeyX";
    
    private static final String SS_IN_URI = "safestorage://";
    
    // minLength: 30 maxLength: 250
    private static final String requestId = "123456789012345678901234567890";
    private static final OffsetDateTime now = OffsetDateTime.now();
    // minLength: 1 maxLength: 10
    private static final String attachmentId = "AttachmIdX";
    // minLength: 2 maxLength: 10
    private static final String documentType = ATTACHMENT_DOCUMENT_TYPE_ARCAD;
    private static final String documentKey = "docKeyX";
	private static final String CLIENT_ID = "CLIENT_ID";
	private static final String X_API_KEY = "X_API_KEY";
    private static final String uri = SS_IN_URI + documentKey;
    // minLength: 40 maxLength: 50
    private static final String sha256Id = "abcdefghilabcdefghilabcdefghilabcdefghil123";

	private static final String IUN = "abcdefghie";
	private static final ClientConfigurationInternalDto clientConfigurationInternalDto = new ClientConfigurationInternalDto();

	private static final String STATUS_CODE_INESISTENTE = "test";
    
    private ConsolidatoreIngressPaperProgressStatusEvent getProgressStatusEventWithoutAttachments() {
    	ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent = new ConsolidatoreIngressPaperProgressStatusEvent();
    	progressStatusEvent.setRequestId(requestId);
    	progressStatusEvent.setStatusCode(CON010);
    	progressStatusEvent.setStatusDescription(statusCodeDescriptionMap().get(CON010));
    	progressStatusEvent.setStatusDateTime(now);
    	progressStatusEvent.setProductType(PRODUCT_TYPE_AR);
		progressStatusEvent.setIun(IUN);
    	progressStatusEvent.setClientRequestTimeStamp(now);
    	return progressStatusEvent;
    }

	@BeforeAll
	public static void buildClientConfigurationInternalDto() {
		clientConfigurationInternalDto.setApiKey(xApiKeyHeaderValue);
		clientConfigurationInternalDto.setxPagopaExtchCxId(xPagopaExtchServiceIdHeaderValue);
	}
    
    private ConsolidatoreIngressPaperProgressStatusEvent getProgressStatusEventWithAttachments() {
    	ConsolidatoreIngressPaperProgressStatusEventAttachments attachment = new ConsolidatoreIngressPaperProgressStatusEventAttachments();
    	attachment.setId(attachmentId);	
    	attachment.setDocumentType(documentType);
    	attachment.setUri(uri);
    	attachment.setSha256(sha256Id);
    	attachment.setDate(now);
    	
    	List<ConsolidatoreIngressPaperProgressStatusEventAttachments> attachments = new ArrayList<>();
    	attachments.add(attachment);
    	
    	ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent = getProgressStatusEventWithoutAttachments();
    	progressStatusEvent.setAttachments(attachments);
    	return progressStatusEvent;
    }
    
    private RequestDto getRequestDto() {
    	RequestDto requestDto = new RequestDto();
    	requestDto.setRequestIdx(requestId);
		requestDto.setxPagopaExtchCxId(xPagopaExtchServiceIdHeaderValue);
    	return requestDto;
    }
    
    @Test
    /** Test CRCRE.100.1 */
    void ricezioneEsitiOk() {
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiOk() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
    	when(gestoreRepositoryCall.getRichiesta(xPagopaExtchServiceIdHeaderValue, requestId)).thenReturn(Mono.just(getRequestDto()));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
    	
    	FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
    	fileDownloadResponse.setKey(documentKey);
    	
    	when(fileCall.getFile(documentKey, xPagopaExtchServiceIdHeaderValue, true)).thenReturn(Mono.just(fileDownloadResponse));
    	
    	List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
    	events.add(getProgressStatusEventWithoutAttachments());
    	
        webClient.put()
	        .uri(RICEZIONE_ESITI_ENDPOINT)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .header(xPagopaExtchServiceIdHeaderName, xPagopaExtchServiceIdHeaderValue)
	        .header(xApiKeyHeaderaName, xApiKeyHeaderValue)
	        .body(BodyInserters.fromValue(events))
	        .exchange()
	        .expectStatus()
	        .isOk();
    }

	@Test
	/** Test CRCRE.100.1 */
	void ricezioneEsitiErroreValidazioneIun() {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiOk() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(xPagopaExtchServiceIdHeaderValue, requestId)).thenReturn(Mono.just(getRequestDto()));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun("DIFFERENT_IUN")));

		FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
		fileDownloadResponse.setKey(documentKey);

		when(fileCall.getFile(documentKey, xPagopaExtchServiceIdHeaderValue, true)).thenReturn(Mono.just(fileDownloadResponse));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithoutAttachments());

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(xPagopaExtchServiceIdHeaderName, xPagopaExtchServiceIdHeaderValue)
				.header(xApiKeyHeaderaName, xApiKeyHeaderValue)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

//	@Test
//	/** Test CRCRE.100.1 */
//	void ricezioneEsitiErroreValidazioneProductType() {
//		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiOk() : START");
//		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
//		when(gestoreRepositoryCall.getRichiesta(xPagopaExtchServiceIdHeaderValue, requestId)).thenReturn(Mono.just(getRequestDto()));
//		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType("DIFFERENT_PRODUCT_TYPE").iun(IUN)));
//
//		FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
//		fileDownloadResponse.setKey(documentKey);
//
//		when(fileCall.getFile(documentKey, xPagopaExtchServiceIdHeaderValue, true)).thenReturn(Mono.just(fileDownloadResponse));
//
//		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
//		events.add(getProgressStatusEventWithoutAttachments());
//
//		webClient.put()
//				.uri(RICEZIONE_ESITI_ENDPOINT)
//				.accept(APPLICATION_JSON)
//				.contentType(APPLICATION_JSON)
//				.header(xPagopaExtchServiceIdHeaderName, xPagopaExtchServiceIdHeaderValue)
//				.header(xApiKeyHeaderaName, xApiKeyHeaderValue)
//				.body(BodyInserters.fromValue(events))
//				.exchange()
//				.expectStatus()
//				.isBadRequest();
//	}

	@Test
    /** Test CRCRE.100.2 */
    void ricezioneEsitiErroreValidazioneIdRichiesta() {
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErroreValidazioneIdRichiesta() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
    	
    	when(gestoreRepositoryCall.getRichiesta(xPagopaExtchServiceIdHeaderValue, requestId)).thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));
    	
    	List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
    	events.add(getProgressStatusEventWithoutAttachments());
    	
        webClient.put()
	        .uri(RICEZIONE_ESITI_ENDPOINT)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .header(xPagopaExtchServiceIdHeaderName, xPagopaExtchServiceIdHeaderValue)
	        .header(xApiKeyHeaderaName, xApiKeyHeaderValue)
	        .body(BodyInserters.fromValue(events))
	        .exchange()
	        .expectStatus()
	        .isBadRequest();
    }
    
    @Test
    /** Test CRCRE.100.3 */
    void ricezioneEsitiErroreValidazioneStatusCode() {
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErroreValidazioneStatusCode() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
    	when(gestoreRepositoryCall.getRichiesta(xPagopaExtchServiceIdHeaderValue, requestId)).thenReturn(Mono.just(getRequestDto()));
    	
    	ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent = getProgressStatusEventWithoutAttachments();
    	progressStatusEvent.setStatusCode(STATUS_CODE_INESISTENTE);
    	
    	List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
    	events.add(progressStatusEvent);
    	
        webClient.put()
	        .uri(RICEZIONE_ESITI_ENDPOINT)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .header(xPagopaExtchServiceIdHeaderName, xPagopaExtchServiceIdHeaderValue)
	        .header(xApiKeyHeaderaName, xApiKeyHeaderValue)
	        .body(BodyInserters.fromValue(events))
	        .exchange()
	        .expectStatus()
	        .isBadRequest();
    }
    
    @Test
    /** Test CRCRE.100.4 */
    void ricezioneEsitiErroreValidazioneAttachments() {
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErroreValidazioneAttachments() : START");
    	
    	when(gestoreRepositoryCall.getRichiesta(xPagopaExtchServiceIdHeaderValue, requestId)).thenReturn(Mono.just(getRequestDto()));
    	
    	when(fileCall.getFile(documentKey, xPagopaExtchServiceIdHeaderValue, true))
    		.thenReturn(Mono.error(new AttachmentNotAvailableException(documentKey)));
    	
    	List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
    	events.add(getProgressStatusEventWithAttachments());
    	
        webClient.put()
	        .uri(RICEZIONE_ESITI_ENDPOINT)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .header(xPagopaExtchServiceIdHeaderName, xPagopaExtchServiceIdHeaderValue)
	        .header(xApiKeyHeaderaName, xApiKeyHeaderValue)
	        .body(BodyInserters.fromValue(events))
	        .exchange()
	        .expectStatus()
	        .isBadRequest();
    }
    
    @Test
    /** Test CRCRE.100.4 */
    void ricezioneEsitiErrorePubblicazioneCodaNotificationTracker() {
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErrorePubblicazioneCodaNotificationTracker() : START");
    	
    	when(gestoreRepositoryCall.getRichiesta(xPagopaExtchServiceIdHeaderValue, requestId)).thenReturn(Mono.just(getRequestDto()));
    	
    	FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
    	fileDownloadResponse.setKey(documentKey);
    	
    	when(fileCall.getFile(documentKey, xPagopaExtchServiceIdHeaderValue, true)).thenReturn(Mono.just(fileDownloadResponse));
    	
    	// errore pubblicazione su coda cartaceo
		when(sqsService.send(eq(notificationTrackerSqsName.statoCartaceoName()), any(NotificationTrackerQueueDto.class)))
			.thenReturn(Mono.error(new SqsClientException(notificationTrackerSqsName.statoCartaceoName())));
    	
    	List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
    	events.add(getProgressStatusEventWithoutAttachments());
    	events.add(getProgressStatusEventWithoutAttachments());
    	
        webClient.put()
        .uri(RICEZIONE_ESITI_ENDPOINT)
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .header(xPagopaExtchServiceIdHeaderName, xPagopaExtchServiceIdHeaderValue)
        .header(xApiKeyHeaderaName, xApiKeyHeaderValue)
        .body(BodyInserters.fromValue(events))
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
