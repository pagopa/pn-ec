package it.pagopa.pn.ec.consolidatore.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.cartaceo.model.pojo.StatusCodesToDeliveryFailureCauses;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.StatusNotFoundException;
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
import it.pagopa.pn.ec.consolidatore.exception.NoSuchEventException;
import it.pagopa.pn.ec.consolidatore.model.pojo.ConsAuditLogError;
import it.pagopa.pn.ec.consolidatore.model.dto.RicezioneEsitiDto;
import it.pagopa.pn.ec.consolidatore.exception.RicezioneEsitiCartaceoException;
import it.pagopa.pn.ec.consolidatore.service.RicezioneEsitiCartaceoService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.commons.constant.Status.BOOKED;
import static it.pagopa.pn.ec.commons.constant.Status.SENT;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.consolidatore.constant.ConsAuditLogEventType.*;
import static it.pagopa.pn.ec.consolidatore.utils.PaperConstant.*;
import static it.pagopa.pn.ec.consolidatore.utils.PaperElem.*;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.*;

@Service
@CustomLog
public class RicezioneEsitiCartaceoServiceImpl implements RicezioneEsitiCartaceoService {

	private final GestoreRepositoryCall gestoreRepositoryCall;
	private final FileCall fileCall;
	private final ObjectMapper objectMapper;
	private final NotificationTrackerSqsName notificationTrackerSqsName;
	private final SqsService sqsService;
	private final StatusCodesToDeliveryFailureCauses statusCodesToDeliveryFailureCauses;
	private final StatusPullService statusPullService;
	private boolean considerEventsWithoutSentStatusAsBooked;
	private final Duration offsetDuration;


