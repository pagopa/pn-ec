package it.pagopa.pn.ec.consolidatore.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic400ErrorException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsClientException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.AttachmentNotAvailableException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.consolidatore.dto.RicezioneEsitiDto;
import it.pagopa.pn.ec.consolidatore.exception.AttachmentsEmptyRicezioneEsitiCartaceoException;
import it.pagopa.pn.ec.consolidatore.exception.RicezioneEsitiCartaceoException;
import it.pagopa.pn.ec.consolidatore.service.RicezioneEsitiCartaceoService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.consolidatore.utils.PaperConstant.*;
import static it.pagopa.pn.ec.consolidatore.utils.PaperElem.*;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.*;

@Service
@Slf4j
public class RicezioneEsitiCartaceoServiceImpl implements RicezioneEsitiCartaceoService {

	private final GestoreRepositoryCall gestoreRepositoryCall;
	private final FileCall fileCall;
	private final ObjectMapper objectMapper;
	private final NotificationTrackerSqsName notificationTrackerSqsName;
	private final SqsService sqsService;

	private final StatusPullService statusPullService;

	public RicezioneEsitiCartaceoServiceImpl(GestoreRepositoryCall gestoreRepositoryCall,
											 FileCall fileCall, ObjectMapper objectMapper, NotificationTrackerSqsName notificationTrackerSqsName,
											 SqsService sqsService, StatusPullService statusPullService) {
		super();
		this.gestoreRepositoryCall = gestoreRepositoryCall;
		this.fileCall = fileCall;
		this.objectMapper = objectMapper;
		this.notificationTrackerSqsName = notificationTrackerSqsName;
		this.sqsService = sqsService;
		this.statusPullService = statusPullService;
	}

	private OperationResultCodeResponse getOperationResultCodeResponse(
			String resultCode, String resultDescription, List<String> errorList) {
		OperationResultCodeResponse response = new OperationResultCodeResponse();
		response.setResultCode(resultCode);
		response.setResultDescription(resultDescription);
		response.setErrorList(errorList);
		return response;
	}

