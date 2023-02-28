package it.pagopa.pn.ec.notificationtracker.service.impl;


import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.notificationtracker.service.NotificationTrackerService;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
public class NotificationTrackerServiceImpl implements NotificationTrackerService {

    private final PutEventsImpl putEventsImpl;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final CallMacchinaStati callMachinaStatiImpl;
    private final SqsService sqsService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;

    public NotificationTrackerServiceImpl(PutEventsImpl putEventsImpl, GestoreRepositoryCall gestoreRepositoryCall,
                                          CallMacchinaStati callMachinaStatiImpl, SqsService sqsService,
                                          NotificationTrackerSqsName notificationTrackerSqsName) {
        this.putEventsImpl = putEventsImpl;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.callMachinaStatiImpl = callMachinaStatiImpl;
        this.sqsService = sqsService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
    }

    @Override
    public Mono<Void> validateSmsStatus(final NotificationTrackerQueueDto notificationTrackerQueueDto) {
        return checkStatusChange(notificationTrackerQueueDto, notificationTrackerSqsName.statoSmsErratoName());

    }

    @Override
    public Mono<Void> validateEmailStatus(final NotificationTrackerQueueDto notificationTrackerQueueDto) {
        return checkStatusChange(notificationTrackerQueueDto, notificationTrackerSqsName.statoEmailErratoName());
    }

    @Override
    public Mono<Void> validatePecStatus(final NotificationTrackerQueueDto notificationTrackerQueueDto) {
        return checkStatusChange(notificationTrackerQueueDto, notificationTrackerSqsName.statoPecErratoName());
    }

    @Override
    public Mono<Void> validateCartaceoStatus(final NotificationTrackerQueueDto notificationTrackerQueueDto) {
        return checkStatusChange(notificationTrackerQueueDto, notificationTrackerSqsName.statoCartaceoErratoName());
    }

    private Mono<Void> checkStatusChange(NotificationTrackerQueueDto notificationTrackerQueueDto, String ntStatoErratoQueueName) {
        String processId = notificationTrackerQueueDto.getProcessId();
        String nextStatus = notificationTrackerQueueDto.getNextStatus();

        return callMachinaStatiImpl.statusValidation(processId,
                                                     notificationTrackerQueueDto.getCurrentStatus(),
                                                     notificationTrackerQueueDto.getXPagopaExtchCxId(),
                                                     nextStatus).flatMap(macchinaStatiValidateStatoResponseDto -> {
            if (macchinaStatiValidateStatoResponseDto.isAllowed()) {
                var events = new EventsDto();

                if (processId.equals("cartaceoProcess")) {
                    // TODO: HANDLE CARTACEO CONDITION

                } else {
                    var digitalProgressStatusDto =
                            new DigitalProgressStatusDto().eventTimestamp(notificationTrackerQueueDto.getEventTimestamp())
                                                          .status(nextStatus)
                                                          // TODO: DEFINE statusCode RETRIEVAL FROM TECHNICAL STATUS
//                                                          .statusCode()
                                                          .eventDetails(notificationTrackerQueueDto.getEventDetails())
                                                          .generatedMessage(notificationTrackerQueueDto.getGeneratedMessageDto());
                    events.setDigProgrStatus(digitalProgressStatusDto);
                }


                return gestoreRepositoryCall.patchRichiestaEvent(notificationTrackerQueueDto.getRequestIdx(), events);
            }
            if (macchinaStatiValidateStatoResponseDto.getNotificationMessage() != null) {
                return putEventsImpl.putEventExternal(notificationTrackerQueueDto);
            } else {
                return sqsService.send(ntStatoErratoQueueName, notificationTrackerQueueDto);
            }
        }).then();
    }
}
