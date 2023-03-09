package it.pagopa.pn.ec.cartaceo.service;

import static it.pagopa.pn.ec.cartaceo.utils.PaperElem.attachmentDocumentTypeMap;
import static it.pagopa.pn.ec.cartaceo.utils.PaperElem.deliveryFailureCausemap;
import static it.pagopa.pn.ec.cartaceo.utils.PaperElem.productTypeMap;
import static it.pagopa.pn.ec.cartaceo.utils.PaperElem.statusCodeDescriptionMap;
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
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.rest.v1.dto.ConsolidatoreIngressPaperProgressStatusEvent;
import it.pagopa.pn.ec.rest.v1.dto.ConsolidatoreIngressPaperProgressStatusEventDiscoveredAddress;
import it.pagopa.pn.ec.rest.v1.dto.GeneratedMessageDto;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

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
	private Mono<List<String>> verificaErroriSintattici(ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent) {
		log.info("RicezioneEsitiCartaceoServiceImpl.verificaErroriSintattici() : START for requestId \"{}\"",
				 progressStatusEvent.getRequestId() != null ? progressStatusEvent.getRequestId() : "assente");
		String errore = "field %s is required";
		List<String> errori = new ArrayList<>();
		if (progressStatusEvent.getRequestId() == null || progressStatusEvent.getRequestId().isBlank()) {
			errori.add(String.format(errore, "requestId"));
		}
		if (progressStatusEvent.getStatusCode() == null || progressStatusEvent.getStatusCode().isBlank()) {
			errori.add(String.format(errore, "statusCode"));
		}
		if (progressStatusEvent.getStatusDescription() == null || progressStatusEvent.getStatusDescription().isBlank()) {
			errori.add(String.format(errore, "statusDescription"));
		}
		if (progressStatusEvent.getStatusDateTime() == null) {
			errori.add(String.format(errore, "statusDateTime"));
		}
		if (progressStatusEvent.getProductType() == null || progressStatusEvent.getProductType().isBlank()) {
			errori.add(String.format(errore, "statusDateTime"));
		}
		if (progressStatusEvent.getClientRequestTimeStamp() == null) {
			errori.add(String.format(errore, "clientRequestTimeStamp"));
		}
		if (progressStatusEvent.getAttachments() != null && !progressStatusEvent.getAttachments().isEmpty()) {
			progressStatusEvent.getAttachments().forEach(attachment -> {
				if (attachment.getId() == null || attachment.getId().isBlank()) {
					errori.add(String.format(errore, "attachment.id"));
				}
				if (attachment.getDocumentType() == null || attachment.getDocumentType().isBlank()) {
					errori.add(String.format(errore, "attachment.documentType"));
				}
				if (attachment.getUri() == null || attachment.getUri().isBlank()) {
					errori.add(String.format(errore, "attachment.uri"));
				}
				if (attachment.getSha256() == null || attachment.getSha256().isBlank()) {
					errori.add(String.format(errore, "attachment.sha256"));
				}
				if (attachment.getDate() == null) {
					errori.add(String.format(errore, "attachment.date"));
				}
			});
		}
		ConsolidatoreIngressPaperProgressStatusEventDiscoveredAddress discoveredAddress = progressStatusEvent.getDiscoveredAddress();
		if (discoveredAddress != null) {
			if (discoveredAddress.getName() == null || discoveredAddress.getName().isBlank()) {
				errori.add(String.format(errore, "discoveredAddress.name"));
			}
			if (discoveredAddress.getAddress() == null || discoveredAddress.getAddress().isBlank()) {
				errori.add(String.format(errore, "discoveredAddress.address"));
			}
			if (discoveredAddress.getCity() == null || discoveredAddress.getCity().isBlank()) {
				errori.add(String.format(errore, "discoveredAddress.city"));
			}
		}
		return Mono.just(errori);
	}
	
	// errore semantico -> resultDescription: 'Semantic Error'
	private Mono<List<String>> verificaErroriSemantici(ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent) {
		log.info("RicezioneEsitiCartaceoServiceImpl.verificaCorrettezzaStatusCode() : START for requestId \"{}\"",
				progressStatusEvent.getRequestId() != null ? progressStatusEvent.getRequestId() : "assente");

		List<String> errori = new ArrayList<>();
		String erroreUnrecognized = "%s unrecognized : wrong value = %s";
		
		if (!statusCodeDescriptionMap().containsKey(progressStatusEvent.getStatusCode())) {
			errori.add(String.format(erroreUnrecognized, "statusCode"));
		}
		if (!productTypeMap().containsKey(progressStatusEvent.getProductType())) {
			errori.add(String.format(erroreUnrecognized, "productType"));
		}
		// DeliveryFailureCaus non è un campo obbligatorio
		if (progressStatusEvent.getDeliveryFailureCause() != null 
				&& !progressStatusEvent.getDeliveryFailureCause().isBlank()
				&& !deliveryFailureCausemap().containsKey(progressStatusEvent.getDeliveryFailureCause())) {
			errori.add(String.format(erroreUnrecognized, "deliveryFailureCause", progressStatusEvent.getDeliveryFailureCause()));
		}
		// Attachments non e' una lista obbligatoria
		if (progressStatusEvent.getAttachments() != null && !progressStatusEvent.getAttachments().isEmpty()) {
			progressStatusEvent.getAttachments().forEach(attachment -> {
				if (attachment.getDocumentType() != null 
						&& !attachmentDocumentTypeMap().contains(attachment.getDocumentType())) {
					errori.add(String.format(erroreUnrecognized, "attachment.documentType", attachment.getDocumentType()));
				}
			});
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
			ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent) 
	{
		 return  Mono.just(progressStatusEvent)
				     .doOnNext(event -> log.info("RicezioneEsitiCartaceoServiceImpl.ricezioneEsitiDaConsolidatore() : "
				     							 + "START for requestId {}",
				     							progressStatusEvent.getRequestId()))
				     .flatMap(event -> 
				    	 // verificare presenza attributi obbligatori
				    	 verificaErroriSintattici(progressStatusEvent)
				    	 		.flatMap(errori -> {
				    	 			if (errori != null && !errori.isEmpty()) {
				    	 				return Mono.error(new RicezioneEsitiCartaceoException(
				    	 						"Verifica Errori Sintattici", 
				    	 						SYNTAX_ERROR_CODE, 
				    	 						SYNTAX_ERROR, 
				    	 						errori));
				    	 			}
				    	 			// verificare correttezza dati
				    	 			return verificaErroriSemantici(progressStatusEvent);
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
				    	 			return gestoreRepositoryCall.getRichiesta(progressStatusEvent.getRequestId());
				    	 		})
				    	 		.flatMap(requestDto -> {
				    	 			// pubblicazione sulla coda del notification tracker
				    	 			GeneratedMessageDto gmDTO = new GeneratedMessageDto();
				    	 			gmDTO.setId(""); // altrimenti "N/A"
				    	 			return sqsService.send(notificationTrackerSqsName.statoCartaceoName(), 
						    	 							new NotificationTrackerQueueDto(
						    	 									progressStatusEvent.getRequestId(),        
								    	 							xPagopaExtchServiceId,        
								    	 							OffsetDateTime.now(),        
									    	 						transactionProcessConfigurationProperties.paper(),        
									    	 						transactionProcessConfigurationProperties.paperStarterStatus(),        
									    	 						"inprogress",        
									    	 						gmDTO));
				    	 		})
				    	 		.flatMap(sendMessageResponse -> getOperationResultCodeResponse(COMPLETED_OK_CODE, COMPLETED_MESSAGE, null))
				     )
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
