package it.pagopa.pn.ec.commons.service.impl;

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
        return authService.clientAuth(xPagopaExtchCxId)
                          .then(gestoreRepositoryCall.getRichiesta(requestIdx))
                          .onErrorResume(RestCallException.ResourceNotFoundException.class,
                                         e -> Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
                          .map(requestDTO -> {
                              var eventsListDTO = requestDTO.getRequestMetadata().getEventsList();
                              List<CourtesyMessageProgressEvent> eventsList = new ArrayList<>();

                              if (eventsListDTO != null && !eventsListDTO.isEmpty()) {
                                  for (EventsDto eventDTO : eventsListDTO) {

                                      var event = new CourtesyMessageProgressEvent();
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

                                      eventsList.add(event);
                                  }
                              }
                              return eventsList;
                          })
                          .flatMapIterable(courtesyMessageProgressEvents -> courtesyMessageProgressEvents);
    }

    @Override
    public Flux<CourtesyMessageProgressEvent> paperPullService(String requestIdx, String xPagopaExtchCxId) {
        return null;
    }
}