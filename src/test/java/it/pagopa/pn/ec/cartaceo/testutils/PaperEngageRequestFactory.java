package it.pagopa.pn.ec.cartaceo.testutils;

import it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest;
import it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequestAttachments;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.OffsetDateTime;

public class PaperEngageRequestFactory {

	/**
	 *
	 * @param attachNum
	 * @return
	 */
	public static it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest createDtoPaperRequest(int attachNum) {

		var paperEngageRequestAttachments = getPaperEngageRequestAttachments("PN_EXTERNAL_LEGAL_FACTS");

		return getPaperEngageRequest(paperEngageRequestAttachments);
	}

	/**
	 *
	 * @param attachNum
	 * @return
	 */
	public static it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest createDtoPaperRequestPdfRaster(String requestPaId) {
		var paperEngageRequestAttachments = getPaperEngageRequestAttachments("PN_NOTIFICATION_ATTACHMENTS");

		return getPaperEngageRequest(paperEngageRequestAttachments).requestPaId(requestPaId);
	}

	private static @NotNull PaperEngageRequest getPaperEngageRequest(PaperEngageRequestAttachments paperEngageRequestAttachments) {
		var attachments = new ArrayList<PaperEngageRequestAttachments>();
		attachments.add(paperEngageRequestAttachments);
		/*for (int idx = 0; idx < attachNum; idx++) {
			paperEngageRequestAttachments.setOrder(new BigDecimal(idx));

		}*/

		var vas = new HashMap<String, String>();
		vas.put("Servizi", "valore aggiunto");
		var odt = OffsetDateTime.parse("2023-02-01T07:41:35.717Z");
        return getPaperEngageRequest(odt, attachments, vas);
	}

	private static @NotNull PaperEngageRequestAttachments getPaperEngageRequestAttachments(String documentTypeArchiving) {
		var paperEngageRequestAttachments = new PaperEngageRequestAttachments();
		paperEngageRequestAttachments.setUri(new StringBuilder().append("safestorage://").append(documentTypeArchiving).append("-14d277f9beb4c8a9da322092c350d51").toString());
		paperEngageRequestAttachments.setDocumentType("AAR");
		paperEngageRequestAttachments.setSha256("stringstringstringstringstringstringstring");
		paperEngageRequestAttachments.setOrder(new BigDecimal(0));
		return paperEngageRequestAttachments;
	}

	private static @NotNull PaperEngageRequest getPaperEngageRequest(OffsetDateTime odt, ArrayList<PaperEngageRequestAttachments> attachments, HashMap<String, String> vas) {
		var paperEngageRequestFactory = new PaperEngageRequest();
		paperEngageRequestFactory.setIun("ABCD-HILM-YKWX-202202-1");
		paperEngageRequestFactory.setRequestId("ABCD-HILM-YKWX-202202-1_rec0_try1");
		paperEngageRequestFactory.setRequestPaId("00414580183");
		paperEngageRequestFactory.setClientRequestTimeStamp(odt);
		paperEngageRequestFactory.setProductType("AR");
		paperEngageRequestFactory.setAttachments(attachments);
		paperEngageRequestFactory.setPrintType("BN_FRONTE_RETRO");
		paperEngageRequestFactory.setReceiverName("Mario Rossi");
		paperEngageRequestFactory.setReceiverNameRow2("famiglia Bianchi");
		paperEngageRequestFactory.setReceiverAddress("via senza nome 610106");
		paperEngageRequestFactory.setReceiverAddressRow2("scala Z interno 400");
		paperEngageRequestFactory.setReceiverCap("40050");
		paperEngageRequestFactory.setReceiverCity("Argelato");
		paperEngageRequestFactory.setReceiverCity2("fraz. malacappa");
		paperEngageRequestFactory.setReceiverPr("BO");
		paperEngageRequestFactory.setReceiverCountry("Ita");
		paperEngageRequestFactory.setReceiverFiscalCode("00011234");
		paperEngageRequestFactory.setSenderName("Ragione sociale PagoPA");
		paperEngageRequestFactory.setSenderAddress("via senza nome 61010");
		paperEngageRequestFactory.setSenderCity("Cuneo");
		paperEngageRequestFactory.setSenderPr("Cu");
		paperEngageRequestFactory.setSenderDigitalAddress("via senza nome 61010");
		paperEngageRequestFactory.setArName("Mario Rossi");
		paperEngageRequestFactory.setArAddress("via senza nome 61010");
		paperEngageRequestFactory.setArCap("00144");
		paperEngageRequestFactory.setArCity("Cuneo");
		paperEngageRequestFactory.setVas(vas);
		return paperEngageRequestFactory;
	}

	public static PaperEngageRequest createApiPaperRequest(int attachNum) {

		var odt = OffsetDateTime.now();

		var paperEngageRequestAttachments = new PaperEngageRequestAttachments();
		paperEngageRequestAttachments.setUri("safestorage://PN_EXTERNAL_LEGAL_FACTS-14d277f9beb4c8a9da322092c350d51");
		paperEngageRequestAttachments.setDocumentType("AR");
		paperEngageRequestAttachments.setSha256("");

		var attachments = new ArrayList<PaperEngageRequestAttachments>();
		for (int idx = 0; idx < attachNum; idx++) {
			paperEngageRequestAttachments.setOrder(new BigDecimal(idx));
			attachments.add(paperEngageRequestAttachments);
		}

		var vas = new HashMap<String, String>();
		vas.put("Servizi", "valore aggiunto");

		var paperEngageRequestFactory = new PaperEngageRequest();

		paperEngageRequestFactory.setIun("ABCD-HILM-YKWX-202202-1");
		paperEngageRequestFactory.setRequestId("requestId");
		paperEngageRequestFactory.setRequestPaId("00414580183");
		paperEngageRequestFactory.setClientRequestTimeStamp(odt);
		paperEngageRequestFactory.setProductType("AR");
		paperEngageRequestFactory.setAttachments(attachments);
		paperEngageRequestFactory.setPrintType("BN_FRONTE_RETRO");
		paperEngageRequestFactory.setReceiverName("Mario Rossi");
		paperEngageRequestFactory.setReceiverNameRow2("c/o famiglia Bianchi");
		paperEngageRequestFactory.setReceiverAddress("via senza nome 610106");
		paperEngageRequestFactory.setReceiverAddressRow2("scala Z interno 400");
		paperEngageRequestFactory.setReceiverCap("40050");
		paperEngageRequestFactory.setReceiverCity("Argelato");
		paperEngageRequestFactory.setReceiverCity2("fraz. malacappa");
		paperEngageRequestFactory.setReceiverPr("BO");
		paperEngageRequestFactory.setReceiverCountry("Ita");
		paperEngageRequestFactory.setReceiverFiscalCode("00011234");
		paperEngageRequestFactory.setSenderName("Ragione sociale PagoPA");
		paperEngageRequestFactory.setSenderAddress("via senza nome 61010");
		paperEngageRequestFactory.setSenderCity("Cuneo");
		paperEngageRequestFactory.setSenderPr("Cu");
		paperEngageRequestFactory.setSenderDigitalAddress("via senza nome 61010");
		paperEngageRequestFactory.setArName("Mario Rossi");
		paperEngageRequestFactory.setArAddress("via senza nome 61010");
		paperEngageRequestFactory.setArCap("00144");
		paperEngageRequestFactory.setArCity("Cuneo");
		paperEngageRequestFactory.setVas(vas);

		return paperEngageRequestFactory;
	}

}
