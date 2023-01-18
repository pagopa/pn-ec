package it.pagopa.pn.template.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.rest.v1.api.NotificationtrackerControllerApiApi;
import it.pagopa.pnec.notificationTracker.model.RequestModel;
import it.pagopa.pnec.notificationTracker.model.ResponseModel;
import it.pagopa.pnec.notificationTracker.service.NotificationtrackerService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class NotificationtrackerControllerApi  implements NotificationtrackerControllerApiApi{
	
	
	  @Override
	    public Mono<ResponseEntity<Map<String, List<String>>>> getStatusMap(ServerWebExchange exchange) {


	        return Mono.fromSupplier(() ->{
	            log.debug("Start getHttpHeadersMap");
	            Map<String, List<String>> headers = new HashMap<>();
	            exchange.getRequest().getHeaders().forEach((k, v) -> headers.put(k, v));
	            return ResponseEntity.ok(headers);
	        });

	    }
	  
//	  @GetMapping(value="/getStatus")
//	    public String validateStatus() throws Exception{   
//	        return "OK";
//	    }
	  
	  

}
