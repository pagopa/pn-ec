package it.pagopa.pn.ec.consolidatore.service;

import static it.pagopa.pn.ec.consolidatore.utils.PaperElem.attachmentDocumentTypeMap;
import static it.pagopa.pn.ec.consolidatore.utils.PaperElem.deliveryFailureCausemap;
import static it.pagopa.pn.ec.consolidatore.utils.PaperElem.productTypeMap;
import static it.pagopa.pn.ec.consolidatore.utils.PaperElem.statusCodeDescriptionMap;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.INTERNAL_SERVER_ERROR_CODE;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.REQUEST_ID_ERROR_CODE;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.SEMANTIC_ERROR_CODE;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.COMPLETED_OK_CODE;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.COMPLETED_MESSAGE;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.errorCodeDescriptionMap;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.consolidatore.exception.AttachmentsEmptyRicezioneEsitiCartaceoException;
import it.pagopa.pn.ec.consolidatore.exception.RicezioneEsitiCartaceoException;
import it.pagopa.pn.ec.rest.v1.dto.ConsolidatoreIngressPaperProgressStatusEvent;
import it.pagopa.pn.ec.rest.v1.dto.ConsolidatoreIngressPaperProgressStatusEventAttachments;
import it.pagopa.pn.ec.rest.v1.dto.GeneratedMessageDto;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class RicezioneEsitiCartaceoServiceImpl implements RicezioneEsitiCartaceoService {
	
	private static final String SS_IN_URI = "safestorage://";
	private static final String UNRECOGNIZED_ERROR = "%s unrecognized = %s";
	
	private final GestoreRepositoryCall gestoreRepositoryCall;
	private final FileCall fileCall;
	private final NotificationTrackerSqsName notificationTrackerSqsName;
	private final SqsService sqsService;
	private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;
	
	public RicezioneEsitiCartaceoServiceImpl(GestoreRepositoryCall gestoreRepositoryCall, 
			FileCall fileCall, NotificationTrackerSqsName notificationTrackerSqsName,
			SqsService sqsService, TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
		super();
		this.gestoreRepositoryCall = gestoreRepositoryCall;
		this.fileCall = fileCall;
		this.notificationTrackerSqsName = notificationTrackerSqsName;
		this.sqsService = sqsService;
		this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
	}
	
	private OperationResultCodeResponse getOperationResultCodeResponse(
			String resultCode, String resultDescription, List<String> errorList) {
		OperationResultCodeResponse response = new OperationResultCodeResponse();
		response.setResultCode(resultCode);
		response.setResultDescription(resultDescription);
		response.setErrorList(errorList);
		response.setClientResponseTimeStamp(OffsetDateTime.now());
		return response;
	}
	
	// errore semantico -> resultDescription: 'Semantic Error'
	private Mono<OperationResultCodeResponse> verificaErroriSemantici(ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent) 
			throws RicezioneEsitiCartaceoException 
	{
		log.info("RicezioneEsitiCartaceoServiceImpl.verificaCorrettezzaStatusCode() : START for requestId \"{}\"",
				progressStatusEvent.getRequestId());

		// StatusCode
		if (!statusCodeDescriptionMap().containsKey(progressStatusEvent.getStatusCode())) {
				return Mono.error(new RicezioneEsitiCartaceoException(
		    	 						SEMANTIC_ERROR_CODE, 
		    	 						errorCodeDescriptionMap().get(SEMANTIC_ERROR_CODE),
		    	 						List.of(String.format(UNRECOGNIZED_ERROR, "statusCode", progressStatusEvent.getStatusCode()))));
		}
		// ProductTypeMap
		if (!productTypeMap().containsKey(progressStatusEvent.getProductType())) {
				return Mono.error(new RicezioneEsitiCartaceoException(
		    	 						SEMANTIC_ERROR_CODE, 
		    	 						errorCodeDescriptionMap().get(SEMANTIC_ERROR_CODE),
		    	 						List.of(String.format(UNRECOGNIZED_ERROR, "productType", progressStatusEvent.getProductType()))));										
		}
		// DeliveryFailureCaus non è un campo obbligatorio
		if (progressStatusEvent.getDeliveryFailureCause() != null 
				&& !progressStatusEvent.getDeliveryFailureCause().isBlank()
				&& !deliveryFailureCausemap().containsKey(progressStatusEvent.getDeliveryFailureCause())) {
				return Mono.error(new RicezioneEsitiCartaceoException(
		    	 						SEMANTIC_ERROR_CODE, 
		    	 						errorCodeDescriptionMap().get(SEMANTIC_ERROR_CODE),
		    	 						List.of(String.format(UNRECOGNIZED_ERROR, "deliveryFailureCause", progressStatusEvent.getDeliveryFailureCause()))));	
		}
		// Attachments non e' una lista obbligatoria
		if (progressStatusEvent.getAttachments() != null && !progressStatusEvent.getAttachments().isEmpty()) {
			for (ConsolidatoreIngressPaperProgressStatusEventAttachments attachment: progressStatusEvent.getAttachments()) {
				if (!attachmentDocumentTypeMap().contains(attachment.getDocumentType())) {
	 				return Mono.error(new RicezioneEsitiCartaceoException(
	 						SEMANTIC_ERROR_CODE, 
	 						errorCodeDescriptionMap().get(SEMANTIC_ERROR_CODE),
	 						List.of(String.format(UNRECOGNIZED_ERROR, "attachment.documentType", attachment.getDocumentType()))));
				}
			}
		}
		return Mono.just(getOperationResultCodeResponse(COMPLETED_OK_CODE, COMPLETED_MESSAGE, null));
	}
	
	private Mono<OperationResultCodeResponse> verificaAttachments(
			String xPagopaExtchServiceId, String requestId,
			List<ConsolidatoreIngressPaperProgressStatusEventAttachments> attachments) 
				throws RicezioneEsitiCartaceoException // uri formalmente scorretto
	{
		return Flux.fromIterable(attachments)
			.switchIfEmpty(Mono.error(new AttachmentsEmptyRicezioneEsitiCartaceoException()))
			.flatMap(attachment -> {
				if (attachment.getUri().contains(SS_IN_URI)) {
					String documentKey = attachment.getUri().replace(SS_IN_URI, "");
					log.info("RicezioneEsitiCartaceoServiceImpl.verificaAttachments() : "
							+ "attachment.uri = {} -> documentKey = {}",
							attachment.getUri(), documentKey);
					// se ok (httpstatus 200), continuo (verifica andata a buon fine)
					return Mono.just(documentKey);
				}
				return Mono.error(new RicezioneEsitiCartaceoException(
								  SEMANTIC_ERROR_CODE, 
								  errorCodeDescriptionMap().get(SEMANTIC_ERROR_CODE),
								  List.of(String.format(UNRECOGNIZED_ERROR, "attachment.uri", attachment.getUri()))));
			})
			.flatMap(documentKey -> fileCall.getFile(documentKey, xPagopaExtchServiceId, true))
			// se non si verificano errori, procedo e non mi interssa il risultato
			.collectList()
			.flatMap(unused -> Mono.just(getOperationResultCodeResponse(COMPLETED_OK_CODE, COMPLETED_MESSAGE, null)))
			// ***
			.onErrorResume(AttachmentsEmptyRicezioneEsitiCartaceoException.class, 
						   throwable -> {
								 log.info("RicezioneEsitiCartaceoServiceImpl.verificaAttachments() : "
								 		+ "there aren't Attachments for requestId \"{}\"",
										  requestId);
								 return Mono.just(new OperationResultCodeResponse());
			})
			.onErrorResume(RicezioneEsitiCartaceoException.class, 
					   throwable -> {
							 log.error("RicezioneEsitiCartaceoServiceImpl.verificaAttachments() : "
							 		+ "requestId = {}, errore attachment uri = {}",
									  requestId, throwable.getErrorList().get(0), throwable);
							 return Mono.error(throwable);
			})
			.onErrorResume(RuntimeException.class, 
					   throwable -> {
							 log.error("RicezioneEsitiCartaceoServiceImpl.verificaAttachments() : "
							 		+ "requestId = {} : errore generico = {}",
									  requestId, throwable.getMessage(), throwable);
							 return Mono.error(throwable);
			});
	}

	@Override
	public Mono<OperationResultCodeResponse> verificaEsitoDaConsolidatore(
			String xPagopaExtchServiceId, ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent) 
	{
		 return Mono.just(progressStatusEvent)
			     .doOnNext(unused -> {
			    	 log.info("RicezioneEsitiCartaceoServiceImpl.verificaEsitoDaConsolidatore() : START for requestId \"{}\"",
			    			 progressStatusEvent.getRequestId());
			    	 log.info("RicezioneEsitiCartaceoServiceImpl.verificaEsitoDaConsolidatore() : xPagopaExtchServiceId = {} : progressStatusEvent {}",
			    			 xPagopaExtchServiceId, progressStatusEvent);
			     })
			     .flatMap(unused -> gestoreRepositoryCall.getRichiesta(progressStatusEvent.getRequestId()))
			     .flatMap(unused -> verificaErroriSemantici(progressStatusEvent))
			     .flatMap(unused -> verificaAttachments(xPagopaExtchServiceId, progressStatusEvent.getRequestId(), progressStatusEvent.getAttachments()))
			     .flatMap(unused -> Mono.just(getOperationResultCodeResponse(COMPLETED_OK_CODE, COMPLETED_MESSAGE, null)))
			     // *** errore Request Id non trovata
			     .onErrorResume(RestCallException.ResourceNotFoundException.class, throwable -> {
		    		 log.error("RicezioneEsitiCartaceoServiceImpl.verificaEsitoDaConsolidatore() : request id = {} : errore  = {} ",
		    				   progressStatusEvent.getRequestId(), throwable.getMessage(), throwable);
			    	 return Mono.just(getOperationResultCodeResponse(REQUEST_ID_ERROR_CODE, 
		    			 										     errorCodeDescriptionMap().get(REQUEST_ID_ERROR_CODE), 
		    			 										     List.of("requestId unrecognized")));
			     })
			     // *** errore semantico
			     .onErrorResume(RicezioneEsitiCartaceoException.class, throwable -> {
		    		 log.error("RicezioneEsitiCartaceoServiceImpl.verificaEsitoDaConsolidatore() : errore ResultCode = {} : errore ResultDescription = {} :",
		    				   throwable.getResultCode(), throwable.getResultDescription(), throwable);
			    	 return Mono.just(getOperationResultCodeResponse(throwable.getResultCode(), 
	 															     throwable.getResultDescription(), 
	 															     throwable.getErrorList()));
			     })
			     // *** errore generico
			     .onErrorResume(RuntimeException.class, throwable -> {
		    		 log.error("RicezioneEsitiCartaceoServiceImpl.verificaEsitoDaConsolidatore() : errore generico : message = {} :",
		    				   throwable.getMessage(), throwable);
			    	 return Mono.just(getOperationResultCodeResponse(INTERNAL_SERVER_ERROR_CODE, 
	 															     errorCodeDescriptionMap().get(INTERNAL_SERVER_ERROR_CODE),
	 															     List.of(throwable.getMessage())));
			     })
			     ;
	}
	
	@Override
	public Mono<OperationResultCodeResponse> pubblicaEsitoCodaNotificationTracker(
			String xPagopaExtchServiceId,
			ConsolidatoreIngressPaperProgressStatusEvent consolidatoreIngressPaperProgressStatusEvent) 
	{
		return Mono.just(consolidatoreIngressPaperProgressStatusEvent)
			.flatMap(progressStatusEvent -> {
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
			.flatMap(unused -> Mono.just(getOperationResultCodeResponse(COMPLETED_OK_CODE, COMPLETED_MESSAGE, null)))
			.onErrorResume(SqsPublishException.class, throwable -> {
	    		 log.error("RicezioneEsitiCartaceoServiceImpl.pubblicaEsitoCodaNotificationTracker() : errore pubblicazione coda : message = {} :",
	    				   throwable.getMessage(), throwable);
	    		 return Mono.just(getOperationResultCodeResponse(INTERNAL_SERVER_ERROR_CODE, 
	    				 										 errorCodeDescriptionMap().get(INTERNAL_SERVER_ERROR_CODE), 
	    				 										 List.of(throwable.getMessage())));
			})
			.onErrorResume(RuntimeException.class, throwable -> {
	    		 log.error("RicezioneEsitiCartaceoServiceImpl.pubblicaEsitoCodaNotificationTracker() : errore generico : message = {} :",
	    				   throwable.getMessage(), throwable);
	    		 return Mono.just(getOperationResultCodeResponse(INTERNAL_SERVER_ERROR_CODE, 
	    				 										 errorCodeDescriptionMap().get(INTERNAL_SERVER_ERROR_CODE), 
	    				 										 List.of(throwable.getMessage())));
			});
	}

}
