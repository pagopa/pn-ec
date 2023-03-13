package it.pagopa.pn.ec.notificationtracker.service.impl;


import it.pagopa.pn.ec.commons.exception.InvalidNextStatusException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.notificationtracker.service.NotificationTrackerService;
import it.pagopa.pn.ec.notificationtracker.service.PutEvents;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
public class NotificationTrackerServiceImpl implements NotificationTrackerService {

    private final PutEvents putEvents;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final CallMacchinaStati callMachinaStatiImpl;
    private final SqsService sqsService;

    public NotificationTrackerServiceImpl(PutEvents putEvents, GestoreRepositoryCall gestoreRepositoryCall,
                                          CallMacchinaStati callMachinaStatiImpl, SqsService sqsService) {
        this.putEvents = putEvents;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.callMachinaStatiImpl = callMachinaStatiImpl;
        this.sqsService = sqsService;
    }

    @Override
    public Mono<Void> handleRequestStatusChange(NotificationTrackerQueueDto notificationTrackerQueueDto, String ntStatoErratoQueueName) {
        String nextStatus = notificationTrackerQueueDto.getNextStatus();

        return callMachinaStatiImpl.statusValidation(notificationTrackerQueueDto)
                                   .then(callMachinaStatiImpl.statusDecode(notificationTrackerQueueDto))
                                   .zipWhen(macchinaStatiDecodeResponseDto -> {
                                       var logicStatus = macchinaStatiDecodeResponseDto.getLogicStatus();
                                       var paperProgressStatusDto = notificationTrackerQueueDto.getPaperProgressStatusDto();
                                       var digitalProgressStatusDto = notificationTrackerQueueDto.getDigitalProgressStatusDto();

                                       if (digitalProgressStatusDto != null) {
                                           digitalProgressStatusDto.status(nextStatus).statusCode(logicStatus);
                                       } else if (paperProgressStatusDto != null) {
                                           paperProgressStatusDto.statusDescription(nextStatus)
                                                                 .statusCode(macchinaStatiDecodeResponseDto.getLogicStatus());
                                       }

                                       return gestoreRepositoryCall.patchRichiestaEvent(notificationTrackerQueueDto.getRequestIdx(),
                                                                                        new EventsDto().digProgrStatus(
                                                                                                               digitalProgressStatusDto)
                                                                                                       .paperProgrStatus(
                                                                                                               paperProgressStatusDto));
                                   })
                                   .filter(objects -> objects.getT1().getLogicStatus() != null)
                                   .then(putEvents.putEventExternal(notificationTrackerQueueDto))
                                   .then()
                                   .onErrorResume(InvalidNextStatusException.class,
                                                  throwable -> sqsService.send(ntStatoErratoQueueName, notificationTrackerQueueDto).then());
    }
}
