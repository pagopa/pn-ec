package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.ClientNotFoundException;
import it.pagopa.pn.ec.commons.exception.ClientNotAuthorizedException;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.rest.v1.dto.CourtesyMessageProgressEvent;
import it.pagopa.pn.ec.rest.v1.dto.DigitalMessageReference;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatusPullServiceImpl implements StatusPullService {

	private final AuthService authService;
	private final GestoreRepositoryCall gestoreRepositoryCall;

	public StatusPullServiceImpl(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall) {
		this.authService = authService;
		this.gestoreRepositoryCall = gestoreRepositoryCall;
	}

	@Override
	public Flux<CourtesyMessageProgressEvent> digitalPullService(String requestIdx, String xPagopaExtchCxId) {
		return Flux.from(authService.clientAuth(xPagopaExtchCxId).then(gestoreRepositoryCall.getRichiesta(requestIdx))
				.onErrorResume(RestCallException.ResourceNotFoundException.class,
						e -> Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
				.map(requestDTO -> {

					// Controlla se il clientID della richiesta e quello del chiamante coincidono.
					// Se non coincidono, lancia un'eccezione FORBIDDEN 403.
					String requestClientID = requestDTO.getxPagopaExtchCxId();

					// TODO In futuro le richieste su DB avranno l'attributo xPagopaExtchCxId
					// inizializzato, il controllo sulla stringa null è solamente temporaneo
					if (requestClientID == null || !requestClientID.equals(xPagopaExtchCxId))
						throw new ClientNotAuthorizedException(xPagopaExtchCxId);

					var eventsListDTO = requestDTO.getRequestMetadata().getEventsList();
					var event = new CourtesyMessageProgressEvent();

					if (eventsListDTO != null && !eventsListDTO.isEmpty()) {

						EventsDto eventDTO = eventsListDTO.get(eventsListDTO.size() - 1);

						var digProgrStatus = eventDTO.getDigProgrStatus();

						event.setRequestId(requestIdx);
						event.setEventDetails(digProgrStatus.getEventDetails());
						event.setEventTimestamp(digProgrStatus.getEventTimestamp());

						// TODO: MAP INTERNAL STATUS CODE TO EXTERNAL STATUS
						event.setStatus(null);
						event.setEventCode(null);

						var generatedMessageDTO = digProgrStatus.getGeneratedMessage();

						if (generatedMessageDTO != null) {
							var digitalMessageReference = new DigitalMessageReference();

							digitalMessageReference.setId(generatedMessageDTO.getId());
							digitalMessageReference.setLocation(generatedMessageDTO.getLocation());
							digitalMessageReference.setSystem(generatedMessageDTO.getSystem());

							event.setGeneratedMessage(digitalMessageReference);
						}
					}
					return event;
				}));
	}

	@Override
	public Flux<CourtesyMessageProgressEvent> paperPullService(String requestIdx, String xPagopaExtchCxId) {
		return null;
	}
}