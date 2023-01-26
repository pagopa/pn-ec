package it.pagopa.pn.ec.notificationtracker.service.impl;


import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.notificationtracker.model.NtStatoError;
import it.pagopa.pn.ec.notificationtracker.model.ResponseModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.*;


@Service
@Slf4j
public class NotificationtrackerServiceImpl  {

	private final WebClient ecInternalWebClient;
	@Value("${statemachine.url}")
	String statemachineGetClientEndpoint;

	private final SqsService sqsService;
	public NotificationtrackerServiceImpl(WebClient ecInternalWebClient, SqsService sqsService) {
		this.ecInternalWebClient = ecInternalWebClient;
		this.sqsService = sqsService;
		this.statemachineGetClientEndpoint = statemachineGetClientEndpoint;
	}

	public Mono<Void> getValidateStatoSmS(String process, String status, String clientId, String nextStatus) throws Exception{

		return ecInternalWebClient.get()
				.uri(uriBuilder -> uriBuilder.path(statemachineGetClientEndpoint + "{processId}/{status}" )
						.queryParam("clientId",  clientId)
						.queryParam("nextStatus" ,nextStatus)
						.build(process,status))
				.retrieve()
				.bodyToMono(ResponseModel.class)
				.publishOn(Schedulers.boundedElastic())
				.flatMap(responseModel -> {
					if(responseModel.isAllowed()){
						System.out.println("tets" + responseModel.toString());

						return putStatoSms(process,nextStatus,clientId);
					}else {

						return sqsService.send(NT_STATO_SMS_ERRATO_QUEUE_NAME, new NtStatoError(process, status,clientId));
					}
				});

	}

	public Mono<Void> getValidateStatoEmail(String process, String status, String clientId, String nextStatus) {
		return ecInternalWebClient.get()
				.uri(uriBuilder -> uriBuilder.path(statemachineGetClientEndpoint + "{processId}/{status}" )
						.queryParam("clientId",  clientId)
						.queryParam("nextStatus" ,nextStatus)
						.build(process,status))
				.retrieve()
				.bodyToMono(ResponseModel.class)
				.flatMap(responseModel -> {
					if(responseModel.isAllowed()){
						System.out.println("tets" + responseModel.toString());

						return putStatoEmail(process,nextStatus,clientId);
					}else {

						return sqsService.send(NT_STATO_EMAIL_ERRATO_QUEUE_NAME, new NtStatoError(process, status,clientId));
					}
				});

	}



	public Mono<Void> getValidateStatoPec(String process, String status, String clientId, String nextStatus) {
		return ecInternalWebClient.get()
				.uri(uriBuilder -> uriBuilder.path(statemachineGetClientEndpoint + "{processId}/{status}" )
						.queryParam("clientId",  clientId)
						.queryParam("nextStatus" ,nextStatus)
						.build(process,status))
				.retrieve()
				.bodyToMono(ResponseModel.class)
				.flatMap(responseModel -> {
					if(responseModel.isAllowed()){
						System.out.println("tets" + responseModel.toString());

						return putStatoPec(process,nextStatus,clientId);
					}else {

						return sqsService.send(NT_STATO_PEC_ERRATO_QUEUE_NAME, new NtStatoError(process, status,clientId));
					}
				});

	}

	public Mono<Void> getValidateCartaceStatus(String process, String status, String clientId, String nextStatus) {
		return ecInternalWebClient.get()
				.uri(uriBuilder -> uriBuilder.path(statemachineGetClientEndpoint + "{processId}/{status}" )
						.queryParam("clientId",  clientId)
						.queryParam("nextStatus" ,nextStatus)
						.build(process,status))
				.retrieve()
				.bodyToMono(ResponseModel.class)
				.flatMap(responseModel -> {
					if(responseModel.isAllowed()){
						System.out.println("tets" + responseModel.toString());

						return putStatoCartaceo(process,nextStatus,clientId);
					}else {

						return sqsService.send(NT_STATO_CARTACEO_ERRATO_QUEUE_NAME, new NtStatoError(process, status,clientId));
					}
				});

	}

	public Mono<Void> putStatoSms(String process, String nextStatus, String clientId) {
		return null;
	}

	public Mono<Void> putStatoEmail(String process, String nextStatus, String clientId) {
		return null;
	}
	public Mono<Void> putStatoPec(String process, String nextStatus, String clientId) {
		return null;
	}
	public Mono<Void> putStatoCartaceo(String process, String nextStatus, String clientId) {
		return null;
	}
}
