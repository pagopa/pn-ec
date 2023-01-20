package it.pagopa.pn.template.rest;



import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import it.pagopa.pnec.notificationtracker.model.ResponseModel;
import it.pagopa.pnec.notificationtracker.service.impl.NotificationtrackerServiceImpl;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
public class NotificationtrackerController   {




	@GetMapping(value="/getStatus")
	private ResponseModel getStatus() throws Exception {
		NotificationtrackerServiceImpl service = new NotificationtrackerServiceImpl();
		ResponseModel resul = service.getValidateStato();	
		
		return resul;
		
	}
	

}
	  

