package it.pagopa.pn.ec.cartaceo.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.ec.cartaceo.exception.RicezioneEsitiCartaceoException;
import it.pagopa.pn.ec.cartaceo.service.RicezioneEsitiCartaceoService;
import it.pagopa.pn.ec.rest.v1.api.PushProgressEventsToNotificationPlatformApi;
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
	
	@Override
	public Mono<ResponseEntity<OperationResultCodeResponse>> sendPaperProgressStatusRequest(
			String xPagopaExtchServiceId, String xApiKey, 
			Flux<PaperProgressStatusEvent> paperProgressStatusEvent, 
			final ServerWebExchange exchange)
	{ 
		paperProgressStatusEvent
				.doOnNext(event -> log.info("PushProgressEventsToNotificationPlatformApiController.sendPaperProgressStatusRequest() : "
											+ "START for requestId {}",
											event.getRequestId()))
				.flatMap(ricezioneEsitiCartaceoService::ricezioneEsitiDaConsolidatore)
			    .onErrorResume(RicezioneEsitiCartaceoException.class, throwable -> {
			    	 
			    	 return null;
			     });
//				.flatMap(response -> ResponseEntity.ok(response));
		 
		 return null;
	}

}