	// errore semantico -> resultDescription: 'Semantic Error'
	private Mono<OperationResultCodeResponse> verificaErroriSemantici(ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent, String xPagopaExtchServiceId)
			throws RicezioneEsitiCartaceoException
	{
		final String LOG_LABEL = "RicezioneEsitiCartaceoServiceImpl.verificaErroriSemantici() ";
		final String ERROR_LABEL = "error = {}";
		log.info(LOG_LABEL + "START for requestId \"{}\"", progressStatusEvent.getRequestId());

		return statusPullService.paperPullService(progressStatusEvent.getRequestId(), xPagopaExtchServiceId)
				.flatMap(progressStatusEventToCheck ->
				{
					var iun = progressStatusEventToCheck.getIun();
					var productType = progressStatusEventToCheck.getProductType();

					List<String> errorList = new ArrayList<>();

					//TODO COMMENTATO PER UN CASO PARTICOLARE CHE ANDRA' GESTITO IN FUTURO.
//					if (!progressStatusEvent.getProductType().equals(productType)) {
//						log.debug(LOG_LABEL + ERROR_LABEL, String.format(UNRECOGNIZED_ERROR, PRODUCT_TYPE_LABEL, progressStatusEvent.getStatusCode()));
//						errorList.add(String.format(UNRECOGNIZED_ERROR, PRODUCT_TYPE_LABEL, productType));
//					}
					//Iun
					if (!StringUtils.isBlank(iun) && !progressStatusEvent.getIun().equals(iun)) {
						log.debug(LOG_LABEL + ERROR_LABEL, String.format(UNRECOGNIZED_ERROR, IUN_LABEL, progressStatusEvent.getStatusCode()));
						errorList.add(String.format(UNRECOGNIZED_ERROR, IUN_LABEL, iun));
					}
					// StatusCode
					if (!statusCodeDescriptionMap().containsKey(progressStatusEvent.getStatusCode())) {
						log.debug(LOG_LABEL + ERROR_LABEL, String.format(UNRECOGNIZED_ERROR, STATUS_CODE_LABEL, progressStatusEvent.getStatusCode()));
						errorList.add(String.format(UNRECOGNIZED_ERROR, STATUS_CODE_LABEL, progressStatusEvent.getStatusCode()));
					}
					// ProductTypeMap
					if (!productTypeMap().containsKey(progressStatusEvent.getProductType())) {
						log.debug(LOG_LABEL + ERROR_LABEL, String.format(UNRECOGNIZED_ERROR, PRODUCT_TYPE_LABEL, progressStatusEvent.getProductType()));
						errorList.add(String.format(UNRECOGNIZED_ERROR, PRODUCT_TYPE_LABEL, progressStatusEvent.getProductType()));
					}
					// DeliveryFailureCause non è un campo obbligatorio
					if (progressStatusEvent.getDeliveryFailureCause() != null
							&& !progressStatusEvent.getDeliveryFailureCause().isBlank()
							&& !deliveryFailureCausemap().containsKey(progressStatusEvent.getDeliveryFailureCause())) {
						log.debug(LOG_LABEL + ERROR_LABEL, String.format(UNRECOGNIZED_ERROR, DELIVERY_FAILURE_CAUSE_LABEL, progressStatusEvent.getDeliveryFailureCause()));
						errorList.add(String.format(UNRECOGNIZED_ERROR, DELIVERY_FAILURE_CAUSE_LABEL, progressStatusEvent.getDeliveryFailureCause()));
					}
					// Attachments non e' una lista obbligatoria
					if (progressStatusEvent.getAttachments() != null && !progressStatusEvent.getAttachments().isEmpty()) {
						for (ConsolidatoreIngressPaperProgressStatusEventAttachments attachment : progressStatusEvent.getAttachments()) {
							if (!attachmentDocumentTypeMap().contains(attachment.getDocumentType())) {
								log.debug(LOG_LABEL + ERROR_LABEL, String.format(UNRECOGNIZED_ERROR, ATTACHMENT_DOCUMENT_TYPE_LABEL, attachment.getDocumentType()));
								errorList.add(String.format(UNRECOGNIZED_ERROR, ATTACHMENT_DOCUMENT_TYPE_LABEL, attachment.getDocumentType()));
							}
						}
					}
						return Mono.just(errorList);
					})
				.handle((errorList, syncrhonousSink) ->
				{
					if (errorList.isEmpty()) {
						log.debug(LOG_LABEL + "END without errors for requestId \"{}\"", progressStatusEvent.getRequestId());
						syncrhonousSink.next(getOperationResultCodeResponse(COMPLETED_OK_CODE, COMPLETED_MESSAGE, null));
					} else syncrhonousSink.error(new RicezioneEsitiCartaceoException(
							SEMANTIC_ERROR_CODE,
							errorCodeDescriptionMap().get(SEMANTIC_ERROR_CODE),
							errorList));
				});
	}


