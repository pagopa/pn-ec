package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMachinaStati;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.rest.v1.dto.CourtesyMessageProgressEvent;
import it.pagopa.pn.ec.rest.v1.dto.DigitalMessageReference;
import it.pagopa.pn.ec.rest.v1.dto.ProgressEventCategory;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class StatusPullServiceImpl implements StatusPullService {

    private final AuthService authService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final CallMachinaStati callMacchinaStati;

    public StatusPullServiceImpl(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall, CallMachinaStati callMacchinaStati) {
        this.authService = authService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.callMacchinaStati = callMacchinaStati;
    }

    @Override
    public Mono<CourtesyMessageProgressEvent> digitalPullService(String requestIdx, String xPagopaExtchCxId, String processId) {
        return authService.clientAuth(xPagopaExtchCxId)
                          .then(gestoreRepositoryCall.getRichiesta(requestIdx))
                          .onErrorResume(RestCallException.ResourceNotFoundException.class,
                                         e -> Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
                          .handle((requestDto, synchronousSink) -> {
                              String requestClientID = requestDto.getxPagopaExtchCxId();
                              if (requestClientID == null || !requestClientID.equals(xPagopaExtchCxId)) {
                                  // TODO: CHANGE EXCEPTION
                                  synchronousSink.error(new RuntimeException(xPagopaExtchCxId));
                              } else {
                                  synchronousSink.next(requestDto);
                              }
                          })
                          .flatMap(object -> {
                              var requestDTO = (RequestDto) object;
                              var eventsList = requestDTO.getRequestMetadata().getEventsList();
                              if (eventsList != null && !eventsList.isEmpty()) {
                                  // TODO: GET EVENT WITH LAST TIMESTAMP
                                  return Mono.just(eventsList.get(0));
                              } else {
                                  return Mono.empty();
                              }
                          })
                          .flatMap(eventDTO -> {
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
                              return callMacchinaStati.statusDecode(processId, digProgrStatus.getStatus(), xPagopaExtchCxId)
                                                      .map(macchinaStatiDecodeResponseDto -> event.status(ProgressEventCategory.valueOf(
                                                                                                          macchinaStatiDecodeResponseDto.getExternalStatus()))
                                                                                                  .eventCode(macchinaStatiDecodeResponseDto.getLogicStatus()));
                          });
    }

    @Override
    public Flux<CourtesyMessageProgressEvent> paperPullService(String requestIdx, String xPagopaExtchCxId) {
        return null;
    }
}
