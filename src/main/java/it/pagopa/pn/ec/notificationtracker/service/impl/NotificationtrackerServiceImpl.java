package it.pagopa.pn.ec.notificationtracker.service.impl;


import it.pagopa.pn.ec.commons.model.configurationproperties.endpoint.GestoreRepositoryEndpoint;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.notificationtracker.model.NotificationResponseModel;
import it.pagopa.pn.ec.notificationtracker.model.NtStatoError;
import it.pagopa.pn.ec.notificationtracker.model.NotificationRequestModel;
import it.pagopa.pn.ec.notificationtracker.service.PutEventsImpl;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.*;


@Service
@Slf4j
public class NotificationtrackerServiceImpl  {

	private final WebClient ecInternalWebClient;

	private final PutEventsImpl putEventsImpl;
	@Value("${statemachine.url}")
	String statemachineGetClientEndpoint;

	private final GestoreRepositoryCall gestoreRepositoryCall;


	private final SqsService sqsService;
	public NotificationtrackerServiceImpl(WebClient ecInternalWebClient, PutEventsImpl putEventsImpl, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService) {
		this.ecInternalWebClient = ecInternalWebClient;
		this.putEventsImpl = putEventsImpl;
		this.gestoreRepositoryCall = gestoreRepositoryCall;
		this.sqsService = sqsService;

		this.statemachineGetClientEndpoint = statemachineGetClientEndpoint;
	}

	public Mono<Void> getValidateStatoSmS(String process, String status, String xpagopaExtchCxId, String nextStatus) {

		return ecInternalWebClient.get()
				.uri(uriBuilder -> uriBuilder.path(statemachineGetClientEndpoint + "{processId}/{status}" )
						.queryParam("xpagopaExtchCxId",  xpagopaExtchCxId)
						.queryParam("nextStatus" ,nextStatus)
						.build(process,status))
				.retrieve()
				.bodyToMono(NotificationResponseModel.class)
				.flatMap(notificationResponseModel -> {
					if(notificationResponseModel.isAllowed()){
						log.info(">>> publish response {} ", notificationResponseModel);
						EventsDto events  = new EventsDto();
						events.getDigProgrStatus().setStatus(DigitalProgressStatusDto.StatusEnum.valueOf(nextStatus));
						return gestoreRepositoryCall.updateRichiesta(xpagopaExtchCxId, events);
					}if(notificationResponseModel.getMessage() != null){
						return   putEventsImpl.putEventExternal(new NotificationRequestModel(process,xpagopaExtchCxId,nextStatus));
					}
					else {

						return sqsService.send(NT_STATO_SMS_ERRATO_QUEUE_NAME, new NtStatoError(process, status,xpagopaExtchCxId));
					}
				}).then();

	}

	public Mono<Void> getValidateStatoEmail(String process, String status, String xpagopaExtchCxId, String nextStatus) {
		return ecInternalWebClient.get()
				.uri(uriBuilder -> uriBuilder.path(statemachineGetClientEndpoint + "{processId}/{status}" )
						.queryParam("xpagopaExtchCxId",  xpagopaExtchCxId)
						.queryParam("nextStatus" ,nextStatus)
						.build(process,status))
				.retrieve()
				.bodyToMono(NotificationResponseModel.class)
				.flatMap(notificationResponseModel -> {
					if(notificationResponseModel.isAllowed()){
						log.info(">>> publish response {} ", notificationResponseModel);
						EventsDto events  = new EventsDto();
						events.getDigProgrStatus().setStatus(DigitalProgressStatusDto.StatusEnum.valueOf(nextStatus));
					}
					if(notificationResponseModel.getMessage() != null){
						return   putEventsImpl.putEventExternal(new NotificationRequestModel(process,xpagopaExtchCxId,nextStatus));
					}
					else {
						return sqsService.send(NT_STATO_EMAIL_ERRATO_QUEUE_NAME, new NtStatoError(process, status,xpagopaExtchCxId));
					}
				}).then();

	}



	public Mono<Void> getValidateStatoPec(String process, String status, String xpagopaExtchCxId, String nextStatus) {
		return ecInternalWebClient.get()
				.uri(uriBuilder -> uriBuilder.path(statemachineGetClientEndpoint + "{processId}/{status}" )
						.queryParam("xpagopaExtchCxId",  xpagopaExtchCxId)
						.queryParam("nextStatus" ,nextStatus)
						.build(process,status))
				.retrieve()
				.bodyToMono(NotificationResponseModel.class)
				.flatMap(notificationResponseModel -> {
					if(notificationResponseModel.isAllowed()){
						log.info(">>> publish response {} ", notificationResponseModel);
						EventsDto events  = new EventsDto();
						events.getDigProgrStatus().setStatus(DigitalProgressStatusDto.StatusEnum.valueOf(nextStatus));
					}if(notificationResponseModel.getMessage() != null){
						return   putEventsImpl.putEventExternal(new NotificationRequestModel(process,xpagopaExtchCxId,nextStatus));
					}
					else {
						return sqsService.send(NT_STATO_PEC_ERRATO_QUEUE_NAME, new NtStatoError(process, status,xpagopaExtchCxId));
					}
				}).then();

	}

	public Mono<Void> getValidateCartaceStatus(String process, String status, String xpagopaExtchCxId, String nextStatus) {
		return ecInternalWebClient.get()
				.uri(uriBuilder -> uriBuilder.path(statemachineGetClientEndpoint + "{processId}/{status}" )
						.queryParam("xpagopaExtchCxId",  xpagopaExtchCxId)
						.queryParam("nextStatus" ,nextStatus)
						.build(process,status))
				.retrieve()
				.bodyToMono(NotificationResponseModel.class)
				.flatMap(notificationResponseModel -> {
					if(notificationResponseModel.isAllowed()){
						log.info(">>> publish response {} ", notificationResponseModel);
						EventsDto events  = new EventsDto();
						events.getDigProgrStatus().setStatus(DigitalProgressStatusDto.StatusEnum.valueOf(nextStatus));
					}
					if(notificationResponseModel.getMessage() != null){
						return   putEventsImpl.putEventExternal(new NotificationRequestModel(process,xpagopaExtchCxId,nextStatus));
					}
					else {
						return sqsService.send(NT_STATO_CARTACEO_ERRATO_QUEUE_NAME, new NtStatoError(process, status,xpagopaExtchCxId));
					}
				}).then();

	}


}
