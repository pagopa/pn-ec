package it.pagopa.pn.ec.cartaceo.rest;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.ec.cartaceo.exception.RicezioneEsitiCartaceoException;
import it.pagopa.pn.ec.cartaceo.service.RicezioneEsitiCartaceoService;
import it.pagopa.pn.ec.rest.v1.api.PushProgressEventsToNotificationPlatformApi;
import it.pagopa.pn.ec.rest.v1.dto.ConsolidatoreIngressPaperProgressStatusEvent;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusEvent;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class PushProgressEventsToNotificationPlatformApiController implements PushProgressEventsToNotificationPlatformApi{
	
	private final RicezioneEsitiCartaceoService ricezioneEsitiCartaceoService;

	public PushProgressEventsToNotificationPlatformApiController(
			RicezioneEsitiCartaceoService ricezioneEsitiCartaceoService) {
		this.ricezioneEsitiCartaceoService = ricezioneEsitiCartaceoService;
	}
	
	private Mono<OperationResultCodeResponse> getOperationResultCodeResponse(
			String resultCode, String resultDescription, List<String> errorList) {
		OperationResultCodeResponse response = new OperationResultCodeResponse();
		response.setResultCode(resultCode);
		response.setResultDescription(resultDescription);
		response.setErrorList(errorList);
		response.setClientResponseTimeStamp(OffsetDateTime.now());
		return Mono.just(response);
	}
	
	@Override
	public Mono<ResponseEntity<OperationResultCodeResponse>> sendPaperProgressStatusRequest(
			String xPagopaExtchServiceId, String xApiKey, 
			Flux<ConsolidatoreIngressPaperProgressStatusEvent> consolidatoreIngressPaperProgressStatusEvent,  
			final ServerWebExchange exchange)
	{ 
		consolidatoreIngressPaperProgressStatusEvent
				.doOnNext(event -> log.info("PushProgressEventsToNotificationPlatformApiController.sendPaperProgressStatusRequest() : "
											+ "START for requestId {}",
											event.getRequestId()))
				.flatMap(statusEvent -> ricezioneEsitiCartaceoService.ricezioneEsitiDaConsolidatore(xPagopaExtchServiceId, statusEvent))
			    .onErrorResume(RuntimeException.class, throwable -> {
			    	 
			    	 return null;
			     });
		 
		 return null;
	}

}