	private Mono<OperationResultCodeResponse> verificaAttachments(
			String xPagopaExtchServiceId, String requestId,
			List<ConsolidatoreIngressPaperProgressStatusEventAttachments> attachments)
				throws RicezioneEsitiCartaceoException // uri formalmente scorretto
	{
		final String LOG_LABEL = "RicezioneEsitiCartaceoServiceImpl.verificaAttachments() : ";
		log.info(LOG_LABEL + "START : requestId \"{}\" : xPagopaExtchServiceId = {} : attachments = {}",
				 requestId, xPagopaExtchServiceId, attachments);

		if (attachments == null || attachments.isEmpty()) {
			log.debug(LOG_LABEL + "END without attachments");
			return Mono.just(getOperationResultCodeResponse(COMPLETED_OK_CODE, COMPLETED_MESSAGE, null));
		}
		return Flux.fromIterable(attachments)
			.switchIfEmpty(Mono.error(new AttachmentsEmptyRicezioneEsitiCartaceoException()))
			.flatMap(attachment -> {
				if (attachment.getUri().contains(SS_IN_URI)) {
					String documentAttachmentKey = attachment.getUri().replace(SS_IN_URI, "");
					log.debug(LOG_LABEL + "attachment.uri = {} -> documentKey = {}", attachment.getUri(), documentAttachmentKey);
					// se ok (httpstatus 200), continuo (verifica andata a buon fine)
					return Mono.just(documentAttachmentKey);
				}
				return Mono.error(new RicezioneEsitiCartaceoException(
										  SEMANTIC_ERROR_CODE,
										  errorCodeDescriptionMap().get(SEMANTIC_ERROR_CODE),
										  List.of(String.format(UNRECOGNIZED_ERROR, ATTACHMENT_URI_LABEL, attachment.getUri()))));
			})
			.flatMap(documentAttachmentKey -> fileCall.getFile(documentAttachmentKey, xPagopaExtchServiceId, true))
			// se non si verificano errori, procedo e non mi interssa il risultato
			.collectList()
			.flatMap(unused -> {
				log.debug(LOG_LABEL + "END without errors");
				return Mono.just(getOperationResultCodeResponse(COMPLETED_OK_CODE, COMPLETED_MESSAGE, null));
			})
			// ***
			.onErrorResume(AttachmentsEmptyRicezioneEsitiCartaceoException.class,
						   throwable -> {
								 log.debug(LOG_LABEL + "there aren't Attachments for requestId \"{}\"", requestId);
								 return Mono.just(new OperationResultCodeResponse());
			})
			.onErrorResume(RicezioneEsitiCartaceoException.class,
					   throwable -> {
							 log.debug(LOG_LABEL + "requestId = {}, errore attachment uri = {}",
									   requestId, throwable.getErrorList().get(0), throwable);
							 return Mono.error(throwable);
			})
			.onErrorResume(Generic400ErrorException.class,
					   throwable -> {
							 log.debug(LOG_LABEL + "requestId = {}, errore attachment -> title = {}, details {}",
									   requestId, throwable.getTitle(), throwable.getDetails(), throwable);
							 return Mono.error(new RicezioneEsitiCartaceoException(
												  SEMANTIC_ERROR_CODE,
												  errorCodeDescriptionMap().get(SEMANTIC_ERROR_CODE),
												  List.of(String.format(UNRECOGNIZED_ERROR_NO_VALUE, ATTACHMENT_URI_LABEL))));
			})
			.onErrorResume(AttachmentNotAvailableException.class,
					   throwable -> {
							 log.debug(LOG_LABEL + "requestId = {}, errore attachment = {}",
									   requestId, throwable.getMessage(), throwable);
							 return Mono.error(new RicezioneEsitiCartaceoException(
												  SEMANTIC_ERROR_CODE,
												  errorCodeDescriptionMap().get(SEMANTIC_ERROR_CODE),
												  List.of(String.format(UNRECOGNIZED_ERROR_NO_VALUE, ATTACHMENT_URI_LABEL))));
			})
			.onErrorResume(RuntimeException.class,
					   throwable -> {
							 log.error(LOG_LABEL + "requestId = {} : errore generico = {}",
									  requestId, throwable.getMessage(), throwable);
							 return Mono.error(throwable);
			});
	}

	@Override
	public Mono<RicezioneEsitiDto> verificaEsitoDaConsolidatore(
			String xPagopaExtchServiceId, ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent)
	{
		  return Mono.just(progressStatusEvent)
			     .doOnNext(unused -> {
			    	 log.info(LOG_VERIF_LABEL + "START for requestId \"{}\"", progressStatusEvent.getRequestId());
			    	 log.info(LOG_VERIF_LABEL + "xPagopaExtchServiceId = {} : progressStatusEvent = {}",
			    			 xPagopaExtchServiceId, progressStatusEvent);
			     })
				 .flatMap(unused -> gestoreRepositoryCall.getRichiesta(xPagopaExtchServiceId, progressStatusEvent.getRequestId()))
			     .flatMap(unused -> verificaErroriSemantici(progressStatusEvent, xPagopaExtchServiceId))
			     .flatMap(unused -> verificaAttachments(xPagopaExtchServiceId, progressStatusEvent.getRequestId(), progressStatusEvent.getAttachments()))
			     .flatMap(unused -> Mono.just(new RicezioneEsitiDto(progressStatusEvent,
											 			 		    getOperationResultCodeResponse(COMPLETED_OK_CODE,
											 			 		    							   COMPLETED_MESSAGE,
											 			 		    							   null)))
			     )
			     // *** errore Request Id non trovata
			     .onErrorResume(RestCallException.ResourceNotFoundException.class, throwable -> {
		    		 log.debug(LOG_VERIF_LABEL + "request id = {} : errore  = {} ",
		    				   progressStatusEvent.getRequestId(), throwable.getMessage(), throwable);
			    	 return Mono.just(new RicezioneEsitiDto(progressStatusEvent,
			    			 								getOperationResultCodeResponse(REQUEST_ID_ERROR_CODE,
																					       errorCodeDescriptionMap().get(REQUEST_ID_ERROR_CODE),
																					       List.of(String.format("requestId '%s' unrecognized", progressStatusEvent.getRequestId())))));
			     })
			     // *** errore semantico
			     .onErrorResume(RicezioneEsitiCartaceoException.class, throwable -> {
		    		 log.debug("RicezioneEsitiCartaceoServiceImpl.verificaEsitoDaConsolidatore() : errore ResultCode = {} : errore ResultDescription = {} :",
		    				   throwable.getResultCode(), throwable.getResultDescription(), throwable);
			    	 return Mono.just(new RicezioneEsitiDto(progressStatusEvent,
								    			 		    getOperationResultCodeResponse(throwable.getResultCode(),
																						   throwable.getResultDescription(),
																						   throwable.getErrorList())));
			     })
			     // *** errore generico
			     .onErrorResume(RuntimeException.class, throwable -> {
		    		 log.error("RicezioneEsitiCartaceoServiceImpl.verificaEsitoDaConsolidatore() : errore generico : message = {} :",
		    				   throwable.getMessage(), throwable);
			    	 return Mono.just(new RicezioneEsitiDto(progressStatusEvent,
							    			 		       getOperationResultCodeResponse(INTERNAL_SERVER_ERROR_CODE,
							    			 		    		   						  errorCodeDescriptionMap().get(INTERNAL_SERVER_ERROR_CODE),
							    			 		    		   						  List.of(throwable.getMessage()))));
			     });
	}

