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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;

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
								.sorted(Comparator.comparing(e -> e.getDigProgrStatus().getEventTimestamp()))
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
	public Flux<CourtesyMessageProgressEvent> paperPullService(String requestIdx, String xPagopaExtchCxId) {
		return null;
	}

	@Override
	public Mono<LegalMessageSentDetails> legalPullService(String requestIdx, String xPagopaExtchCxId, String processId) {
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
								.sorted(Comparator.comparing(e -> e.getDigProgrStatus().getEventTimestamp()))
								.skip(eventsList.size() - 1).findFirst().get();
						return Mono.just(eventDTO);
					} else {
						return Mono.empty();
					}
				}).flatMap(eventDTO -> {
					var event = new LegalMessageSentDetails();
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
						Mono.just(new LegalMessageSentDetails().eventCode("").eventDetails("").requestId("")));

	}
}