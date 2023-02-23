package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.ClientNotAuthorizedException;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic400ErrorException;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic404ErrorException;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMachinaStati;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.rest.v1.dto.CourtesyMessageProgressEvent;
import it.pagopa.pn.ec.rest.v1.dto.DigitalMessageReference;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import it.pagopa.pn.ec.rest.v1.dto.ProgressEventCategory;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

@Service
public class StatusPullServiceImpl implements StatusPullService {

	private final AuthService authService;
	private final GestoreRepositoryCall gestoreRepositoryCall;
	private final CallMachinaStati callMacchinaStati;

	public StatusPullServiceImpl(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall,
			CallMachinaStati callMacchinaStati) {
		this.authService = authService;
		this.gestoreRepositoryCall = gestoreRepositoryCall;
		this.callMacchinaStati = callMacchinaStati;
	}

	@Override
	public Flux<CourtesyMessageProgressEvent> digitalPullService(String requestIdx, String xPagopaExtchCxId) {
		return Flux.from(authService.clientAuth(xPagopaExtchCxId).then(gestoreRepositoryCall.getRichiesta(requestIdx))
				.onErrorResume(RestCallException.ResourceNotFoundException.class,
						e -> Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
				.handle((requestDTO, sink) -> {
//					// Controlla se il clientID della richiesta e quello del chiamante coincidono.
//					// Se non coincidono, lancia un'eccezione FORBIDDEN 403.
					String requestClientID = requestDTO.getxPagopaExtchCxId();
					if (requestClientID == null || !requestClientID.equals(xPagopaExtchCxId))
						sink.error(new ClientNotAuthorizedException(xPagopaExtchCxId));
					else
						sink.next(requestDTO);
				}).map(dto -> {

					var requestDTO = (RequestDto) dto;

					String processId = requestDTO.getRequestMetadata().getDigitalRequestMetadata().getChannel().name();
					String currStatus = "";

					var eventsListDTO = requestDTO.getRequestMetadata().getEventsList();
					var event = new CourtesyMessageProgressEvent();

					if (eventsListDTO != null && !eventsListDTO.isEmpty()) {

						EventsDto eventDTO = eventsListDTO.get(eventsListDTO.size() - 1);

						var digProgrStatus = eventDTO.getDigProgrStatus();

						event.setRequestId(requestIdx);
						event.setEventDetails(digProgrStatus.getEventDetails());
						event.setEventTimestamp(digProgrStatus.getEventTimestamp());

						currStatus = digProgrStatus.getStatus();

//						var decodedStatusDto = callMacchinaStati.statusDecode(processId, currStatus.toLowerCase(),
//								xPagopaExtchCxId);
//
//						event.setStatus(ProgressEventCategory.fromValue(decodedStatusDto.block().getExternalState()));
						event.setEventCode(digProgrStatus.getStatusCode());

						var generatedMessageDTO = digProgrStatus.getGeneratedMessage();

						if (generatedMessageDTO != null) {
							var digitalMessageReference = new DigitalMessageReference();

							digitalMessageReference.setId(generatedMessageDTO.getId());
							digitalMessageReference.setLocation(generatedMessageDTO.getLocation());
							digitalMessageReference.setSystem(generatedMessageDTO.getSystem());

							event.setGeneratedMessage(digitalMessageReference);
						}
					}
					return Tuples.of(processId, currStatus, event);
				}).flatMap(info ->

				{
					CourtesyMessageProgressEvent event = info.getT3();
					//System.out.println("TUPLAAAAA---->" + info);

					return callMacchinaStati.statusDecode(info.getT1(), info.getT2().toLowerCase(), xPagopaExtchCxId)
							.flatMap(decodedStatus -> {
								event.setStatus(ProgressEventCategory.fromValue(decodedStatus.getExternalState()));
								return Mono.just(event);
							});

				}));
	}

	@Override
	public Flux<CourtesyMessageProgressEvent> paperPullService(String requestIdx, String xPagopaExtchCxId) {
		return null;
	}
}