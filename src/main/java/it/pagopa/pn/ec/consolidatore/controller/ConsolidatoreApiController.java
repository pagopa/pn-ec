package it.pagopa.pn.ec.consolidatore.controller;

import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.COMPLETED_MESSAGE;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.COMPLETED_OK_CODE;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.INTERNAL_SERVER_ERROR_CODE;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.errorCodeDescriptionMap;

import java.util.ArrayList;
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
	
	private static final Integer NRO_MAX_ERRORS = 50;
	private static final String LOG_LABEL = "ConsolidatoreApiController.sendPaperProgressStatusRequest() : ";

    private final PushAttachmentPreloadService pushAttachmentPreloadService;
    private final RicezioneEsitiCartaceoService ricezioneEsitiCartaceoService;
    
	public ConsolidatoreApiController(PushAttachmentPreloadService pushAttachmentPreloadService,
			RicezioneEsitiCartaceoService ricezioneEsitiCartaceoService) 
	{
		this.pushAttachmentPreloadService = pushAttachmentPreloadService;
		this.ricezioneEsitiCartaceoService = ricezioneEsitiCartaceoService;
	}
	
	private List<String> getAllErrors(List<OperationResultCodeResponse> responses) {
		var errors = new ArrayList<String>();
		if (responses == null) {
			return errors;
		}
		responses.forEach(response -> {
			if (!response.getResultCode().equals(COMPLETED_OK_CODE)) {
				errors.addAll(response.getErrorList());
			}
		});
		return errors;
	}
	
	private OperationResultCodeResponse getOperationResultCodeResponse(
			String resultCode, String resultDescription, List<String> errors) {
		
		if (errors != null && errors.size() > NRO_MAX_ERRORS) {
			errors = errors.subList(0, NRO_MAX_ERRORS-1);
		}
		
		OperationResultCodeResponse response = new OperationResultCodeResponse();
		response.setResultCode(resultCode);
		response.setResultDescription(resultDescription);
		response.setErrorList(errors);
		return response;
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
		return consolidatoreIngressPaperProgressStatusEvent
				.flatMap(statusEvent -> {
					log.info(LOG_LABEL + "START for requestId {}", statusEvent.getRequestId());
					return ricezioneEsitiCartaceoService.verificaEsitoDaConsolidatore(xPagopaExtchServiceId, statusEvent);
				})
				.collectList()
				.flatMap(listRicezioneEsitiDto -> {
					// ricerco errori
					var listErrorResponse = listRicezioneEsitiDto.stream()
																.filter(ricezioneEsito -> ricezioneEsito.getOperationResultCodeResponse() != null &&
																						  ricezioneEsito.getOperationResultCodeResponse().getResultCode() != null && 
																						  !ricezioneEsito.getOperationResultCodeResponse().getResultCode().equals(COMPLETED_OK_CODE))
																.toList();
					
					if (listErrorResponse.isEmpty()) 
					{
						log.info(LOG_LABEL + "Non ci sono errori sintattici/semantici");
						
						// eventi
						var listEvents = new ArrayList<ConsolidatoreIngressPaperProgressStatusEvent>();
						listRicezioneEsitiDto.forEach(dto -> {
							if (dto.getPaperProgressStatusEvent() != null) {
								listEvents.add(dto.getPaperProgressStatusEvent());
							}
						});
						
						return Flux.fromIterable(listEvents)
							// pubblicazione sulla coda
							.flatMap(statusEvent -> ricezioneEsitiCartaceoService.pubblicaEsitoCodaNotificationTracker(xPagopaExtchServiceId, statusEvent))
							.collectList()
							// gestione errori oppure response ok
							.flatMap(listSendResponse -> {
								var listSendErrorResponse = listSendResponse.stream().filter(response -> response.getResultCode() != null && !response.getResultCode().equals(COMPLETED_OK_CODE)).toList();
								if (listSendErrorResponse.isEmpty()) {
									log.info(LOG_LABEL + "OK END");
							    	return Mono.just(ResponseEntity.ok()
		    								   .body(getOperationResultCodeResponse(COMPLETED_OK_CODE, 
		    										   								COMPLETED_MESSAGE, 
		    										                                null)));
								}
								else {
									var sendErrors = getAllErrors(listSendErrorResponse);
									log.error(LOG_LABEL + "pubblicazione coda : errori individuati = {}", sendErrors);
							    	return Mono.just(ResponseEntity.internalServerError()
		    								   .body(getOperationResultCodeResponse(INTERNAL_SERVER_ERROR_CODE, 
		    										   								errorCodeDescriptionMap().get(INTERNAL_SERVER_ERROR_CODE), 
		    										   								sendErrors)));
								}
							})
							.onErrorResume(RuntimeException.class, throwable -> {
								log.error(LOG_LABEL	+ "errore generico interno = {}", throwable.getMessage(), throwable);
								return Mono.error(throwable);
							});
					}
					else 
					{
						log.info(LOG_LABEL + "errori sintattici/semantici : Sono stati individuati {} macro errori", listErrorResponse.size());
						
						// errori 
						var listErrors = new ArrayList<OperationResultCodeResponse>();
						listErrorResponse.forEach(dto -> {
							if (dto.getOperationResultCodeResponse() != null) {
								listErrors.add(dto.getOperationResultCodeResponse());
							}
						});
						
						var errors = getAllErrors(listErrors);
						log.error(LOG_LABEL + "errori sintattici/semantici : "
								+ "result code = \"{}\" : "
								+ "result description = \"{}\" : "
								+ "specifici errori individuati = {}", 
								listErrors.get(0).getResultCode(),
								listErrors.get(0).getResultDescription(),
								errors);
				    	return Mono.just(ResponseEntity
				    						.badRequest()
				    						.body(getOperationResultCodeResponse(listErrors.get(0).getResultCode(), 
				    															 listErrors.get(0).getResultDescription(), 
												   								 errors)));
					}
				})
			    .onErrorResume(RuntimeException.class, throwable -> {
					log.error(LOG_LABEL	+ "errore generico = {}", throwable.getMessage(), throwable);
			    	return Mono.just(ResponseEntity.internalServerError()
			    								   .body(getOperationResultCodeResponse(INTERNAL_SERVER_ERROR_CODE, 
			    										   								errorCodeDescriptionMap().get(INTERNAL_SERVER_ERROR_CODE), 
			    										                                List.of(throwable.getMessage()))));
			    });
	}

}
