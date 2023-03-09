package it.pagopa.pn.ec.cartaceo.service;

import static it.pagopa.pn.ec.cartaceo.utils.PaperResult.COMPLETED_MESSAGE;
import static it.pagopa.pn.ec.cartaceo.utils.PaperResult.COMPLETED_OK_CODE;
import static it.pagopa.pn.ec.cartaceo.utils.PaperResult.GENERIC_ERROR_CODE;
import static it.pagopa.pn.ec.cartaceo.utils.PaperResult.SEMANTIC_ERROR;
import static it.pagopa.pn.ec.cartaceo.utils.PaperResult.SEMANTIC_ERROR_CODE;
import static it.pagopa.pn.ec.cartaceo.utils.PaperResult.SYNTAX_ERROR;
import static it.pagopa.pn.ec.cartaceo.utils.PaperResult.SYNTAX_ERROR_CODE;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import it.pagopa.pn.ec.cartaceo.exception.RicezioneEsitiCartaceoException;
import it.pagopa.pn.ec.cartaceo.utils.PaperElem;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.rest.v1.dto.GeneratedMessageDto;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusEvent;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;;

@Service
@Slf4j
public class RicezioneEsitiCartaceoServiceImpl implements RicezioneEsitiCartaceoService {
	
	private final GestoreRepositoryCall gestoreRepositoryCall;
	private final NotificationTrackerSqsName notificationTrackerSqsName;
	private final SqsService sqsService;
	private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;
	
	public RicezioneEsitiCartaceoServiceImpl(GestoreRepositoryCall gestoreRepositoryCall, 
			NotificationTrackerSqsName notificationTrackerSqsName,
			SqsService sqsService, TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
		super();
		this.gestoreRepositoryCall = gestoreRepositoryCall;
		this.notificationTrackerSqsName = notificationTrackerSqsName;
		this.sqsService = sqsService;
		this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
	}
	
	// errore sintattico -> resultDescription: 'Syntax Error'
	private Mono<List<String>> verificaErroriSintattici(PaperProgressStatusEvent paperProgressStatusEvent) {
		log.info("RicezioneEsitiCartaceoServiceImpl.verificaErroriSintattici() : START for requestId \"{}\"",
				 paperProgressStatusEvent.getRequestId() != null ? paperProgressStatusEvent.getRequestId() : "assente");
		String errore = "field %s is required";
		List<String> errori = new ArrayList<>();
		if (paperProgressStatusEvent.getRequestId() == null || paperProgressStatusEvent.getRequestId().isBlank()) {
			errori.add(String.format(errore, "requestId"));
		}
		if (paperProgressStatusEvent.getStatusCode() == null || paperProgressStatusEvent.getStatusCode().isBlank()) {
			errori.add(String.format(errore, "statusCode"));
		}
		if (paperProgressStatusEvent.getStatusDescription() == null || paperProgressStatusEvent.getStatusDescription().isBlank()) {
			errori.add(String.format(errore, "statusDescription"));
		}
		if (paperProgressStatusEvent.getStatusDateTime() == null) {
			errori.add(String.format(errore, "statusDateTime"));
		}
		if (paperProgressStatusEvent.getProductType() == null || paperProgressStatusEvent.getProductType().isBlank()) {
			errori.add(String.format(errore, "statusDateTime"));
		}
		if (paperProgressStatusEvent.getClientRequestTimeStamp() == null) {
			errori.add(String.format(errore, "clientRequestTimeStamp"));
		}
		if (paperProgressStatusEvent.getAttachments() != null && !paperProgressStatusEvent.getAttachments().isEmpty()) {
			paperProgressStatusEvent.getAttachments().forEach(attachment -> {
				if (attachment.getId() == null || attachment.getId().isBlank()) {
					errori.add(String.format(errore, "attachment.id"));
				}
				if (attachment.getDocumentType() == null || attachment.getDocumentType().isBlank()) {
					errori.add(String.format(errore, "attachment.documentType"));
				}
				if (attachment.getUrl() == null || attachment.getUrl().isBlank()) {
					errori.add(String.format(errore, "attachment.url"));
				}
				if (attachment.getDate() == null) {
					errori.add(String.format(errore, "attachment.date"));
				}
			});
		}
		return Mono.just(errori);
	}
	
