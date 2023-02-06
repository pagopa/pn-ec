package it.pagopa.pn.ec.notificationtracker.service.impl;


import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.notificationtracker.service.callmachinestati.CallMachinaStati;
import it.pagopa.pn.ec.notificationtracker.service.PutEventsImpl;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.DigitalRequestStatus;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.*;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto.*;


@Service
@Slf4j
public class NotificationtrackerServiceImpl  {

	private final PutEventsImpl putEventsImpl;

	private final GestoreRepositoryCall gestoreRepositoryCall;

	private final CallMachinaStati callMachinaStatiImpl;

	private final SqsService sqsService;
	public NotificationtrackerServiceImpl(PutEventsImpl putEventsImpl, GestoreRepositoryCall gestoreRepositoryCall, CallMachinaStati callMachinaStatiImpl, SqsService sqsService) {
		this.putEventsImpl = putEventsImpl;
		this.gestoreRepositoryCall = gestoreRepositoryCall;
		this.callMachinaStatiImpl = callMachinaStatiImpl;
		this.sqsService = sqsService;

	}

	public static final String PROGRES = "PROGRESS";

	public Mono<Void> getValidateStatoSmS( final NotificationTrackerQueueDto nott) {

		return callMachinaStatiImpl.getStato(nott.getProcessId().toString(), nott.getCurrentStatus(),nott.getXPagopaExtchCxId(),nott.getNextStatus())
				.flatMap(notificationResponseModel -> {
					if(notificationResponseModel.isAllowed()){
						log.info(">>> publish response {} ", notificationResponseModel);
						/*
							DA concordare mappa per i stati
						 */
						EventsDto events  = new EventsDto();
						DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto();
						digitalProgressStatusDto.setStatus(DigitalRequestStatus.valueOf(nott.getNextStatus()));
						events.setDigProgrStatus(digitalProgressStatusDto);
						return gestoreRepositoryCall.updateRichiesta(nott.getRequestIdx(), events);
					}if(notificationResponseModel.getNottifMessage() != null){
						return   putEventsImpl.putEventExternal(new NotificationTrackerQueueDto(nott.getRequestIdx(),nott.getXPagopaExtchCxId(),nott.getProcessId(),nott.getCurrentStatus(),nott.getNextStatus()));

					}
					else {
						return sqsService.send(NT_STATO_SMS_ERRATO_QUEUE_NAME, new NotificationTrackerQueueDto(nott.getRequestIdx(),nott.getXPagopaExtchCxId(),nott.getProcessId(),nott.getCurrentStatus(),nott.getNextStatus()));

					}
				}).then();

	}


	public Mono<Void> getValidateStatoEmail(final NotificationTrackerQueueDto nott) {
		return callMachinaStatiImpl.getStato(nott.getProcessId().toString(), nott.getCurrentStatus(),nott.getXPagopaExtchCxId(),nott.getNextStatus())
				.flatMap(notificationResponseModel -> {
					if(notificationResponseModel.isAllowed()){
						log.info(">>> publish response {} ", notificationResponseModel);
						/*
							DA concordare mappa per i stati
						 */
						EventsDto events  = new EventsDto();
						DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto();
						digitalProgressStatusDto.setStatus(DigitalRequestStatus.valueOf(nott.getNextStatus()));
						events.setDigProgrStatus(digitalProgressStatusDto);
						return gestoreRepositoryCall.updateRichiesta(nott.getRequestIdx(), events);
					}if(notificationResponseModel.getNottifMessage() != null){
						return   putEventsImpl.putEventExternal(new NotificationTrackerQueueDto(nott.getRequestIdx(),nott.getXPagopaExtchCxId(),nott.getProcessId(),nott.getCurrentStatus(),nott.getNextStatus()));

					}
					else {
						return sqsService.send(NT_STATO_SMS_ERRATO_QUEUE_NAME, new NotificationTrackerQueueDto(nott.getRequestIdx(),nott.getXPagopaExtchCxId(),nott.getProcessId(),nott.getCurrentStatus(),nott.getNextStatus()));

					}
				}).then();

	}



	public Mono<Void> getValidateStatoPec(final NotificationTrackerQueueDto nott) {
		return callMachinaStatiImpl.getStato(nott.getProcessId().toString(), nott.getCurrentStatus(),nott.getXPagopaExtchCxId(),nott.getNextStatus())
				.flatMap(notificationResponseModel -> {
					if(notificationResponseModel.isAllowed()){
						log.info(">>> publish response {} ", notificationResponseModel);
						/*
							DA concordare mappa per i stati
						 */
						EventsDto events  = new EventsDto();
						DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto();
						digitalProgressStatusDto.setStatus(DigitalRequestStatus.valueOf(nott.getNextStatus()));
						events.setDigProgrStatus(digitalProgressStatusDto);
						return gestoreRepositoryCall.updateRichiesta(nott.getRequestIdx(), events);
					}if(notificationResponseModel.getNottifMessage() != null){
						return   putEventsImpl.putEventExternal(new NotificationTrackerQueueDto(nott.getRequestIdx(),nott.getXPagopaExtchCxId(),nott.getProcessId(),nott.getCurrentStatus(),nott.getNextStatus()));

					}
					else {
						return sqsService.send(NT_STATO_SMS_ERRATO_QUEUE_NAME, new NotificationTrackerQueueDto(nott.getRequestIdx(),nott.getXPagopaExtchCxId(),nott.getProcessId(),nott.getCurrentStatus(),nott.getNextStatus()));

					}
				}).then();

	}

	public Mono<Void> getValidateCartaceStatus(final NotificationTrackerQueueDto nott) {
		return callMachinaStatiImpl.getStato(nott.getProcessId().toString(), nott.getCurrentStatus(),nott.getXPagopaExtchCxId(),nott.getNextStatus())
				.flatMap(notificationResponseModel -> {
					if(notificationResponseModel.isAllowed()){
						log.info(">>> publish response {} ", notificationResponseModel);
						/*
							DA concordare mappa per i stati
						 */
						EventsDto events  = new EventsDto();
						DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto();
						digitalProgressStatusDto.setStatus(DigitalRequestStatus.valueOf(nott.getNextStatus()));
						events.setDigProgrStatus(digitalProgressStatusDto);
						return gestoreRepositoryCall.updateRichiesta(nott.getRequestIdx(), events);
					}if(notificationResponseModel.getNottifMessage() != null){
						return   putEventsImpl.putEventExternal(new NotificationTrackerQueueDto(nott.getRequestIdx(),nott.getXPagopaExtchCxId(),nott.getProcessId(),nott.getCurrentStatus(),nott.getNextStatus()));

					}
					else {
						return sqsService.send(NT_STATO_SMS_ERRATO_QUEUE_NAME, new NotificationTrackerQueueDto(nott.getRequestIdx(),nott.getXPagopaExtchCxId(),nott.getProcessId(),nott.getCurrentStatus(),nott.getNextStatus()));

					}
				}).then();

	}


}
