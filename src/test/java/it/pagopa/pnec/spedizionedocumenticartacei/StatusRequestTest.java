package it.pagopa.pnec.spedizionedocumenticartacei;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StatusRequestTest {
	
	ServicePaperDocument service = new ServicePaperDocument();
	
	@Test
	//SCRR.100.3 connected
	public void connectionSuccess() {
		HeaderRequest headerParam = new HeaderRequest();
		headerParam.setServiceId("x-pagopa-extch-service-id");
		headerParam.setApiKey("x-api-key");
		headerParam.setRequestId("ABCD-HILM-YKWX-202202-1_rec0_try1");
		Assertions.assertTrue(service.getConnection(headerParam), "Connected");
	}
	
	@Test
	//SCRR.100.3 connection refused - error 500
	public void connectionFailed() {
		HeaderRequest headerParam = new HeaderRequest();
//		headerParam.setServiceId(""); serviceId null
		headerParam.setApiKey("x-api-key");
		headerParam.setRequestId("ABCD-HILM-YKWX-202202-1_rec0_try1");
		Assertions.assertFalse(service.getConnection(headerParam), "Connection Refused");
	}
	
	@Test
	//SCRR.100.1 status code - 200
    public void statusCodeSuccess() {
		HeaderRequest headerParam = new HeaderRequest();
		headerParam.setServiceId("x-pagopa-extch-service-id");
		headerParam.setApiKey("x-api-key");
		headerParam.setRequestId("ABCD-HILM-YKWX-202202-1_rec0_try1");
//		service.getStatusCode(headerParam);
		Assertions.assertTrue(service.getStatusCode(headerParam), "Codice trovato");
    }
	
	@Test
	//SCRR.100.2 status code - error 404
    public void statusCodeFailed() {
		HeaderRequest headerParam = new HeaderRequest();
		headerParam.setServiceId("x-pagopa-extch-service-id");
		headerParam.setApiKey("x-api-key");
//		headerParam.setRequestId("ABCD-HILM-YKWX-202202-1_rec0_try1"); requestId null
//		service.getStatusCode(headerParam);
		Assertions.assertFalse(service.getStatusCode(headerParam), "RequestId non corretto");
    }

}