	// errore semantico -> resultDescription: 'Semantic Error'
	private Mono<List<String>> verificaErroriSemantici(PaperProgressStatusEvent paperProgressStatusEvent) {
		log.info("RicezioneEsitiCartaceoServiceImpl.verificaCorrettezzaStatusCode() : START for requestId \"{}\"",
				paperProgressStatusEvent.getRequestId() != null ? paperProgressStatusEvent.getRequestId() : "assente");

		List<String> errori = new ArrayList<>();
		String erroreUnrecognized = "%s unrecognized";
		if (paperProgressStatusEvent != null) {
			if (!PaperElem.statusCodeDescriptionMap().containsKey(paperProgressStatusEvent.getStatusCode())) {
				errori.add(String.format(erroreUnrecognized, "statusCode"));
			}
			if (!PaperElem.productTypeMap().containsKey(paperProgressStatusEvent.getProductType())) {
				errori.add(String.format(erroreUnrecognized, "productType"));
			}
			if (paperProgressStatusEvent.getDeliveryFailureCause() != null 
					&& !paperProgressStatusEvent.getDeliveryFailureCause().isBlank()
					&& !PaperElem.deliveryFailureCausemap().containsKey(paperProgressStatusEvent.getDeliveryFailureCause())) {
				errori.add(String.format(erroreUnrecognized, "deliveryFailureCause"));
			}
		}
		return Mono.just(errori);
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
	public Mono<OperationResultCodeResponse> ricezioneEsitiDaConsolidatore(
			String xPagopaExtchServiceId,
			PaperProgressStatusEvent paperProgressStatusEvent) 
	{
		 return  Mono.just(paperProgressStatusEvent)
				     .doOnNext(event -> log.info("RicezioneEsitiCartaceoServiceImpl.ricezioneEsitiDaConsolidatore() : "
				     							 + "START for requestId {}",
				     							 paperProgressStatusEvent.getRequestId()))
				     .flatMap(event -> {
				    	 // verificare presenza attributi obbligatori
				    	 return verificaErroriSintattici(paperProgressStatusEvent)
				    	 		.flatMap(errori -> {
				    	 			if (errori != null && !errori.isEmpty()) {
				    	 				return Mono.error(new RicezioneEsitiCartaceoException(
				    	 						"Verifica Errori Sintattici", 
				    	 						SYNTAX_ERROR_CODE, 
				    	 						SYNTAX_ERROR, 
				    	 						errori));
				    	 			}
				    	 			// verificare correttezza dati
				    	 			return verificaErroriSemantici(paperProgressStatusEvent);
				    	 		})
				    	 		.flatMap(errori -> {
				    	 			if (errori != null && !errori.isEmpty()) {
				    	 				return Mono.error(new RicezioneEsitiCartaceoException(
				    	 						"Verifica Errori Semantici", 
				    	 						SEMANTIC_ERROR_CODE, 
				    	 						SEMANTIC_ERROR, 
				    	 						errori));
				    	 			}
				    	 			// verificare esistenza requestId
				    	 			return gestoreRepositoryCall.getRichiesta(paperProgressStatusEvent.getRequestId());
				    	 		})
				    	 		.flatMap(requestDto -> {
				    	 			// pubblicazione sulla coda del notification tracker
				    	 			GeneratedMessageDto gmDTO = new GeneratedMessageDto();
				    	 			gmDTO.setId(""); // altrimenti "N/A"
				    	 			return sqsService.send(notificationTrackerSqsName.statoCartaceoName(), 
						    	 							new NotificationTrackerQueueDto(
								    	 							paperProgressStatusEvent.getRequestId(),        
								    	 							xPagopaExtchServiceId,        
								    	 							OffsetDateTime.now(),        
									    	 						transactionProcessConfigurationProperties.paper(),        
									    	 						transactionProcessConfigurationProperties.paperStarterStatus(),        
									    	 						"inprogress",        
									    	 						gmDTO));
				    	 		})
				    	 		.flatMap(sendMessageResponse -> {
				    	 			return getOperationResultCodeResponse(COMPLETED_OK_CODE, COMPLETED_MESSAGE, null);
				    	 		});
				     })
				     // *** errore
				     .onErrorResume(RicezioneEsitiCartaceoException.class, throwable -> {
				    	 if (throwable.getErrorList() != null && !throwable.getErrorList().isEmpty()) {
				    		 log.error("RicezioneEsitiCartaceoServiceImpl.ricezioneEsitiDaConsolidatore() : "
				    				   + "errore verifica sintattica/semantica : "
				    				   + "resultCode = {} : resultDescription {} : errorList {}",
				    				   throwable.getResultCode(), throwable.getResultDescription(), throwable.getErrorList(),
				    				   throwable);
				    		 return getOperationResultCodeResponse(throwable.getResultCode(), throwable.getResultDescription(), throwable.getErrorList());
				    	 }
			    		 log.error("RicezioneEsitiCartaceoServiceImpl.ricezioneEsitiDaConsolidatore() : errore = {} ",
			    				   throwable.getMessage(),
			    				   throwable);
				    	 return getOperationResultCodeResponse(GENERIC_ERROR_CODE, throwable.getMessage(), null);
				     })
				     // *** errore
				     .onErrorResume(RestCallException.ResourceNotFoundException.class, throwable -> {
			    		 log.error("RicezioneEsitiCartaceoServiceImpl.ricezioneEsitiDaConsolidatore() : errore request = {} ",
			    				   throwable.getMessage(),
			    				   throwable);
				    	 return getOperationResultCodeResponse(SEMANTIC_ERROR_CODE, SEMANTIC_ERROR, List.of("requestId unrecognized"));
				     })
				     // *** errore
				     .onErrorResume(RuntimeException.class, throwable -> {
				    	 log.error("RicezioneEsitiCartaceoServiceImpl.ricezioneEsitiDaConsolidatore() : errore generico = {}",
				    			   throwable.getMessage(),
				    			   throwable);
				    	 return getOperationResultCodeResponse(GENERIC_ERROR_CODE, throwable.getMessage(), null);
				     });
	}

}
