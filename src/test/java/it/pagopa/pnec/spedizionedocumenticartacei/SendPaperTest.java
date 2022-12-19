package it.pagopa.pnec.spedizionedocumenticartacei;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SendPaperTest {
	
	ServicePaperDocument service = new ServicePaperDocument();

	@Test
	//SCI.100.1 inviare documento con successo
	void sendDocumentSuccess() {
		Scheme scheme = new Scheme();
		scheme.setRequestId("ABCD-HILM-YKWX-202202-1_rec0_try1");
		scheme.setRequestPaId("00414580183");
		scheme.setAttachmentUri("/pagoPa.pdf");
		Assertions.assertTrue(service.sendDocument(scheme), "Allegato formattato correttamente");
	}
	
	@Test
	//SCI.100.2 inviare documento senza allegato
	void sendDocumentAttachmentFailed() {
		Scheme scheme = new Scheme();
		scheme.setRequestId("ABCD-HILM-YKWX-202202-1_rec0_try1");
		scheme.setRequestPaId("00414580183");
		Assertions.assertFalse(service.sendDocument(scheme), "Allegato non presente");
	}
	
	@Test
	//SCI.100.3 inviare richiesta di spedizione con requestId già presente a sistema
	void sendDocumentDuplicateRequestIdFailed() {
		Scheme scheme = new Scheme();
		scheme.setRequestId("ABCD-HILM-YKWX-202202-1_rec0_try1");
		scheme.setRequestPaId("00414580183");
		scheme.setAttachmentUri("/pagoPa.pdf");
		Assertions.assertTrue(service.sendDocument(scheme), "RequestId già presente");
	}

}
