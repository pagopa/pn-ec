package it.pagopa.pnec.spedizionedocumenticartacei;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AttachmentTest {
	
	ServicePaperDocument service = new ServicePaperDocument();

	@Test
//	SCDA.100.1 download allegato - codice 200
	void downloadAttachmentSuccess() {
		Event event = new Event();
		event.setRequestId("ABCD-HILM-YKWX-202202-1_rec0_try1");
		event.setAttachmentUrl("/pagoPa.pdf");
		Assertions.assertTrue(service.getAttachment(event), "Url valida");
		
	}
	
	@Test
//	SCDA.100.2 download allegato - codice 404
	void downloadAttachmentFailed() {
		Event event = new Event();
		event.setRequestId("ABCD-HILM-YKWX-202202-1_rec0_try1");
		//campo attachmentUrl non presente
		Assertions.assertFalse(service.getAttachment(event), "Url non valida");
	}

}
