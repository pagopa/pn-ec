package it.pagopa.pnec.notificationTracker.controller;

import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;

import it.pagopa.pnec.notificationTracker.factory.RequestObjectFactory;
import it.pagopa.pnec.notificationTracker.model.RequestModel;
import it.pagopa.pnec.notificationTracker.service.NotificationtrackerService;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import org.junit.jupiter.api.Test;


@ExtendWith(SpringExtension.class)
@WebFluxTest(NotificationtrackerController.class)
 class NotificationtrackerControllerTest {
	
	
	NotificationtrackerController apiController = new NotificationtrackerController();
	
		@Autowired
	    private WebTestClient webClient;

	    @MockBean
	    NotificationtrackerService service;
	    
	    public static final String GET_STATUS_ENDPOINT = "/validate/%s";
	    
	    @SuppressWarnings("unused")
		private WebTestClient.ResponseSpec getStato(BodyInserter<RequestModel, ReactiveHttpOutputMessage> bodyInserter,
										    		String processId, String currStatus,
													String clientId,
													String nextStatus) {
								return this.webClient.put()
								.uri(getStatoValidato(processId,currStatus,clientId,nextStatus))
								.accept(APPLICATION_JSON)
								.contentType(APPLICATION_JSON)
								.body(bodyInserter)
								.exchange();
								}



		private String getStatoValidato(String processId, String currStatus, String clientId, String nextStatus) {
			// TODO Auto-generated method stub
			return String.format(GET_STATUS_ENDPOINT, processId,currStatus,clientId,nextStatus);
		}
		
		
		  @Test
		    void getStatusOkTest() {
			  getStato(BodyInserters.fromValue(RequestObjectFactory.getStatus()), "INVIO_PEC", "BOOKED", "C050", "VALIDATE").expectStatus().isOk();
		    }
	    

}