	@Override
	public Mono<OperationResultCodeResponse> pubblicaEsitoCodaNotificationTracker(
			String xPagopaExtchServiceId,
			ConsolidatoreIngressPaperProgressStatusEvent statusEvent)
	{
		return statusPullService.paperPullService(statusEvent.getRequestId(), xPagopaExtchServiceId)
			.flatMap(unused -> {

				log.info(LOG_PUB_LABEL + "START : xPagopaExtchServiceId = {} : statusEvent = {}",
						xPagopaExtchServiceId, statusEvent);

				PresaInCaricoInfo presaInCaricoInfo = new PresaInCaricoInfo();
	 			presaInCaricoInfo.setRequestIdx(statusEvent.getRequestId());
	 			presaInCaricoInfo.setXPagopaExtchCxId(xPagopaExtchServiceId);

	 			DiscoveredAddressDto discoveredAddressDto = null;
	 			if (statusEvent.getDiscoveredAddress() != null) {
	 				discoveredAddressDto = objectMapper.convertValue(statusEvent.getDiscoveredAddress(), DiscoveredAddressDto.class);
	 			}

	 			List<AttachmentsProgressEventDto> attachmentsDto = new ArrayList<>();
	 			if (statusEvent.getAttachments() != null && !statusEvent.getAttachments().isEmpty()) {
	 				statusEvent.getAttachments().forEach(attachment -> {
	 					AttachmentsProgressEventDto attachmentDto = objectMapper.convertValue(attachment, AttachmentsProgressEventDto.class);
	 					attachmentDto.setId(attachment.getId());
	 					attachmentsDto.add(attachmentDto);
	 				});
	 			}

	 			PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto();
	 			paperProgressStatusDto.setStatusCode(statusEvent.getStatusCode());
	 			paperProgressStatusDto.setStatusDescription(statusEvent.getStatusDescription());
	 			paperProgressStatusDto.setStatusDateTime(statusEvent.getStatusDateTime());
	 			paperProgressStatusDto.setDeliveryFailureCause(statusEvent.getDeliveryFailureCause());
	 			paperProgressStatusDto.setRegisteredLetterCode(statusEvent.getRegisteredLetterCode());
				paperProgressStatusDto.setIun(statusEvent.getIun());
				paperProgressStatusDto.setProductType(statusEvent.getProductType());
	 			paperProgressStatusDto.setDiscoveredAddress(discoveredAddressDto);
	 			paperProgressStatusDto.setAttachments(attachmentsDto);

				log.debug(LOG_PUB_LABEL + "paperProgressStatusDto = {}", paperProgressStatusDto);

	 			return sqsService.send(notificationTrackerSqsName.statoCartaceoName(),
	 								   NotificationTrackerQueueDto.createNotificationTrackerQueueDtoPaper(
	 										   presaInCaricoInfo,
	 										   paperProgressStatusDto.getStatusCode(),
	 										   paperProgressStatusDto));
			})
			.flatMap(unused -> Mono.just(getOperationResultCodeResponse(COMPLETED_OK_CODE, COMPLETED_MESSAGE, null)))
			.onErrorResume(SqsClientException.class, throwable -> {
	    		 log.debug("RicezioneEsitiCartaceoServiceImpl.pubblicaEsitoCodaNotificationTracker() : errore pubblicazione coda : message = {} :",
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
