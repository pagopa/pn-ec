package it.pagopa.pn.ec.consolidatore.controller;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.ec.consolidatore.service.PushAttachmentPreloadService;
import it.pagopa.pn.ec.consolidatore.service.RicezioneEsitiCartaceoService;
import it.pagopa.pn.ec.rest.v1.api.ConsolidatoreApi;
import it.pagopa.pn.ec.rest.v1.dto.ConsolidatoreIngressPaperProgressStatusEvent;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.ec.rest.v1.dto.PreLoadRequestData;
import it.pagopa.pn.ec.rest.v1.dto.PreLoadResponseData;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class ConsolidatoreApiController implements ConsolidatoreApi {

    private final PushAttachmentPreloadService pushAttachmentPreloadService;
    
    private final RicezioneEsitiCartaceoService ricezioneEsitiCartaceoService;
    
	public ConsolidatoreApiController(PushAttachmentPreloadService pushAttachmentPreloadService,
			RicezioneEsitiCartaceoService ricezioneEsitiCartaceoService) 
	{
		this.pushAttachmentPreloadService = pushAttachmentPreloadService;
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
    public Mono<ResponseEntity<PreLoadResponseData>> presignedUploadRequest(Mono<PreLoadRequestData> preLoadRequestData, ServerWebExchange exchange) {
        return pushAttachmentPreloadService.presignedUploadRequest(preLoadRequestData).map(ResponseEntity::ok);
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
