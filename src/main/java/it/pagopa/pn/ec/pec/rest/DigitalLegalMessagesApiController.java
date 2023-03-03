package it.pagopa.pn.ec.pec.rest;

import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.pec.service.impl.PecService;
import it.pagopa.pn.ec.rest.v1.api.DigitalLegalMessagesApi;
import it.pagopa.pn.ec.rest.v1.dto.CourtesyMessageProgressEvent;
import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;
import it.pagopa.pn.ec.rest.v1.dto.LegalMessageSentDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.OK;

import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@RestController
public class DigitalLegalMessagesApiController implements DigitalLegalMessagesApi {

	private final PecService pecService;

	private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;

	private final StatusPullService statusPullService;

	public DigitalLegalMessagesApiController(PecService pecService, StatusPullService statusPullService,
			TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
		this.pecService = pecService;
		this.statusPullService=statusPullService;
		this.transactionProcessConfigurationProperties=transactionProcessConfigurationProperties;
	}

	@Override
	public Mono<ResponseEntity<Void>> sendDigitalLegalMessage(String requestIdx, String xPagopaExtchCxId,
			Mono<DigitalNotificationRequest> digitalNotificationRequest, final ServerWebExchange exchange) {
		return digitalNotificationRequest.doOnNext(request -> log.info("<-- Start presa in carico -->")).flatMap(
				request -> pecService.presaInCarico(new PecPresaInCaricoInfo(requestIdx, xPagopaExtchCxId, request)))
				.thenReturn(new ResponseEntity<>(OK));
	}

	@Override
	public Mono<ResponseEntity<LegalMessageSentDetails>> getDigitalLegalMessageStatus(String requestIdx,
			String xPagopaExtchCxId, ServerWebExchange exchange) {

		return statusPullService
				.pecPullService(requestIdx, xPagopaExtchCxId, transactionProcessConfigurationProperties.pec())
				.map(ResponseEntity::ok);
	}

}
