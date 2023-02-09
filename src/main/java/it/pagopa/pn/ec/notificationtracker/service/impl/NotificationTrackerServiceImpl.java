package it.pagopa.pn.ec.notificationtracker.service.impl;


import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.notificationtracker.service.PutEventsImpl;
import it.pagopa.pn.ec.notificationtracker.service.callmachinestati.CallMachinaStati;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.DigitalRequestStatus;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
@Slf4j
public class NotificationTrackerServiceImpl {

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

    public Mono<Void> getValidateStatoSmS(final NotificationTrackerQueueDto nott) {

        return getVoidMono(nott, notificationTrackerSqsName.statoSmsErratoName());

    }

    public Mono<Void> getValidateStatoEmail(final NotificationTrackerQueueDto nott) {
        return getVoidMono(nott, notificationTrackerSqsName.statoEmailErratoName());

    }


    public Mono<Void> getValidateStatoPec(final NotificationTrackerQueueDto nott) {
        return getVoidMono(nott, notificationTrackerSqsName.statoPecErratoName());

    }

    public Mono<Void> getValidateCartaceStatus(final NotificationTrackerQueueDto nott) {
        return getVoidMono(nott, notificationTrackerSqsName.statoCartaceoErratoName());

    }

    @NotNull
    private Mono<Void> getVoidMono(NotificationTrackerQueueDto nott, String ntStatoCartaceoErratoQueueName) {
        return callMachinaStatiImpl.getStato(nott.getProcessId().toString(),
                                             nott.getCurrentStatus(),
                                             nott.getXPagopaExtchCxId(),
                                             nott.getNextStatus()).flatMap(notificationResponseModel -> {
            if (notificationResponseModel.isAllowed()) {
                log.info(">>> publish response {} ", notificationResponseModel);
						/*
							DA concordare mappa per i stati
						 */
                EventsDto events = new EventsDto();
                DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto();
                digitalProgressStatusDto.setStatus(DigitalRequestStatus.valueOf(nott.getNextStatus()));
                events.setDigProgrStatus(digitalProgressStatusDto);
                return gestoreRepositoryCall.updateRichiesta(nott.getRequestIdx(), events);
            }
            if (notificationResponseModel.getNottifMessage() != null) {
                return putEventsImpl.putEventExternal(new NotificationTrackerQueueDto(nott.getRequestIdx(),
                                                                                      nott.getXPagopaExtchCxId(),
                                                                                      nott.getProcessId(),
                                                                                      nott.getCurrentStatus(),
                                                                                      nott.getNextStatus()));
            } else {
                return sqsService.send(ntStatoCartaceoErratoQueueName,
                                       new NotificationTrackerQueueDto(nott.getRequestIdx(),
                                                                       nott.getXPagopaExtchCxId(),
                                                                       nott.getProcessId(),
                                                                       nott.getCurrentStatus(),
                                                                       nott.getNextStatus()));

            }
        }).then();
    }
}
