package it.pagopa.pn.template.notificationtracker.service.impl;


import it.pagopa.pn.template.notificationtracker.model.RequestModel;
import it.pagopa.pn.template.notificationtracker.model.ResponseModel;
import it.pagopa.pn.template.notificationtracker.service.SqsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;

import static it.pagopa.pn.template.notificationtracker.constant.QueueNameConstant.NT_STATO_SMS_QUEUE_NAME;


@Service
@Slf4j
public class NotificationtrackerServiceImpl  {

	private final SqsService sqsService;

	@Value("${statemachine.url}")
	String stateMachiUrl;

	public NotificationtrackerServiceImpl(SqsService sqsService) {
		this.sqsService = sqsService;
	}

	public ResponseModel getValidateStato(String process, String status, String clientId, String nextStatus) throws Exception{

		String url = "http://localhost:8080/validate/";
		RestTemplate restTemplate = new RestTemplate();
		URI uri = UriComponentsBuilder.fromUriString(url)
		        .buildAndExpand()
		        .toUri();
		uri = UriComponentsBuilder
		        .fromUri(uri)
		        .pathSegment(process,status)
		        .queryParam("clientId", clientId)
		        .queryParam("nextStatus",nextStatus)
				.build()
		        .toUri();

		System.out.println("uri=" +uri );
		
		ResponseModel resul = restTemplate.getForObject(uri, ResponseModel.class);
				System.out.println("resul =" + resul.isAllowed() );

		String mess ="processId: INVIO_PEC,clientId: C050,currStatus: VALIDATE,nextStatus: COMPOSED}";
		if(!resul.isAllowed()) {
			sqsService.send(NT_STATO_SMS_QUEUE_NAME,mess);
		}
		return resul;
	}
}
