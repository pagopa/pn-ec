package it.pagopa.pn.ec.notificationtracker.service.impl;


import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.constant.ProcessId;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMachinaStati;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.notificationtracker.service.NotificationTrackerService;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.DigitalRequestStatus;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.ProcessId.*;


@Service
public class NotificationTrackerServiceImpl implements NotificationTrackerService {

    private final PutEventsImpl putEventsImpl;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final CallMachinaStati callMachinaStatiImpl;
    private final SqsService sqsService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;

    public NotificationTrackerServiceImpl(PutEventsImpl putEventsImpl, GestoreRepositoryCall gestoreRepositoryCall,
                                          CallMachinaStati callMachinaStatiImpl, SqsService sqsService,
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
        ProcessId processId = notificationTrackerQueueDto.getProcessId();
        String nextStatus = notificationTrackerQueueDto.getNextStatus();

        return callMachinaStatiImpl.statusValidation(processId.name(),
                                                     notificationTrackerQueueDto.getCurrentStatus(),
                                                     notificationTrackerQueueDto.getXPagopaExtchCxId(),
                                                     nextStatus).flatMap(macchinaStatiValidateStatoResponseDto -> {
            if (macchinaStatiValidateStatoResponseDto.isAllowed()) {
                var events = new EventsDto();

                if (processId == INVIO_SMS || processId == INVIO_MAIL || processId == INVIO_PEC) {
                    var digitalProgressStatusDto =
                            new DigitalProgressStatusDto().eventTimestamp(notificationTrackerQueueDto.getEventTimestamp())
                                                          .status(DigitalRequestStatus.valueOf(nextStatus))
                                                          // TODO: DEFINE statusCode RETRIEVAL FROM TECHNICAL STATUS
//                                                          .statusCode()
                                                          // TODO: DEFINE eventDetails IN STATUS UPDATE
                                                          .eventDetails("To be defined")
                                                          .generatedMessage(notificationTrackerQueueDto.getGeneratedMessageDto());
                    events.setDigProgrStatus(digitalProgressStatusDto);
                }

                // TODO: HANDLE CARTACEO CONDITION

                return gestoreRepositoryCall.updateRichiesta(notificationTrackerQueueDto.getRequestIdx(), events);
            }
            if (macchinaStatiValidateStatoResponseDto.getNotificationMessage() != null) {
                return putEventsImpl.putEventExternal(notificationTrackerQueueDto);
            } else {
                return sqsService.send(ntStatoErratoQueueName, notificationTrackerQueueDto);
            }
        }).then();
    }
}
