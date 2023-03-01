//package it.pagopa.pn.ec.spedizionedocumenticartacei.controller;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.server.ServerWebExchange;
//
//import it.pagopa.pn.ec.commons.service.StatusPullService;
//import it.pagopa.pn.ec.rest.v1.api.PaperMessagesApi;
//import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusEvent;
//import reactor.core.publisher.Mono;
//
//@RestController
//public class PaperMessagesApiController implements PaperMessagesApi {
//
//	@Autowired
//	private StatusPullService paperService;
//
//	@Override
//	public Mono<ResponseEntity<PaperProgressStatusEvent>> getPaperEngageProgresses(String requestIdx,
//			String xPagopaExtchCxId, ServerWebExchange exchange) {
//		return paperService.paperPullService(requestIdx, xPagopaExtchCxId).map(ResponseEntity::ok);
//	}
//
//}