	public RicezioneEsitiCartaceoServiceImpl(GestoreRepositoryCall gestoreRepositoryCall,
											 FileCall fileCall, ObjectMapper objectMapper, NotificationTrackerSqsName notificationTrackerSqsName,
											 SqsService sqsService, StatusCodesToDeliveryFailureCauses statusCodesToDeliveryFailureCauses, StatusPullService statusPullService,
											 @Value("${ricezione-esiti-cartaceo.consider-event-without-sent-status-as-booked}") boolean considerEventsWithoutStatusAsBooked,
											 @Value("${ricezione-esiti-cartaceo.allowed-future-offset-duration}") Duration offsetDuration) {
		super();
		this.gestoreRepositoryCall = gestoreRepositoryCall;
		this.fileCall = fileCall;
		this.objectMapper = objectMapper;
		this.notificationTrackerSqsName = notificationTrackerSqsName;
		this.sqsService = sqsService;
		this.statusCodesToDeliveryFailureCauses = statusCodesToDeliveryFailureCauses;
		this.statusPullService = statusPullService;
		this.considerEventsWithoutSentStatusAsBooked = considerEventsWithoutStatusAsBooked;
		if (offsetDuration== null){
			this.offsetDuration = Duration.ofSeconds(-1);
		} else {
			this.offsetDuration = offsetDuration;
		}
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
	private Mono<OperationResultCodeResponse> verificaErroriSemantici(ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent, RequestDto requestDto, String xPagopaExtchServiceId)
			throws RicezioneEsitiCartaceoException {
		var requestId = progressStatusEvent.getRequestId();
		log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, VERIFICA_ERRORI_SEMANTICI, progressStatusEvent);

		return statusPullService.paperPullService(requestId, xPagopaExtchServiceId)
				.onErrorResume(StatusNotFoundException.class, throwable -> {
					log.warn(EXCEPTION_IN_PROCESS_FOR, VERIFICA_ERRORI_SEMANTICI, requestId, throwable, throwable.getMessage());
					ConsAuditLogError consAuditLogError = new ConsAuditLogError()
							.requestId(requestId)
							.error(ERR_CONS_BAD_STATUS.getValue())
							.description("Unable to decode last status");
					return Mono.error(new RicezioneEsitiCartaceoException(
							SEMANTIC_ERROR_CODE,
							errorCodeDescriptionMap().get(SEMANTIC_ERROR_CODE),
							List.of("Unable to decode last status"),
							List.of(consAuditLogError)
					));
				})
				.map(progressStatusEventToCheck ->
						checkProgressStatusEvent(progressStatusEventToCheck, progressStatusEvent, requestDto, requestId)
				)
				.handle((tuple, syncrhonousSink) -> {
					var errorList = tuple.getT1();
					var auditLogErrorList = tuple.getT2();

					if (errorList.isEmpty()) {
						syncrhonousSink.next(getOperationResultCodeResponse(
								COMPLETED_OK_CODE,
								COMPLETED_MESSAGE,
								null
						));
					} else {
						syncrhonousSink.error(new RicezioneEsitiCartaceoException(
								SEMANTIC_ERROR_CODE,
								errorCodeDescriptionMap().get(SEMANTIC_ERROR_CODE),
								errorList,
								auditLogErrorList
						));
					}
				})
				.cast(OperationResultCodeResponse.class)
				.doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, requestId, VERIFICA_ERRORI_SEMANTICI, result));
	}

	private Tuple2<List<String>, List<ConsAuditLogError>> checkProgressStatusEvent(PaperProgressStatusEvent progressStatusEventToCheck, ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent, RequestDto requestDto, String requestId) {

		List<String> errorList = new ArrayList<>();
		List<ConsAuditLogError> auditLogErrorList = new ArrayList<>();

		OffsetDateTime now = OffsetDateTime.now();

		validateStatusDateTime(progressStatusEvent, requestDto, now, errorList, auditLogErrorList, requestId);
		validateClientRequestTimestamp(progressStatusEvent, now, errorList, auditLogErrorList, requestId);
		validateIun(progressStatusEventToCheck, progressStatusEvent, errorList, auditLogErrorList, requestId);
		validateStatusCode(progressStatusEvent, errorList, auditLogErrorList, requestId);
		validateDeliveryFailureCause(progressStatusEvent, errorList, auditLogErrorList, requestId);
		validateAttachmentsForRecStatusCode(progressStatusEvent, errorList, auditLogErrorList, requestId);

		return Tuples.of(errorList, auditLogErrorList);
	}

	private void validateStatusDateTime(ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent, RequestDto requestDto, OffsetDateTime now, List<String> errorList, List<ConsAuditLogError> auditLogErrorList, String requestId) {
		OffsetDateTime statusDateTime = progressStatusEvent.getStatusDateTime();
		EventsDto sentEvent = getSentOrBookedEvent(requestDto);

		if (statusDateTime.isBefore(sentEvent.getPaperProgrStatus().getStatusDateTime())) {
			addError(errorList, auditLogErrorList, requestId, ERR_CONS_BAD_STATUS_DATE_TIME.getValue(), "Status date time is not valid.",
					STATUS_DATE_TIME_LABEL, statusDateTime);
		}

		if (!offsetDuration.isNegative()) {
			OffsetDateTime nowWithOffset = now.plus(offsetDuration);
			if (statusDateTime.isAfter(nowWithOffset)) {
				addError(errorList, auditLogErrorList, requestId, ERR_CONS_BAD_STATUS_DATE_TIME.getValue(), "Status date time is in the future.",
						STATUS_DATE_TIME_LABEL, statusDateTime);
			}
		}
	}

	private void validateClientRequestTimestamp(ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent, OffsetDateTime now, List<String> errorList, List<ConsAuditLogError> auditLogErrorList, String requestId) {
		OffsetDateTime clientRequestTimestamp = progressStatusEvent.getClientRequestTimeStamp();
		if (!offsetDuration.isNegative()) {
			OffsetDateTime nowWithOffset = now.plus(offsetDuration);
			if (clientRequestTimestamp.isAfter(nowWithOffset)) {
				addError(errorList, auditLogErrorList, requestId, ERR_CONS_BAD_CLIENT_REQUEST_TIMESTAMP.getValue(),
						"Client request timestamp is in the future.", CLIENT_REQUEST_TIMESTAMP_LABEL, clientRequestTimestamp);
			}
		}
	}

	private void validateIun(PaperProgressStatusEvent progressStatusEventToCheck, ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent, List<String> errorList, List<ConsAuditLogError> auditLogErrorList, String requestId) {
		String iun = progressStatusEventToCheck.getIun();
		if (!StringUtils.isBlank(iun) && !progressStatusEvent.getIun().equals(iun)) {
			addError(errorList, auditLogErrorList, requestId, ERR_CONS_BAD_IUN.getValue(), "Iun is not valid.", IUN_LABEL, iun);
		}
	}

	private void validateStatusCode(ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent, List<String> errorList, List<ConsAuditLogError> auditLogErrorList, String requestId) {
		if (!statusCodeDescriptionMap().containsKey(progressStatusEvent.getStatusCode())) {
			addError(errorList, auditLogErrorList, requestId, ERR_CONS_BAD_STATUS_CODE.getValue(), "Status code is not valid.",
					STATUS_CODE_LABEL, progressStatusEvent.getStatusCode());
		}
	}

	private void validateDeliveryFailureCause(ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent, List<String> errorList, List<ConsAuditLogError> auditLogErrorList, String requestId) {
		String deliveryFailureCause = progressStatusEvent.getDeliveryFailureCause();
		if (deliveryFailureCause != null && !deliveryFailureCause.isBlank()) {
			boolean isValid = deliveryFailureCausemap().containsKey(deliveryFailureCause) &&
					statusCodesToDeliveryFailureCauses.isDeliveryFailureCauseInStatusCode(progressStatusEvent.getStatusCode(),
							deliveryFailureCause);

			if (!isValid) {
				addError(errorList, auditLogErrorList, requestId, ERR_CONS_BAD_DEL_FAILURE_CAUSE.getValue(),
						"DeliveryFailureCause is not valid.", DELIVERY_FAILURE_CAUSE_LABEL, deliveryFailureCause);
			}
		}
	}

	private void validateAttachmentsForRecStatusCode(ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent, List<String> errorList, List<ConsAuditLogError> auditLogErrorList, String requestId) {
		if (progressStatusEvent.getStatusCode().startsWith("REC") && progressStatusEvent.getAttachments() != null) {
			for (ConsolidatoreIngressPaperProgressStatusEventAttachments attachment : progressStatusEvent.getAttachments()) {
				if (!attachmentDocumentTypeMap().contains(attachment.getDocumentType())) {
					addError(errorList, auditLogErrorList, requestId, ERR_CONS_BAD_DOC_TYPE.getValue(), "Document type is not valid.",
							ATTACHMENT_DOCUMENT_TYPE_LABEL, attachment.getDocumentType());
				}
			}
		}
	}

	private EventsDto getSentOrBookedEvent(RequestDto requestDto) {
		return requestDto.getRequestMetadata().getEventsList()
				.stream()
				.filter(eventsDto -> eventsDto.getPaperProgrStatus().getStatus().equals(SENT.getStatusTransactionTableCompliant()))
				.findFirst()
				.orElseGet(() -> {
					if (considerEventsWithoutSentStatusAsBooked) {
						return requestDto.getRequestMetadata().getEventsList()
								.stream()
								.filter(eventsDto -> eventsDto.getPaperProgrStatus().getStatus().equals(BOOKED.getStatusTransactionTableCompliant()))
								.findFirst()
								.orElseThrow(() -> new NoSuchEventException(BOOKED.getStatusTransactionTableCompliant()));
					} else {
						throw new NoSuchEventException(SENT.getStatusTransactionTableCompliant());
					}
				});
	}

	private void addError(List<String> errorList, List<ConsAuditLogError> auditLogErrorList, String requestId,
						  String errorCode, String description, String label, Object value) {
		auditLogErrorList.add(new ConsAuditLogError().requestId(requestId).error(errorCode).description(description));
		errorList.add(String.format(UNRECOGNIZED_ERROR, label, value));
	}


	private Mono<OperationResultCodeResponse> verificaAttachments(
			String xPagopaExtchServiceId, String requestId,
			List<ConsolidatoreIngressPaperProgressStatusEventAttachments> attachments)
				throws RicezioneEsitiCartaceoException // uri formalmente scorretto
	{
		log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS + " - {}", VERIFICA_ATTACHMENTS, requestId, attachments);

		if (attachments == null || attachments.isEmpty()) {
			log.debug(VERIFICA_ATTACHMENTS + ": END without attachments");
			return Mono.just(getOperationResultCodeResponse(COMPLETED_OK_CODE, COMPLETED_MESSAGE, null));
		}
		return Flux.fromIterable(attachments)
			.flatMap(attachment -> {
				if (attachment.getUri().contains(SS_IN_URI)) {
					String documentAttachmentKey = attachment.getUri().replace(SS_IN_URI, "");
					// se ok (httpstatus 200), continuo (verifica andata a buon fine)
					return Mono.just(documentAttachmentKey);
				}
				var consAuditLogError = new ConsAuditLogError().error(ERR_CONS_BAD_URI.getValue()).requestId(requestId).description("The attachment uri is not valid.");
				return Mono.error(new RicezioneEsitiCartaceoException(
						SEMANTIC_ERROR_CODE,
						errorCodeDescriptionMap().get(SEMANTIC_ERROR_CODE),
						List.of(String.format(UNRECOGNIZED_ERROR, ATTACHMENT_URI_LABEL, attachment.getUri())), List.of(consAuditLogError)));
			})
			.flatMap(documentAttachmentKey -> fileCall.getFile(documentAttachmentKey, xPagopaExtchServiceId, true))
			// se non si verificano errori, procedo e non mi interssa il risultato
			.collectList()
			.flatMap(unused -> {
				log.debug(VERIFICA_ATTACHMENTS + ": END without errors for request '{}'", requestId);
				return Mono.just(getOperationResultCodeResponse(COMPLETED_OK_CODE, COMPLETED_MESSAGE, null));
			})
			.onErrorResume(Generic400ErrorException.class,
					   throwable -> {
							 log.warn(EXCEPTION_IN_PROCESS_FOR, VERIFICA_ATTACHMENTS, requestId, throwable, throwable.getMessage());
							 return Mono.error(new RicezioneEsitiCartaceoException(
												  SEMANTIC_ERROR_CODE,
												  errorCodeDescriptionMap().get(SEMANTIC_ERROR_CODE),
												  List.of(String.format(UNRECOGNIZED_ERROR_NO_VALUE, ATTACHMENT_URI_LABEL)), null));
			})
			.onErrorResume(AttachmentNotAvailableException.class,
					throwable -> {
						var consAuditLogError = new ConsAuditLogError().error(ERR_CONS_ATTACH_NOT_FOUND.getValue()).requestId(requestId).description("The attachment has not been found.");
						return Mono.error(new RicezioneEsitiCartaceoException(
								SEMANTIC_ERROR_CODE,
								errorCodeDescriptionMap().get(SEMANTIC_ERROR_CODE),
								List.of(String.format(UNRECOGNIZED_ERROR_NO_VALUE, ATTACHMENT_URI_LABEL)), List.of(consAuditLogError)));
			})
			.doOnError(throwable -> log.warn(EXCEPTION_IN_PROCESS_FOR, VERIFICA_ATTACHMENTS, requestId, throwable, throwable.getMessage()))
			.doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, requestId, VERIFICA_ATTACHMENTS, result));
	}

	@Override
	public Mono<RicezioneEsitiDto> verificaEsitoDaConsolidatore(
			String xPagopaExtchServiceId, ConsolidatoreIngressPaperProgressStatusEvent progressStatusEvent)
	{
		  log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, VERIFICA_ESITO_DA_CONSOLIDATORE, progressStatusEvent);
		  var requestId = progressStatusEvent.getRequestId();
		  return Mono.just(progressStatusEvent)
				 .flatMap(unused -> gestoreRepositoryCall.getRichiesta(xPagopaExtchServiceId, progressStatusEvent.getRequestId()))
			     .flatMap(requestDto -> verificaErroriSemantici(progressStatusEvent, requestDto, xPagopaExtchServiceId))
			     .flatMap(unused -> verificaAttachments(xPagopaExtchServiceId, requestId, progressStatusEvent.getAttachments()))
				 .flatMap(unused -> Mono.just(new RicezioneEsitiDto(progressStatusEvent,
						  getOperationResultCodeResponse(COMPLETED_OK_CODE,
								  COMPLETED_MESSAGE,
								  null), null))
			     )
			     // *** errore Request Id non trovata
			     .onErrorResume(RestCallException.ResourceNotFoundException.class, throwable -> {
					 var consAuditLogError = new ConsAuditLogError().error(ERR_CONS_REQ_ID_NOT_FOUND.getValue()).requestId(requestId).description("RequestId not found.");
					 return Mono.just(new RicezioneEsitiDto(progressStatusEvent,
							 getOperationResultCodeResponse(REQUEST_ID_ERROR_CODE,
									 errorCodeDescriptionMap().get(REQUEST_ID_ERROR_CODE),
									 List.of(String.format("requestId '%s' unrecognized", requestId))), List.of(consAuditLogError)));
			     })
			     // *** errore semantico
			     .onErrorResume(RicezioneEsitiCartaceoException.class, throwable -> {
		    		 log.debug(EXCEPTION_IN_PROCESS_FOR, VERIFICA_ESITO_DA_CONSOLIDATORE, requestId, throwable, throwable.getMessage());
			    	 return Mono.just(new RicezioneEsitiDto(progressStatusEvent,
								    			 		    getOperationResultCodeResponse(throwable.getResultCode(),
																						   throwable.getResultDescription(),
																						   throwable.getErrorList()), throwable.getAuditLogErrorList()));
			     })
			     // *** errore generico
			     .onErrorResume(RuntimeException.class, throwable -> {
					 log.warn(EXCEPTION_IN_PROCESS_FOR, VERIFICA_ESITO_DA_CONSOLIDATORE, requestId, throwable, throwable.getMessage());
					 return Mono.just(new RicezioneEsitiDto(progressStatusEvent,
							 getOperationResultCodeResponse(INTERNAL_SERVER_ERROR_CODE,
									 errorCodeDescriptionMap().get(INTERNAL_SERVER_ERROR_CODE),
									 List.of(throwable.getMessage())), null));
			     })
				  .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, requestId, VERIFICA_ESITO_DA_CONSOLIDATORE, result));
	}

	@Override
	public Mono<OperationResultCodeResponse> pubblicaEsitoCodaNotificationTracker(
			String xPagopaExtchServiceId,
			ConsolidatoreIngressPaperProgressStatusEvent statusEvent)
	{
		log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PUBBLICA_ESITO_CODA_NOTIFICATION_TRACKER, statusEvent);

		var requestId=statusEvent.getRequestId();

		return statusPullService.paperPullService(requestId, xPagopaExtchServiceId)
			.flatMap(unused -> {

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
				paperProgressStatusDto.setClientRequestTimeStamp(statusEvent.getClientRequestTimeStamp());

	 			return sqsService.send(notificationTrackerSqsName.statoCartaceoName(),
	 								   NotificationTrackerQueueDto.createNotificationTrackerQueueDtoRicezioneEsitiPaper(
	 										   presaInCaricoInfo,
	 										   paperProgressStatusDto.getStatusCode(),
	 										   paperProgressStatusDto));
			})
			.flatMap(unused -> Mono.just(getOperationResultCodeResponse(COMPLETED_OK_CODE, COMPLETED_MESSAGE, null)))
			.onErrorResume(SqsClientException.class, throwable -> {
				 log.warn(EXCEPTION_IN_PROCESS_FOR, PUBBLICA_ESITO_CODA_NOTIFICATION_TRACKER, requestId, throwable, throwable.getMessage());
	    		 return Mono.just(getOperationResultCodeResponse(INTERNAL_SERVER_ERROR_CODE,
	    				 										 errorCodeDescriptionMap().get(INTERNAL_SERVER_ERROR_CODE),
	    				 										 List.of(throwable.getMessage())));
			})
			.onErrorResume(StatusNotFoundException.class, throwable ->
			{
				ConsAuditLogError consAuditLogError = new ConsAuditLogError().requestId(requestId).error(ERR_CONS_BAD_STATUS.getValue()).description(throwable.getMessage());
				return Mono.error(new RicezioneEsitiCartaceoException(SEMANTIC_ERROR_CODE,errorCodeDescriptionMap().get(SEMANTIC_ERROR_CODE),List.of(throwable.getMessage()),List.of(consAuditLogError)));
			})
			.onErrorResume(RuntimeException.class, throwable -> {
				 log.warn(EXCEPTION_IN_PROCESS_FOR, PUBBLICA_ESITO_CODA_NOTIFICATION_TRACKER, requestId, throwable, throwable.getMessage());
	    		 return Mono.just(getOperationResultCodeResponse(INTERNAL_SERVER_ERROR_CODE,
	    				 										 errorCodeDescriptionMap().get(INTERNAL_SERVER_ERROR_CODE),
	    				 										 List.of(throwable.getMessage())));
			})
			.doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, statusEvent.getRequestId(), PUBBLICA_ESITO_CODA_NOTIFICATION_TRACKER, result));
	}

	public Mono<ResponseEntity<OperationResultCodeResponse>> publishOnQueue(List<ConsolidatoreIngressPaperProgressStatusEvent> listEvents, String xPagopaExtchServiceId){
		log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PUBLISH_ON_QUEUE, listEvents);
		return Flux.fromIterable(listEvents)
				// pubblicazione sulla coda
				.flatMap(statusEvent -> pubblicaEsitoCodaNotificationTracker(xPagopaExtchServiceId, statusEvent))
				.collectList()
				// gestione errori oppure response ok
				.flatMap(listSendResponse -> {
					var listSendErrorResponse = listSendResponse.stream().filter(response -> response.getResultCode() != null && !response.getResultCode().equals(COMPLETED_OK_CODE)).toList();
					if (listSendErrorResponse.isEmpty()) {
						return Mono.just(ResponseEntity.ok()
								.body(getOperationResultCodeResponse(COMPLETED_OK_CODE,
										COMPLETED_MESSAGE,
										null)));
					} else {
						var sendErrors = getAllErrors(listSendErrorResponse);
						log.debug(PUBLISH_ON_QUEUE + ": errors found = {}", sendErrors);
						return Mono.just(ResponseEntity.internalServerError()
								.body(getOperationResultCodeResponse(INTERNAL_SERVER_ERROR_CODE,
										errorCodeDescriptionMap().get(INTERNAL_SERVER_ERROR_CODE),
										sendErrors)));
					}
				})
				.onErrorResume(RuntimeException.class, throwable -> {
					log.error("* FATAL * publishOnQueue - {}, {}", throwable, throwable.getMessage());
					return Mono.error(throwable);
				})
				.doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PUBLISH_ON_QUEUE, result));
	}

	 public static List<String> getAllErrors(List<OperationResultCodeResponse> responses) {
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

}
