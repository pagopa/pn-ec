package it.pagopa.pnec.notificationTracker.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pnec.notificationTracker.model.RequestModel;
import it.pagopa.pnec.notificationTracker.service.NotificationtrackerService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
@Slf4j
@RestController
public class NotificationtrackerController {
	
	
	 NotificationtrackerService service ;
	
	
	public Mono<ResponseEntity<Void>> getStato(String processId, String currStatus,
																String clientId,
																String nextStatus) {
		service.getStato(processId, currStatus, clientId, nextStatus);
        // TODO:sendCourtesyShortMessage -> Change HttpStatus.NOT_IMPLEMENTED
        return null;
    }
	
	
	public Mono<ResponseEntity<Void>> putNewStato(String processId, String currStatus,
						String clientId,
						String nextStatus) {
			service.putNewStato(processId, currStatus, clientId, nextStatus);
			// TODO:sendCourtesyShortMessage -> Change HttpStatus.NOT_IMPLEMENTED
			return null;
}
	
	
	  
}
