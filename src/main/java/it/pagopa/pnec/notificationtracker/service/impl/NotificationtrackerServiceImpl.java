package it.pagopa.pnec.notificationtracker.service.impl;

import it.pagopa.pnec.notificationtracker.config.Config;
import it.pagopa.pnec.notificationtracker.model.RequestModel;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import it.pagopa.pnec.notificationtracker.model.ResponseModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
public class NotificationtrackerServiceImpl  {



//	Config prop;
	public ResponseModel getValidateStato() throws Exception{

		String url = "http://localhost:8081/validate/";
		RestTemplate restTemplate = new RestTemplate();
		String pross = "INVIO_PEC";
		String currStato = "BOOKED";
		String clientId = "C050";
		String nextStatus = "VALIDATE";
		RequestModel request = new RequestModel();

		request.setProcessId(pross);
		request.setCurrStatus(currStato);
		request.setClientId(clientId);
		request.setNextStatus(nextStatus);

		URI uri = UriComponentsBuilder.fromUriString(url)
		        .buildAndExpand()
		        .toUri();
		uri = UriComponentsBuilder
		        .fromUri(uri)
		        .pathSegment(pross,currStato)
		        .queryParam("clientId", clientId)
		        .queryParam("nextStatus",nextStatus)

		        .build()
		        .toUri();

		System.out.println("testurl=" +uri);
		
		ResponseModel resul = restTemplate.getForObject(uri, ResponseModel.class);
		if(!resul.isAllowed()) {
//			SqsServiceImpl.sendMessage(null, url, url);
		}
		return resul;
	}
}
