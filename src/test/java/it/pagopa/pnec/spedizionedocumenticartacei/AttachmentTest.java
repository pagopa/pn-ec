package it.pagopa.pnec.spedizionedocumenticartacei;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AttachmentTest {
	
	ServicePaperDocument service = new ServicePaperDocument();
	
	private static final String SERVICE_ID = "CONSOLIDATORE SERVER";
	private static final String API_KEY = "FCRISCIOTTI";
	private static final String REQUEST_ID = "ABCD-HILM-YKWX-202202-1_rec0_try1";
	
	@Test
//	SCDA.100.1 download allegato - codice 200
	void downloadAttachmentSuccess() {
		HeaderRequest hr = new HeaderRequest();
		hr.setXPagopaExtchServiceId(SERVICE_ID);
		hr.setXApiKey(API_KEY);
		hr.setRequestId(REQUEST_ID);
		
		System.out.println(hr.toString());
		Assertions.assertNotNull(service.getAttachment(hr), "download effettuato con successo");
	}
	
//	@Test
////	SCDA.100.2 download allegato - codice 404 - TODO come gestire url malformed?
//	void downloadAttachmentFailed() {
//		HeaderRequest hr = new HeaderRequest();
//		hr.setXPagopaExtchServiceId(SERVICE_ID);
//		hr.setXApiKey(API_KEY);
////		hr.setRequestId(REQUEST_ID);
//		
//		System.out.println(hr.toString());
//		Assertions.assertNull(service.getAttachment(hr), "download effettuato con successo");
//	}

}
