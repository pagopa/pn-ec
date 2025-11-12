package it.pagopa.pn.ec.consolidatore.rest;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.consolidatore.utils.PaperElem.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.pagopa.pn.ec.commons.exception.StatusNotFoundException;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic400ErrorException;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.consolidatore.service.impl.RicezioneEsitiCartaceoServiceImpl;
import it.pagopa.pn.ec.consolidatore.utils.PaperElem;
import it.pagopa.pn.ec.rest.v1.dto.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
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
import lombok.CustomLog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.hamcrest.Matchers;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@CustomLog
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
	@SpyBean
	private RicezioneEsitiCartaceoServiceImpl ricezioneEsitiCartaceoServiceImpl;

    private static final String RICEZIONE_ESITI_ENDPOINT = "/consolidatore-ingress/v1/push-progress-events/";

    private static final String X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME =  "x-pagopa-extch-service-id";
    private static final String X_API_KEY_HEADER_NAME = "x-api-key";
    private static final String X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE = "IdClientX";
    private static final String X_API_KEY_HEADER_VALUE = "ApiKeyX";

    private static final String SS_IN_URI = "safestorage://";

    // minLength: 30 maxLength: 250
    private static final String REQUEST_ID = "123456789012345678901234567890";
    private static final OffsetDateTime NOW = OffsetDateTime.now();
    // minLength: 1 maxLength: 10
    private static final String ATTACHMENT_ID = "AttachmIdX";
    // minLength: 2 maxLength: 10
    private static final String DOCUMENT_TYPE = ATTACHMENT_DOCUMENT_TYPE_ARCAD;
    private static final String DOCUMENT_KEY = "docKeyX";
    private static final String URI = SS_IN_URI + DOCUMENT_KEY;
    // minLength: 40 maxLength: 50
    private static final String SHA_256_ID = "abcdefghilabcdefghilabcdefghilabcdefghil123";

	private static final String IUN = "abcdefghie";
	private static final ClientConfigurationInternalDto clientConfigurationInternalDto = new ClientConfigurationInternalDto();

	private static final String STATUS_CODE_INESISTENTE = "test";

	private static final String DELIVERY_FAILURE_CAUSE_OK = "M03";
	private static final String DELIVERY_FAILURE_CAUSE_INVALID = "M05";
	private static final String DELIVERY_FAILURE_CAUSE_KO = "KO";
	private static final EventsDto SENT_EVENT = new EventsDto().paperProgrStatus(new PaperProgressStatusDto().status(SENT.getStatusTransactionTableCompliant()).statusDateTime(NOW));
	private static final EventsDto BOOKED_EVENT = new EventsDto().paperProgrStatus(new PaperProgressStatusDto().status(BOOKED.getStatusTransactionTableCompliant()).statusDateTime(NOW));
	private static final EventsDto RETRY_EVENT = new EventsDto().paperProgrStatus(new PaperProgressStatusDto().status(RETRY.getStatusTransactionTableCompliant()).statusDateTime(NOW));
	private Duration defaultOffsetDuration;

    private ConsolidatoreIngressPaperProgressStatusEvent getProgressStatusEventWithoutAttachments() {
    	ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent = new ConsolidatoreIngressPaperProgressStatusEvent();
    	progressStatusEvent.setRequestId(REQUEST_ID);
    	progressStatusEvent.setStatusCode(CON010);
    	progressStatusEvent.setStatusDescription(statusCodeDescriptionMap().get(CON010));
    	progressStatusEvent.setStatusDateTime(NOW);
    	progressStatusEvent.setProductType(PRODUCT_TYPE_AR);
		progressStatusEvent.setIun(IUN);
    	progressStatusEvent.setClientRequestTimeStamp(NOW);
		progressStatusEvent.setCourier("recapitista1");
    	return progressStatusEvent;
    }

	private ConsolidatoreIngressPaperProgressStatusEvent getProgressStatusEvent(String deliveryFailureCause) {
		ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent = new ConsolidatoreIngressPaperProgressStatusEvent();
		progressStatusEvent.setRequestId(REQUEST_ID);
		progressStatusEvent.setStatusCode(RECRN006);
		progressStatusEvent.setStatusDescription(statusCodeDescriptionMap().get(RECRN006));
		progressStatusEvent.setStatusDateTime(NOW);
		progressStatusEvent.setProductType(PRODUCT_TYPE_AR);
		progressStatusEvent.setIun(IUN);
		progressStatusEvent.setClientRequestTimeStamp(NOW);
		progressStatusEvent.setDeliveryFailureCause(deliveryFailureCause);
		progressStatusEvent.setCourier("recapitista2");
		return progressStatusEvent;
	}

	private ConsolidatoreIngressPaperProgressStatusEvent getProgressStatusEventWithoutAttachmentsNullIUN() {
		ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent = new ConsolidatoreIngressPaperProgressStatusEvent();
		progressStatusEvent.setRequestId(REQUEST_ID);
		progressStatusEvent.setStatusCode(CON010);
		progressStatusEvent.setStatusDescription(statusCodeDescriptionMap().get(CON010));
		progressStatusEvent.setStatusDateTime(NOW);
		progressStatusEvent.setProductType(PRODUCT_TYPE_AR);
		progressStatusEvent.setIun(null);
		progressStatusEvent.setClientRequestTimeStamp(NOW);
		progressStatusEvent.setCourier("recapitista3");
		return progressStatusEvent;
	}

	@BeforeAll
	static void buildClientConfigurationInternalDto() {
		clientConfigurationInternalDto.setApiKey(X_API_KEY_HEADER_VALUE);
		clientConfigurationInternalDto.setxPagopaExtchCxId(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE);
	}

	@BeforeEach
	void beforeEach() {
		this.defaultOffsetDuration = (Duration) ReflectionTestUtils.getField(ricezioneEsitiCartaceoServiceImpl, "offsetDuration");
	}

	@AfterEach
	void  afterEach() {
		ReflectionTestUtils.setField(ricezioneEsitiCartaceoServiceImpl, "offsetDuration", defaultOffsetDuration);
	}

	private ConsolidatoreIngressPaperProgressStatusEvent consolidatoreIngressPaperProgressStatusEventWithAttachmentsAndRecCode(String code, String documentType){
		ConsolidatoreIngressPaperProgressStatusEvent event = getProgressStatusEventWithAttachments();
		code = code == null || code.isEmpty() ? PaperElem.RECAG003E: code;
		event.setStatusCode(code);
		event.getAttachments().get(0).setDocumentType(
				documentType == null || documentType.isEmpty() ? ATTACHMENT_DOCUMENT_TYPE_ARCAD : documentType);
		return event;
	}

	private ConsolidatoreIngressPaperProgressStatusEvent getProgressStatusEventWithAttachments() {
    	ConsolidatoreIngressPaperProgressStatusEventAttachments attachment = new ConsolidatoreIngressPaperProgressStatusEventAttachments();
    	attachment.setId(ATTACHMENT_ID);
    	attachment.setDocumentType(DOCUMENT_TYPE);
    	attachment.setUri(URI);
    	attachment.setSha256(SHA_256_ID);
    	attachment.setDate(NOW);

    	List<ConsolidatoreIngressPaperProgressStatusEventAttachments> attachments = new ArrayList<>();
    	attachments.add(attachment);

    	ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent = getProgressStatusEventWithoutAttachments();
    	progressStatusEvent.setAttachments(attachments);
    	return progressStatusEvent;
    }

	private ConsolidatoreIngressPaperProgressStatusEvent getProgressStatusEventWithInvalidAttachmentUri() {
		ConsolidatoreIngressPaperProgressStatusEventAttachments attachment = new ConsolidatoreIngressPaperProgressStatusEventAttachments();
		attachment.setId(ATTACHMENT_ID);
		attachment.setDocumentType(DOCUMENT_TYPE);
		attachment.setUri("invalidUri");
		attachment.setSha256(SHA_256_ID);
		attachment.setDate(NOW);

		List<ConsolidatoreIngressPaperProgressStatusEventAttachments> attachments = new ArrayList<>();
		attachments.add(attachment);

		ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent = getProgressStatusEventWithoutAttachments();
		progressStatusEvent.setAttachments(attachments);
		return progressStatusEvent;
	}

	private RequestDto getRequestDto(EventsDto... eventsDtos) {
		return new RequestDto().requestIdx(REQUEST_ID)
				.xPagopaExtchCxId(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.requestMetadata(new RequestMetadataDto().eventsList(List.of(eventsDtos))
														.paperRequestMetadata(new PaperRequestMetadataDto()));
	}

	private Stream<Arguments> provideArguments() {
		return Stream.of(Arguments.of(getProgressStatusEventWithAttachments()), Arguments.of(getProgressStatusEventWithoutAttachments()),
				Arguments.of(getProgressStatusEventWithoutAttachmentsNullIUN()));
	}

	@ParameterizedTest
	@MethodSource("provideArguments")
	void ricezioneEsitiOk(ConsolidatoreIngressPaperProgressStatusEvent consolidatoreIngressPaperProgressStatusEvent) {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiOk() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));

		FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
		fileDownloadResponse.setKey(DOCUMENT_KEY);

		when(fileCall.getFile(DOCUMENT_KEY, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, true)).thenReturn(Mono.just(fileDownloadResponse));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(consolidatoreIngressPaperProgressStatusEvent);

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isOk();
	}

	@Test
	void ricezioneEsitiInvalidAttachmentUri() {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiOk() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
		fileDownloadResponse.setKey(DOCUMENT_KEY);

		when(fileCall.getFile(DOCUMENT_KEY, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, true)).thenReturn(Mono.just(fileDownloadResponse));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithInvalidAttachmentUri());

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@Test
	void ricezioneEsitiAttachmentNotAvailable() {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiOk() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		when(fileCall.getFile(DOCUMENT_KEY, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, true)).thenReturn(Mono.error(new AttachmentNotAvailableException(DOCUMENT_KEY)));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithAttachments());

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@Test
	void ricezioneEsitiAttachmentGeneric400() {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiOk() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		when(fileCall.getFile(DOCUMENT_KEY, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, true))
		 .thenReturn(Mono.error(new Generic400ErrorException("Chiamata a safestorage non valida", "Resource is no longer available. It may have been removed or deleted.")));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithAttachments());

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@Test
	void ricezioneEsitiInternalError() {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiOk() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.error(new RuntimeException()));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithAttachments());

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.is5xxServerError();
	}

	@Test
	/** Test CRCRE.100.1 */
	void ricezioneEsitierroreStatusDecode() {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiOk() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.error(new StatusNotFoundException("status")));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
		fileDownloadResponse.setKey(DOCUMENT_KEY);

		when(fileCall.getFile(DOCUMENT_KEY, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, true)).thenReturn(Mono.just(fileDownloadResponse));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithoutAttachments());

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@Test
	/** Test CRCRE.100.1 */
	void ricezioneEsitiErroreValidazioneIun() {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiOk() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun("DIFFERENT_IUN")));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
		fileDownloadResponse.setKey(DOCUMENT_KEY);

		when(fileCall.getFile(DOCUMENT_KEY, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, true)).thenReturn(Mono.just(fileDownloadResponse));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithoutAttachments());

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@Test
	/** Test CRCRE.100.1 */
	void ricezioneEsitiErroreValidazioneStatusDateTime() {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiOk() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		EventsDto badSentEvent = new EventsDto().paperProgrStatus(new PaperProgressStatusDto().status(SENT.getStatusTransactionTableCompliant()).statusDateTime(NOW.plusDays(1)));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(badSentEvent)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
		fileDownloadResponse.setKey(DOCUMENT_KEY);

		when(fileCall.getFile(DOCUMENT_KEY, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, true)).thenReturn(Mono.just(fileDownloadResponse));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithoutAttachments());

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@Test
    /** Test CRCRE.100.2 */
    void ricezioneEsitiErroreValidazioneIdRichiesta() {
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErroreValidazioneIdRichiesta() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));

    	List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
    	events.add(getProgressStatusEventWithoutAttachments());

        webClient.put()
	        .uri(RICEZIONE_ESITI_ENDPOINT)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
	        .header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
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
    	when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent = getProgressStatusEventWithoutAttachments();
    	progressStatusEvent.setStatusCode(STATUS_CODE_INESISTENTE);

    	List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
    	events.add(progressStatusEvent);

        webClient.put()
	        .uri(RICEZIONE_ESITI_ENDPOINT)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
	        .header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
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
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));

    	when(fileCall.getFile(DOCUMENT_KEY, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, true))
    		.thenReturn(Mono.error(new AttachmentNotAvailableException(DOCUMENT_KEY)));

    	List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
    	events.add(getProgressStatusEventWithAttachments());

        webClient.put()
	        .uri(RICEZIONE_ESITI_ENDPOINT)
	        .accept(APPLICATION_JSON)
	        .contentType(APPLICATION_JSON)
	        .header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
	        .header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
	        .body(BodyInserters.fromValue(events))
	        .exchange()
	        .expectStatus()
	        .isBadRequest();
    }


    @Test
    /** Test CRCRE.100.4 */
    void ricezioneEsitiErrorePubblicazioneCodaNotificationTracker() {
    	log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErrorePubblicazioneCodaNotificationTracker() : START");

    	when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));

    	FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
    	fileDownloadResponse.setKey(DOCUMENT_KEY);

    	when(fileCall.getFile(DOCUMENT_KEY, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, true)).thenReturn(Mono.just(fileDownloadResponse));

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
        .header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
        .header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
        .body(BodyInserters.fromValue(events))
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }


	@Test
	void ricezioneEsitiWithRecCodeAndAttachments(){
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiWithRecCodeAndInvalidAttachment() : START");

		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		when(sqsService.send(eq(notificationTrackerSqsName.statoCartaceoName()), any(NotificationTrackerQueueDto.class)))
				.thenReturn(Mono.error(new SqsClientException(notificationTrackerSqsName.statoCartaceoName())));


		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(consolidatoreIngressPaperProgressStatusEventWithAttachmentsAndRecCode(PaperElem.RECAG003E,"INVALID"));

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isEqualTo(HttpStatus.BAD_REQUEST);

	}

	@Test
	/** Test PN-8187 */
	void ricezioneEsitiErroreValidazioneSyntaxError() {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErroreValidazioneStatusCode() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));

		ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent = getProgressStatusEventWithoutAttachments();
		progressStatusEvent.setStatusCode(STATUS_CODE_INESISTENTE);
		progressStatusEvent.setDiscoveredAddress(new ConsolidatoreIngressPaperProgressStatusEventDiscoveredAddress());

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(progressStatusEvent);

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
				.expectBody(OperationResultCodeResponse.class)
				.value(OperationResultCodeResponse::getErrorList, Matchers.hasItem(Matchers.containsString("'discoveredAddress.address': rejected value [null]")));

	}

	@Test
	void ricezioneEsitiErroreValidazioneDeliveryFailureCauseNotInStatusCodeShouldBeAddedToErrorList() {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErroreValidazioneStatusCode() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
		fileDownloadResponse.setKey(DOCUMENT_KEY);

		when(fileCall.getFile(DOCUMENT_KEY, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, true)).thenReturn(Mono.just(fileDownloadResponse));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEvent(DELIVERY_FAILURE_CAUSE_INVALID));

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isBadRequest()
				.expectBody(OperationResultCodeResponse.class)
				.value(OperationResultCodeResponse::getErrorList, Matchers.hasItem(Matchers.containsString(DELIVERY_FAILURE_CAUSE_INVALID)));
	}


	@Test
	void ricezioneEsitiErroreValidazioneDeliveryFailureCauseNotInMapdBeAddedToErrorList() {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErroreValidazioneStatusCode() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
		fileDownloadResponse.setKey(DOCUMENT_KEY);

		when(fileCall.getFile(DOCUMENT_KEY, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, true)).thenReturn(Mono.just(fileDownloadResponse));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEvent(DELIVERY_FAILURE_CAUSE_KO));

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isBadRequest()
				.expectBody(OperationResultCodeResponse.class)
				.value(OperationResultCodeResponse::getErrorList, Matchers.hasItem(Matchers.containsString(DELIVERY_FAILURE_CAUSE_KO)));
	}

	@Test
	void ricezioneEsitiErroreValidazioneDeliveryFailureCauseInStatusCodeShouldNotBeAddedToErrorList() {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErroreValidazioneStatusCode() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEvent(DELIVERY_FAILURE_CAUSE_OK));

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isOk();
	}

	@Test
	void ricezioneEsitiErroreValidazioneDeliveryWithIncorrectStatusCodeShouldBeAddedToErrorList() {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiErroreValidazioneStatusCode() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());
		ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent = getProgressStatusEventWithoutAttachments();
		progressStatusEvent.setStatusCode(STATUS_CODE_INESISTENTE);
		progressStatusEvent.setCourier("recapitista4");

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(progressStatusEvent);

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isBadRequest()
				.expectBody(OperationResultCodeResponse.class)
				.value(OperationResultCodeResponse::getErrorList, Matchers.hasItem(Matchers.containsString("test")));
	}

	@Test
	void ricezioneEsitiWithRetryStatusShouldThrowException() {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiWithRetryStatusShouldThrowException() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(RETRY_EVENT,RETRY_EVENT)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithoutAttachments());


		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@Test
	void ricezioneEsitiWithBookeAndSentdEventsShouldReturnOk() {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiWithBookedAndSentEventsShouldReturnOk() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(BOOKED_EVENT,SENT_EVENT)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithoutAttachments());

		webClient.put()
			.uri(RICEZIONE_ESITI_ENDPOINT)
			.accept(APPLICATION_JSON)
			.contentType(APPLICATION_JSON)
			.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
			.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
			.body(BodyInserters.fromValue(events))
			.exchange()
			.expectStatus()
			.isOk();
	}

	@Test
	void ricezioneEsitiWithBookedAndRetryEventsShouldReturnOk(){
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiWithBookedAndRetryEventsShouldReturnOk() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(BOOKED_EVENT,RETRY_EVENT)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithoutAttachments());

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isOk();
	}

	@Test
	void ricezioneEsitiWithStatusDateTimeBeforeSentEventDateTimeShouldThrowException(){
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiWithStatusDateTimeBeforeSentEventDateTimeShouldThrowException() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		EventsDto bookedEvent = new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
				.status(BOOKED.getStatusTransactionTableCompliant())
				.statusDateTime(OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC)));
		EventsDto sentEvent = new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
				.status(SENT.getStatusTransactionTableCompliant())
				.statusDateTime(OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC)));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(bookedEvent,sentEvent)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithoutAttachments().statusDateTime(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)));

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@Test
	void ricezioneEsitiWithStatusDateTimeBeforeBookedEventDateTimeShouldThrowException(){
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiWithStatusDateTimeBeforeBookedEventDateTimeShouldThrowException() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		EventsDto bookedEvent = new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
				.status(BOOKED.getStatusTransactionTableCompliant())
				.statusDateTime(OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC)));
		EventsDto retryEvent = new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
				.status(RETRY.getStatusTransactionTableCompliant())
				.statusDateTime(OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC)));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(bookedEvent,retryEvent)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithoutAttachments().statusDateTime(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)));

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@Test
	void ricezioneEsitiWithStatusDateTimeAfterSentEventDateTimeShouldReturnOk(){
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiWithStatusDateTimeAfterSentEventDateTimeShouldReturnOk() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		EventsDto bookedEvent = new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
				.status(BOOKED.getStatusTransactionTableCompliant())
				.statusDateTime(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)));
		EventsDto sentEvent = new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
				.status(SENT.getStatusTransactionTableCompliant())
				.statusDateTime(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(bookedEvent,sentEvent)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithoutAttachments().statusDateTime(OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC)));

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isOk();
	}

	@Test
	void ricezioneEsitiWithStatusDateTimeAfterBookedEventDateTimeShouldReturnOk(){
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiWithStatusDateTimeAfterBookedEventDateTimeShouldReturnOk() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		EventsDto bookedEvent = new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
				.status(BOOKED.getStatusTransactionTableCompliant())
				.statusDateTime(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)));
		EventsDto retryEvent = new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
				.status(RETRY.getStatusTransactionTableCompliant())
				.statusDateTime(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(bookedEvent,retryEvent)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithoutAttachments().statusDateTime(OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC)));

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isOk();
	}

	//con evento duplicato:
	// configurazione globale non attiva -> verifichiamo che non ci sia chiamata al controllo di duplicazione
	// configurazione globale attiva + passthrough a true -> verifichiamo che non ci sia chiamata al controllo di duplicazione
	// configurazione globale attiva + passthrough a false -> verifichiamo che ci sia chiamata al controllo di duplicazione e che venga ritornata eccezione

	//CG NA:
	@Test
	void ricezioneEsitiDuplicatesCheckNoActiveConfigOk()  {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiDuplicatesCheckNoActiveConfigOk() : START");
		ConsolidatoreIngressPaperProgressStatusEvent event = getProgressStatusEventWithoutAttachments();
		EventsDto con010 = new EventsDto().paperProgrStatus(new PaperProgressStatusDto().status(event.getStatusCode())
																						.statusDateTime(event.getStatusDateTime())
																						.statusDescription(event.getStatusDescription())
																						.iun(event.getIun())
																						.productType(event.getProductType())
																						.clientRequestTimeStamp(event.getClientRequestTimeStamp())
		);
		ReflectionTestUtils.setField(ricezioneEsitiCartaceoServiceImpl, "duplicatesCheck", new String[]{PRODUCT_TYPE_AR});
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(BOOKED_EVENT,SENT_EVENT, con010)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(event);

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isOk();
	}

	@Test
	void ricezioneEsitiDuplicatesCheckActiveConfigActivePassthroughOk()  {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiDuplicatesCheckNoActiveConfigOk() : START");
		ConsolidatoreIngressPaperProgressStatusEvent event = getProgressStatusEventWithoutAttachments();
		EventsDto con010 = new EventsDto().paperProgrStatus(new PaperProgressStatusDto().status(event.getStatusCode())
																						.statusDateTime(event.getStatusDateTime())
																						.statusDescription(event.getStatusDescription())
																						.iun(event.getIun())
																						.productType(event.getProductType())
																						.clientRequestTimeStamp(event.getClientRequestTimeStamp())
		);
		RequestDto requestDto = getRequestDto(BOOKED_EVENT, SENT_EVENT, con010);
		requestDto.getRequestMetadata().getPaperRequestMetadata().setDuplicateCheckPassthrough(true);
		ReflectionTestUtils.setField(ricezioneEsitiCartaceoServiceImpl, "duplicatesCheck", new String[]{PRODUCT_TYPE_AR});
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(requestDto));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(event);

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isOk();
	}

	@Test
	void ricezioneEsitiDuplicatesCheckActiveConfigNoActivePassthroughKo()  {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiDuplicatesCheckNoActiveConfigOk() : START");
		ConsolidatoreIngressPaperProgressStatusEvent event = getProgressStatusEventWithoutAttachments();
		EventsDto con010 = new EventsDto().paperProgrStatus(new PaperProgressStatusDto().status(event.getStatusCode())
																						.statusCode(event.getStatusCode())
																						.statusDateTime(event.getStatusDateTime())
																						.statusDescription(event.getStatusDescription())
																						.iun(event.getIun())
																						.productType(event.getProductType())
																						.clientRequestTimeStamp(event.getClientRequestTimeStamp())
																						.courier("recapitista1")
		);
		ReflectionTestUtils.setField(ricezioneEsitiCartaceoServiceImpl, "duplicatesCheck", new String[]{"ProductType",PRODUCT_TYPE_AR,"OtherProductType"});
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(BOOKED_EVENT, SENT_EVENT, con010)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(event);

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@ParameterizedTest(name = "duplicatesCheck active={0}, passthrough={1}, openRework={2}, expectedStatus={3}")
	@MethodSource("duplicatesCheckTestCases")
	void ricezioneEsitiDuplicatesCheckParametrized2(boolean configAttiva, boolean passthrough, boolean openRework, int expectedStatus) {
		log.info("Param test -> configAttiva={}, passthrough={}, openRework={}", configAttiva, passthrough, openRework);

		// event principale che inviamo
		ConsolidatoreIngressPaperProgressStatusEvent event = getProgressStatusEventWithoutAttachments();

		// evento già presente nella lista
		EventsDto con010 = new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
				.status(event.getStatusCode())
				.statusCode(event.getStatusCode())
				.statusDescription(event.getStatusDescription())
				.iun(event.getIun())
				.productType(event.getProductType())
				.statusDateTime(event.getStatusDateTime())
				.clientRequestTimeStamp(event.getClientRequestTimeStamp())
				.courier(event.getCourier())
		);

		RequestDto requestDto = getRequestDto(BOOKED_EVENT, SENT_EVENT, con010);
		requestDto.getRequestMetadata().getPaperRequestMetadata().setDuplicateCheckPassthrough(passthrough);
		requestDto.getRequestMetadata().getPaperRequestMetadata().setIsOpenReworkRequest(openRework);

		// config duplicatesCheck
		if (configAttiva) {
			ReflectionTestUtils.setField(ricezioneEsitiCartaceoServiceImpl, "duplicatesCheck", new String[]{"ProductType",PRODUCT_TYPE_AR,"OtherProductType"});
		} else {
			ReflectionTestUtils.setField(ricezioneEsitiCartaceoServiceImpl, "duplicatesCheck", new String[]{});
		}

		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID))
				.thenReturn(Mono.just(requestDto));
		when(statusPullService.paperPullService(anyString(), anyString()))
				.thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = List.of(event);
		var response = webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.bodyValue(events)
				.exchange();

		if (expectedStatus == 200) {
			response.expectStatus().isOk();
		} else {
			response.expectStatus().isBadRequest();
		}
	}


	@Test
	void ricezioneEsitiWithStatusDateTimeAndClientRequestTimeStampInFutureShouldThrowException(){
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiWithStatusDateTimeAndClientRequestTimeStampInFutureShouldThrowException() : START");
		OffsetDateTime now = OffsetDateTime.now();
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		EventsDto bookedEvent = new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
				.status(BOOKED.getStatusTransactionTableCompliant())
				.statusDateTime(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)));
		EventsDto retryEvent = new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
				.status(RETRY.getStatusTransactionTableCompliant())
				.statusDateTime(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)));
		EventsDto clientRequestTimeStamp = new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
				.status(BOOKED.getStatusTransactionTableCompliant())
				.clientRequestTimeStamp(OffsetDateTime.of(2024, 8, 1, 0, 0, 0, 0, ZoneOffset.UTC)));

		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(bookedEvent,retryEvent,clientRequestTimeStamp)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());
		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithoutAttachments().statusDateTime(now.plusDays(1)));
		events.add(getProgressStatusEventWithoutAttachments().clientRequestTimeStamp(now.plusDays(1)));

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@Test
	void ricezioneEsitiWithStatusDateTimeAndClientRequestTimeStampInFutureShouldReturnOk(){
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiWithStatusDateTimeAndClientRequestTimeStampInFutureShouldThrowException() : START");
		OffsetDateTime now = OffsetDateTime.now();
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		EventsDto bookedEvent = new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
				.status(BOOKED.getStatusTransactionTableCompliant())
				.statusDateTime(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)));
		EventsDto retryEvent = new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
				.status(RETRY.getStatusTransactionTableCompliant())
				.statusDateTime(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)));
		EventsDto clientRequestTimeStamp = new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
				.status(BOOKED.getStatusTransactionTableCompliant())
				.clientRequestTimeStamp(OffsetDateTime.of(2024, 8, 1, 0, 0, 0, 0, ZoneOffset.UTC)));

		ReflectionTestUtils.setField(ricezioneEsitiCartaceoServiceImpl, "offsetDuration", Duration.ofMinutes(-1));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(bookedEvent,retryEvent,clientRequestTimeStamp)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithoutAttachments().statusDateTime(now.plusDays(1)));
		events.add(getProgressStatusEventWithoutAttachments().clientRequestTimeStamp(now.plusDays(1)));

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isOk();
	}
	@Test
	void ricezioneEsitiDiscardedEventShouldBeAddedOnGeneric400Error() {
		log.info("RicezioneEsitiConsolidatoreControllerTest.ricezioneEsitiOk() : START");
		when(authService.clientAuth(anyString())).thenReturn(Mono.just(clientConfigurationInternalDto));
		when(gestoreRepositoryCall.getRichiesta(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, REQUEST_ID)).thenReturn(Mono.just(getRequestDto(SENT_EVENT)));
		when(statusPullService.paperPullService(anyString(), anyString())).thenReturn(Mono.just(new PaperProgressStatusEvent().productType(PRODUCT_TYPE_AR).iun(IUN)));
		when(gestoreRepositoryCall.insertDiscardedEvents(any())).thenReturn(Flux.empty());

		when(fileCall.getFile(DOCUMENT_KEY, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE, true))
				.thenReturn(Mono.error(new Generic400ErrorException("Chiamata a safestorage non valida", "Resource is no longer available. It may have been removed or deleted.")));

		List<ConsolidatoreIngressPaperProgressStatusEvent> events = new ArrayList<>();
		events.add(getProgressStatusEventWithAttachments());

		webClient.put()
				.uri(RICEZIONE_ESITI_ENDPOINT)
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.header(X_PAGOPA_EXTCH_SERVICE_ID_HEADER_NAME, X_PAGOPA_EXTCH_SERVICE_ID_HEADER_VALUE)
				.header(X_API_KEY_HEADER_NAME, X_API_KEY_HEADER_VALUE)
				.body(BodyInserters.fromValue(events))
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	private static Stream<Arguments> duplicatesCheckTestCases() {
		return Stream.of(
				// config attiva (shouldCheck = true)
				Arguments.of(true, true, true, 200),
				Arguments.of(true, false, true, 400),    // uno dei flag è false -> controllo duplicati -> error
				Arguments.of(true, false, false, 400),   // passthrough=false e isOpenRework=false -> controllo duplicati -> error
				Arguments.of(true, true, false, 400),     // passthrough=true ma isOpenRework=false -> controllo duplicati -> error

				// config non attiva (shouldCheck = false)
				Arguments.of(false, false, false, 200),
				Arguments.of(false, true, false, 200),
				Arguments.of(false, false, true, 200),
				Arguments.of(false, true, true, 200)
		);
	}


}
