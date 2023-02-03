package it.pagopa.pn.ec.spedizionedocumenticartacei;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
@Slf4j
class CommonStatusRequestTest {
	
	ServicePaperDocument service = new ServicePaperDocument();
	
	private static final String SERVICE_ID = "CONSOLIDATORE SERVER";
	private static final String API_KEY = "FCRISCIOTTI";
	private static final String REQUEST_ID = "ABCD-HILM-YKWX-202202-1_rec0_try1";
	
	@Test
	//SCRR.100.3 connection refused - error 500
	void connectionFailed() {
		HeaderRequest headerParam = new HeaderRequest();
		headerParam.setXApiKey(API_KEY);
		headerParam.setRequestId(REQUEST_ID);
		
		log.info(String.valueOf(headerParam));
		Assertions.assertFalse(service.getConnection(headerParam), "Connection Refused");
	}
	
	@Test
	//SCRR.100.1 status code - 200
    void statusCodeSuccess() {
		HeaderRequest headerParam = new HeaderRequest();
		headerParam.setXPagopaExtchServiceId(SERVICE_ID);
		headerParam.setXApiKey(API_KEY);
		headerParam.setRequestId(REQUEST_ID);
		
		log.info(String.valueOf(headerParam));
		Assertions.assertNotNull(service.getStatusCode(headerParam), "Richiesta trovata");
    }
	
	@Test
	//SCRR.100.2 status code - error 404
    void requestNotSent() {
		HeaderRequest headerParam = new HeaderRequest();
		headerParam.setXPagopaExtchServiceId(SERVICE_ID);
		headerParam.setXApiKey(API_KEY);
		headerParam.setRequestId("");
		
		log.info(String.valueOf(headerParam));
		Assertions.assertNotNull(service.getStatusCode(headerParam), "RequestId non corretto");
    }
	
	@Test
	//authentication failed
    void authenticationFailed() {
		HeaderRequest headerParam = new HeaderRequest();
		headerParam.setXPagopaExtchServiceId(SERVICE_ID);
		headerParam.setXApiKey("");
		headerParam.setRequestId(REQUEST_ID);
		
		log.info(String.valueOf(headerParam));
		Assertions.assertNotNull(service.getStatusCode(headerParam), "Required field is not provided");
    }

}