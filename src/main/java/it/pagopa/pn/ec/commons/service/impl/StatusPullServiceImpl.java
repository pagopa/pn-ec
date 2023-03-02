package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.ClientNotAuthorizedException;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Service
public class StatusPullServiceImpl implements StatusPullService {

	private final AuthService authService;
	private final GestoreRepositoryCall gestoreRepositoryCall;
	private final CallMacchinaStati callMacchinaStati;

	public StatusPullServiceImpl(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall,
			CallMacchinaStati callMacchinaStati) {
		this.authService = authService;
		this.gestoreRepositoryCall = gestoreRepositoryCall;
		this.callMacchinaStati = callMacchinaStati;
	}

	@Override
	public Mono<CourtesyMessageProgressEvent> digitalPullService(String requestIdx, String xPagopaExtchCxId,
			String processId) {
		return authService.clientAuth(xPagopaExtchCxId).then(gestoreRepositoryCall.getRichiesta(requestIdx))
				.onErrorResume(RestCallException.ResourceNotFoundException.class,
						e -> Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
				.handle((requestDto, synchronousSink) -> {
					String requestClientID = requestDto.getxPagopaExtchCxId();
					if (requestClientID == null || !requestClientID.equals(xPagopaExtchCxId)) {
						synchronousSink.error(new ClientNotAuthorizedException(xPagopaExtchCxId));
					} else {
						synchronousSink.next(requestDto);
					}
				}).flatMap(object -> {
					var requestDTO = (RequestDto) object;
					var eventsList = requestDTO.getRequestMetadata().getEventsList();
					if (eventsList != null && !eventsList.isEmpty()) {
						var eventDTO = eventsList.stream()
								.sorted((e1, e2) -> e1.getDigProgrStatus().getEventTimestamp()
										.compareTo(e2.getDigProgrStatus().getEventTimestamp()))
								.skip(eventsList.size() - 1).findFirst().get();
						return Mono.just(eventDTO);
					} else {
						return Mono.empty();
					}
				}).flatMap(eventDTO -> {
					var event = new CourtesyMessageProgressEvent();
					var digProgrStatus = eventDTO.getDigProgrStatus();

					event.setRequestId(requestIdx);
					event.setEventDetails(digProgrStatus.getEventDetails());
					event.setEventTimestamp(digProgrStatus.getEventTimestamp());

					var generatedMessageDTO = digProgrStatus.getGeneratedMessage();
					if (generatedMessageDTO != null) {
						var digitalMessageReference = new DigitalMessageReference();

						digitalMessageReference.setId(generatedMessageDTO.getId());
						digitalMessageReference.setLocation(generatedMessageDTO.getLocation());
						digitalMessageReference.setSystem(generatedMessageDTO.getSystem());

						event.setGeneratedMessage(digitalMessageReference);
					}
					return callMacchinaStati
							.statusDecode(processId, digProgrStatus.getStatus().toLowerCase(), xPagopaExtchCxId)
							.map(macchinaStatiDecodeResponseDto -> {

								event.setStatus(ProgressEventCategory
										.valueOf(macchinaStatiDecodeResponseDto.getExternalStatus()));
								event.setEventCode(macchinaStatiDecodeResponseDto.getLogicStatus());
								return event;
							});
				}).switchIfEmpty(
						Mono.just(new CourtesyMessageProgressEvent().eventCode("").eventDetails("").requestId("")));

	}

	@Override
	public Mono<PaperProgressStatusEvent> paperPullService(String requestIdx, String xPagopaExtchCxId) {
		return authService.clientAuth(xPagopaExtchCxId).then(gestoreRepositoryCall.getRichiesta(requestIdx))
				.onErrorResume(RestCallException.ResourceNotFoundException.class,
						e -> Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
				.handle((requestDto, synchronousSink) -> {
					// Controlla se il client è autorizzato.
					String requestClientID = requestDto.getxPagopaExtchCxId();
					if (requestClientID == null || !requestClientID.equals(xPagopaExtchCxId)) {
						synchronousSink.error(new ClientNotAuthorizedException(xPagopaExtchCxId));
					} else {
						synchronousSink.next(requestDto);
					}
				}).flatMap(object -> {
					var requestDTO = (RequestDto) object;
					var event = new PaperProgressStatusEvent();
					var eventsList = requestDTO.getRequestMetadata().getEventsList();

					// Se l'ultimo evento della lista esiste, viene costruito il suo DTO. Altrimenti
					// viene restituito un Mono.empty().
					if (eventsList != null && !eventsList.isEmpty()) {
						var eventDTO = eventsList.stream()
								.sorted((e1, e2) -> e1.getPaperProgrStatus().getStatusDateTime()
										.compareTo(e2.getPaperProgrStatus().getStatusDateTime()))
								.skip(eventsList.size() - 1).findFirst().get();

						var paperProgrStatus = eventDTO.getPaperProgrStatus();

						event.setRequestId(requestIdx);

						event.setStatusDateTime(paperProgrStatus.getStatusDateTime());
						event.setDeliveryFailureCause(paperProgrStatus.getDeliveryFailureCause());
						event.setRegisteredLetterCode(paperProgrStatus.getRegisteredLetterCode());

						var discoveredAddress = new DiscoveredAddress();
						var discoveredAddressDTO = paperProgrStatus.getDiscoveredAddress();

						if (discoveredAddressDTO != null) {
							discoveredAddress.setAddress(discoveredAddressDTO.getAddress());
							discoveredAddress.setAddressRow2(discoveredAddressDTO.getAddressRow2());
							discoveredAddress.setCap(discoveredAddressDTO.getCap());
							discoveredAddress.setCity(discoveredAddressDTO.getCity());
							discoveredAddress.setCity2(discoveredAddressDTO.getCity2());
							discoveredAddress.setCountry(discoveredAddressDTO.getCountry());
							discoveredAddress.setName(discoveredAddressDTO.getName());
							discoveredAddress.setNameRow2(discoveredAddressDTO.getNameRow2());
							discoveredAddress.setPr(discoveredAddressDTO.getPr());
						}
						event.setDiscoveredAddress(discoveredAddress);

						// Settiamo all'evento lo status NON ANCORA decodificato. La decodifica avverrà
						// successivamente.
						event.setStatusDescription(paperProgrStatus.getStatusDescription());

						var attachmentsListDTO = paperProgrStatus.getAttachments();
						var attachmentList = new ArrayList<AttachmentDetails>();

						if (attachmentsListDTO != null) {
							for (AttachmentsProgressEventDto attachmentDTO : attachmentsListDTO) {

								var attachment = new AttachmentDetails();

								attachment.setDate(attachmentDTO.getDate());
								attachment.setDocumentType(attachmentDTO.getDocumentType());
								attachment.setId(attachmentDTO.getId());
								attachment.setUrl(attachmentDTO.getUri());

								attachmentList.add(attachment);
							}
						}
						event.setAttachments(attachmentList);

						event.setClientRequestTimeStamp(requestDTO.getClientRequestTimeStamp());
						event.setIun(requestDTO.getRequestMetadata().getPaperRequestMetadata().getIun());
						event.setProductType(
								requestDTO.getRequestMetadata().getPaperRequestMetadata().getProductType());

						return Mono.just(event);
					} else {
						return Mono.empty();
					}
				}).flatMap(event -> {

					// Decodifica dello stato della richiesta.
					return callMacchinaStati.statusDecode("PAPER", event.getStatusDescription(), xPagopaExtchCxId)
							.map(macchinaStatiDecodeResponseDto -> {

								event.setStatusDescription(macchinaStatiDecodeResponseDto.getExternalStatus());
								event.setStatusCode(macchinaStatiDecodeResponseDto.getLogicStatus());
								return event;
							});
				})

				.switchIfEmpty(Mono.just(new PaperProgressStatusEvent().requestId(requestIdx).statusDescription("")
						.deliveryFailureCause("").productType("").statusCode("").statusDescription("").iun("")
						.registeredLetterCode("")));

	}
}
