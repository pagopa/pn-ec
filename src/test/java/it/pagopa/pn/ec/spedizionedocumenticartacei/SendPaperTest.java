package it.pagopa.pn.ec.spedizionedocumenticartacei;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openapitools.client.model.PaperEngageRequest;
import org.openapitools.client.model.PaperEngageRequestAttachments;
import org.threeten.bp.OffsetDateTime;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SendPaperTest {
	
	ServicePaperDocument service = new ServicePaperDocument();
	
	private static final String SERVICE_ID = "CONSOLIDATORE SERVER";
	private static final String API_KEY = "FCRISCIOTTI";
	
	@Test
	//SCI.100.1 inviare documento con successo
	void sendDocumentSuccess() {
		
		RichiestaImpegnoCartaceo richImpCart = new RichiestaImpegnoCartaceo();
		richImpCart.setPapEngReq(new PaperEngageRequest());
		
		AllegatiImpegnoRichiestaCartaceo allImpRichCart1 = new AllegatiImpegnoRichiestaCartaceo();
		allImpRichCart1.setPapEngReqAtt(new PaperEngageRequestAttachments());
		allImpRichCart1.papEngReqAtt.setUri("https://www.eng.it/resources/whitepaper/doc/blockchain/Blockchain_whitepaper_it.pdf");
		allImpRichCart1.papEngReqAtt.setOrder(new BigDecimal("0"));
		allImpRichCart1.papEngReqAtt.setDocumentType("AR");
		allImpRichCart1.papEngReqAtt.setSha256("");
		
		AllegatiImpegnoRichiestaCartaceo allImpRichCart2 = new AllegatiImpegnoRichiestaCartaceo();
		allImpRichCart2.setPapEngReqAtt(new PaperEngageRequestAttachments());
		allImpRichCart2.papEngReqAtt.setUri("https://assets.loescher.it/risorse/download/innovando/itastra/Scheda1_GliArticoli.pdf");
		allImpRichCart2.papEngReqAtt.setOrder(new BigDecimal("1"));
		allImpRichCart2.papEngReqAtt.setDocumentType("AR");
		allImpRichCart2.papEngReqAtt.setSha256("");
		
		List<PaperEngageRequestAttachments> attachments = new ArrayList<PaperEngageRequestAttachments>();
		
		attachments.add(allImpRichCart1.papEngReqAtt);
		attachments.add(allImpRichCart2.papEngReqAtt);
		
		OffsetDateTime odt = OffsetDateTime.now();
		
		Map<String, String> vas = new HashMap<String,String>();
		
		vas.put("Servizi", "valore aggiunto");
		
		richImpCart.papEngReq.setIun("ABCD-HILM-YKWX-202202-1");
		richImpCart.papEngReq.setRequestId("ABCD-HILM-YKWX-202202-1_rec0_try1");
		richImpCart.papEngReq.setRequestPaId("00414580183");
		richImpCart.papEngReq.setClientRequestTimeStamp(odt);
		richImpCart.papEngReq.setProductType("AR");
		richImpCart.papEngReq.setAttachments(attachments);
		richImpCart.papEngReq.setPrintType("BN_FRONTE_RETRO");
		richImpCart.papEngReq.setReceiverName("Mario Rossi");
		richImpCart.papEngReq.setReceiverNameRow2("c/o famiglia Bianchi");
		richImpCart.papEngReq.setReceiverAddress("via senza nome 610106");
		richImpCart.papEngReq.setReceiverAddressRow2("scala Z interno 400");
		richImpCart.papEngReq.setReceiverCap("40050");
		richImpCart.papEngReq.setReceiverCity("Argelato");
		richImpCart.papEngReq.setReceiverCity2("fraz. malacappa");
		richImpCart.papEngReq.setReceiverPr("BO");
		richImpCart.papEngReq.setReceiverCountry("Ita");
		richImpCart.papEngReq.setReceiverFiscalCode("00011234");
		richImpCart.papEngReq.setSenderName("Ragione sociale PagoPA");
		richImpCart.papEngReq.setSenderAddress("via senza nome 61010");
		richImpCart.papEngReq.setSenderCity("Cuneo");
		richImpCart.papEngReq.setSenderPr("Cu");
		richImpCart.papEngReq.setSenderDigitalAddress("via senza nome 61010");
		richImpCart.papEngReq.setArName("Mario Rossi");
		richImpCart.papEngReq.setArAddress("via senza nome 61010");
		richImpCart.papEngReq.setArCap("00144");
		richImpCart.papEngReq.setArCity("Cuneo");
		richImpCart.papEngReq.setVas(vas);
		
		System.out.println(richImpCart.toString());
		
		Assertions.assertNotNull(service.sendDocumentPaper(SERVICE_ID,API_KEY,richImpCart), "Richiesta inviata correttamente");
	}
	
	@Test
	//SCI.100.2 inviare documento senza allegato
	void sendDocumentAttachmentFailed() {
		RichiestaImpegnoCartaceo richImpCart = new RichiestaImpegnoCartaceo();
		richImpCart.setPapEngReq(new PaperEngageRequest());
		
		AllegatiImpegnoRichiestaCartaceo allImpRichCart1 = new AllegatiImpegnoRichiestaCartaceo();
		allImpRichCart1.setPapEngReqAtt(new PaperEngageRequestAttachments());
		allImpRichCart1.papEngReqAtt.setUri("");
		allImpRichCart1.papEngReqAtt.setOrder(new BigDecimal("0"));
		allImpRichCart1.papEngReqAtt.setDocumentType("AR");
		allImpRichCart1.papEngReqAtt.setSha256("");
		List<PaperEngageRequestAttachments> attachments = new ArrayList<>();
		attachments.add(allImpRichCart1.papEngReqAtt);
		
		OffsetDateTime odt = OffsetDateTime.now();
		
		Map<String, String> vas = new HashMap<String,String>();
		
		vas.put("Servizi", "valore aggiunto");
		
		richImpCart.papEngReq.setIun("ABCD-HILM-YKWX-202202-1");
		richImpCart.papEngReq.setRequestId("ABCD-HILM-YKWX-202202-1_rec0_try1");
		richImpCart.papEngReq.setRequestPaId("00414580183");
		richImpCart.papEngReq.setClientRequestTimeStamp(odt);
		richImpCart.papEngReq.setProductType("AR");
		richImpCart.papEngReq.setAttachments(attachments);
		richImpCart.papEngReq.setPrintType("BN_FRONTE_RETRO");
		richImpCart.papEngReq.setReceiverName("Mario Rossi");
		richImpCart.papEngReq.setReceiverNameRow2("c/o famiglia Bianchi");
		richImpCart.papEngReq.setReceiverAddress("via senza nome 610106");
		richImpCart.papEngReq.setReceiverAddressRow2("scala Z interno 400");
		richImpCart.papEngReq.setReceiverCap("40050");
		richImpCart.papEngReq.setReceiverCity("Argelato");
		richImpCart.papEngReq.setReceiverCity2("fraz. malacappa");
		richImpCart.papEngReq.setReceiverPr("BO");
		richImpCart.papEngReq.setReceiverCountry("Ita");
		richImpCart.papEngReq.setReceiverFiscalCode("00011234");
		richImpCart.papEngReq.setSenderName("Ragione sociale PagoPA");
		richImpCart.papEngReq.setSenderAddress("via senza nome 61010");
		richImpCart.papEngReq.setSenderCity("Cuneo");
		richImpCart.papEngReq.setSenderPr("Cu");
		richImpCart.papEngReq.setSenderDigitalAddress("via senza nome 61010");
		richImpCart.papEngReq.setArName("Mario Rossi");
		richImpCart.papEngReq.setArAddress("via senza nome 61010");
		richImpCart.papEngReq.setArCap("00144");
		richImpCart.papEngReq.setArCity("Cuneo");
		richImpCart.papEngReq.setVas(vas);
		
		System.out.println(richImpCart.toString());
		
		Assertions.assertNotNull(service.sendDocumentPaper(SERVICE_ID,API_KEY,richImpCart), "Allegato non presente");
	}
	
	@Test
	//SCI.100.3 inviare richiesta di spedizione con requestId già presente a sistema
	void sendDocumentDuplicateRequestIdFailed() {
		
		RichiestaImpegnoCartaceo richImpCart = new RichiestaImpegnoCartaceo();
		richImpCart.setPapEngReq(new PaperEngageRequest());
		
		AllegatiImpegnoRichiestaCartaceo allImpRichCart1 = new AllegatiImpegnoRichiestaCartaceo();
		allImpRichCart1.setPapEngReqAtt(new PaperEngageRequestAttachments());
		allImpRichCart1.papEngReqAtt.setUri("https://www.eng.it/resources/whitepaper/doc/blockchain/Blockchain_whitepaper_it.pdf");
		allImpRichCart1.papEngReqAtt.setOrder(new BigDecimal("0"));
		allImpRichCart1.papEngReqAtt.setDocumentType("AR");
		allImpRichCart1.papEngReqAtt.setSha256("");
		
		AllegatiImpegnoRichiestaCartaceo allImpRichCart2 = new AllegatiImpegnoRichiestaCartaceo();
		allImpRichCart2.setPapEngReqAtt(new PaperEngageRequestAttachments());
		allImpRichCart2.papEngReqAtt.setUri("https://www.eng.it/resources/whitepaper/doc/blockchain/Blockchain_whitepaper_it.pdf");
		allImpRichCart2.papEngReqAtt.setOrder(new BigDecimal("0"));
		allImpRichCart2.papEngReqAtt.setDocumentType("AR");
		allImpRichCart2.papEngReqAtt.setSha256("");
		
		List<PaperEngageRequestAttachments> attachments = new ArrayList<PaperEngageRequestAttachments>();
		
		attachments.add(allImpRichCart1.papEngReqAtt);
		attachments.add(allImpRichCart2.papEngReqAtt);
		
		OffsetDateTime odt = OffsetDateTime.now();
		
		Map<String, String> vas = new HashMap<String,String>();
		
		vas.put("Servizi", "valore aggiunto");
		
		richImpCart.papEngReq.setIun("ABCD-HILM-YKWX-202202-1");
		richImpCart.papEngReq.setRequestId("1");
		richImpCart.papEngReq.setRequestPaId("00414580183");
		richImpCart.papEngReq.setClientRequestTimeStamp(odt);
		richImpCart.papEngReq.setProductType("AR");
		richImpCart.papEngReq.setAttachments(attachments);
		richImpCart.papEngReq.setPrintType("BN_FRONTE_RETRO");
		richImpCart.papEngReq.setReceiverName("Mario Rossi");
		richImpCart.papEngReq.setReceiverNameRow2("c/o famiglia Bianchi");
		richImpCart.papEngReq.setReceiverAddress("via senza nome 610106");
		richImpCart.papEngReq.setReceiverAddressRow2("scala Z interno 400");
		richImpCart.papEngReq.setReceiverCap("40050");
		richImpCart.papEngReq.setReceiverCity("Argelato");
		richImpCart.papEngReq.setReceiverCity2("fraz. malacappa");
		richImpCart.papEngReq.setReceiverPr("BO");
		richImpCart.papEngReq.setReceiverCountry("Ita");
		richImpCart.papEngReq.setReceiverFiscalCode("00011234");
		richImpCart.papEngReq.setSenderName("Ragione sociale PagoPA");
		richImpCart.papEngReq.setSenderAddress("via senza nome 61010");
		richImpCart.papEngReq.setSenderCity("Cuneo");
		richImpCart.papEngReq.setSenderPr("Cu");
		richImpCart.papEngReq.setSenderDigitalAddress("via senza nome 61010");
		richImpCart.papEngReq.setArName("Mario Rossi");
		richImpCart.papEngReq.setArAddress("via senza nome 61010");
		richImpCart.papEngReq.setArCap("00144");
		richImpCart.papEngReq.setArCity("Cuneo");
		richImpCart.papEngReq.setVas(vas);
		
		System.out.println(richImpCart.toString());
		
		Assertions.assertNotNull(service.sendDocumentPaper(SERVICE_ID,API_KEY,richImpCart), "RequestId già presente a sistema");
	}
	
	@Test
	//syntax error
	void syntaxErrorSendFailed() {
		
		RichiestaImpegnoCartaceo richImpCart = new RichiestaImpegnoCartaceo();
		richImpCart.setPapEngReq(new PaperEngageRequest());
		
		AllegatiImpegnoRichiestaCartaceo allImpRichCart1 = new AllegatiImpegnoRichiestaCartaceo();
		allImpRichCart1.setPapEngReqAtt(new PaperEngageRequestAttachments());
		allImpRichCart1.papEngReqAtt.setUri("https://www.eng.it/resources/whitepaper/doc/blockchain/Blockchain_whitepaper_it.pdf");
		allImpRichCart1.papEngReqAtt.setOrder(new BigDecimal("0"));
		allImpRichCart1.papEngReqAtt.setDocumentType("AR");
		allImpRichCart1.papEngReqAtt.setSha256("");
		
		AllegatiImpegnoRichiestaCartaceo allImpRichCart2 = new AllegatiImpegnoRichiestaCartaceo();
		allImpRichCart2.setPapEngReqAtt(new PaperEngageRequestAttachments());
		allImpRichCart2.papEngReqAtt.setUri("https://www.eng.it/resources/whitepaper/doc/blockchain/Blockchain_whitepaper_it.pdf");
		allImpRichCart2.papEngReqAtt.setOrder(new BigDecimal("0"));
		allImpRichCart2.papEngReqAtt.setDocumentType("AR");
		allImpRichCart2.papEngReqAtt.setSha256("");
		
		List<PaperEngageRequestAttachments> attachments = new ArrayList<PaperEngageRequestAttachments>();
		
		attachments.add(allImpRichCart1.papEngReqAtt);
		attachments.add(allImpRichCart2.papEngReqAtt);
		
		OffsetDateTime odt = OffsetDateTime.now();
		
		Map<String, String> vas = new HashMap<String,String>();
		
		vas.put("Servizi", "valore aggiunto");
		
		richImpCart.papEngReq.setIun("ABCD-HILM-YKWX-202202-1");
		richImpCart.papEngReq.setRequestId("");
		richImpCart.papEngReq.setRequestPaId("00414580183");
		richImpCart.papEngReq.setClientRequestTimeStamp(odt);
		richImpCart.papEngReq.setProductType("AR");
		richImpCart.papEngReq.setAttachments(attachments);
		richImpCart.papEngReq.setPrintType("BN_FRONTE_RETRO");
		richImpCart.papEngReq.setReceiverName("Mario Rossi");
		richImpCart.papEngReq.setReceiverNameRow2("c/o famiglia Bianchi");
		richImpCart.papEngReq.setReceiverAddress("via senza nome 610106");
		richImpCart.papEngReq.setReceiverAddressRow2("scala Z interno 400");
		richImpCart.papEngReq.setReceiverCap("40050");
		richImpCart.papEngReq.setReceiverCity("Argelato");
		richImpCart.papEngReq.setReceiverCity2("fraz. malacappa");
		richImpCart.papEngReq.setReceiverPr("BO");
		richImpCart.papEngReq.setReceiverCountry("Ita");
		richImpCart.papEngReq.setReceiverFiscalCode("00011234");
		richImpCart.papEngReq.setSenderName("Ragione sociale PagoPA");
		richImpCart.papEngReq.setSenderAddress("via senza nome 61010");
		richImpCart.papEngReq.setSenderCity("Cuneo");
		richImpCart.papEngReq.setSenderPr("Cu");
		richImpCart.papEngReq.setSenderDigitalAddress("via senza nome 61010");
		richImpCart.papEngReq.setArName("Mario Rossi");
		richImpCart.papEngReq.setArAddress("via senza nome 61010");
		richImpCart.papEngReq.setArCap("00144");
		richImpCart.papEngReq.setArCity("Cuneo");
		richImpCart.papEngReq.setVas(vas);
		
		System.out.println(richImpCart.toString());
		
		Assertions.assertNotNull(service.sendDocumentPaper(SERVICE_ID,API_KEY,richImpCart), "field requestId is required");
	}
	
	@Test
	//semantic error
	void semanticErrorSendFailed() {
		
		RichiestaImpegnoCartaceo richImpCart = new RichiestaImpegnoCartaceo();
		richImpCart.setPapEngReq(new PaperEngageRequest());
		
		AllegatiImpegnoRichiestaCartaceo allImpRichCart1 = new AllegatiImpegnoRichiestaCartaceo();
		allImpRichCart1.setPapEngReqAtt(new PaperEngageRequestAttachments());
		allImpRichCart1.papEngReqAtt.setUri("https://www.eng.it/resources/whitepaper/doc/blockchain/Blockchain_whitepaper_it.pdf");
		allImpRichCart1.papEngReqAtt.setOrder(new BigDecimal("0"));
		allImpRichCart1.papEngReqAtt.setDocumentType("AR");
		allImpRichCart1.papEngReqAtt.setSha256("");
		
		AllegatiImpegnoRichiestaCartaceo allImpRichCart2 = new AllegatiImpegnoRichiestaCartaceo();
		allImpRichCart2.setPapEngReqAtt(new PaperEngageRequestAttachments());
		allImpRichCart2.papEngReqAtt.setUri("https://www.eng.it/resources/whitepaper/doc/blockchain/Blockchain_whitepaper_it.pdf");
		allImpRichCart2.papEngReqAtt.setOrder(new BigDecimal("0"));
		allImpRichCart2.papEngReqAtt.setDocumentType("AR");
		allImpRichCart2.papEngReqAtt.setSha256("");
		
		List<PaperEngageRequestAttachments> attachments = new ArrayList<PaperEngageRequestAttachments>();
		
		attachments.add(allImpRichCart1.papEngReqAtt);
		attachments.add(allImpRichCart2.papEngReqAtt);
		
		OffsetDateTime odt = OffsetDateTime.now();
		
		Map<String, String> vas = new HashMap<String,String>();
		
		vas.put("Servizi", "valore aggiunto");
		
		richImpCart.papEngReq.setIun("ABCD-HILM-YKWX-202202-1");
		richImpCart.papEngReq.setRequestId("ABCD-HILM-YKWX-202202-1_rec0_try1");
		richImpCart.papEngReq.setRequestPaId("00414580183");
		richImpCart.papEngReq.setClientRequestTimeStamp(odt);
		richImpCart.papEngReq.setProductType("ARU");
		richImpCart.papEngReq.setAttachments(attachments);
		richImpCart.papEngReq.setPrintType("BN_FRONTE_RETRO");
		richImpCart.papEngReq.setReceiverName("Mario Rossi");
		richImpCart.papEngReq.setReceiverNameRow2("c/o famiglia Bianchi");
		richImpCart.papEngReq.setReceiverAddress("via senza nome 610106");
		richImpCart.papEngReq.setReceiverAddressRow2("scala Z interno 400");
		richImpCart.papEngReq.setReceiverCap("40050");
		richImpCart.papEngReq.setReceiverCity("Argelato");
		richImpCart.papEngReq.setReceiverCity2("fraz. malacappa");
		richImpCart.papEngReq.setReceiverPr("BO");
		richImpCart.papEngReq.setReceiverCountry("Ita");
		richImpCart.papEngReq.setReceiverFiscalCode("00011234");
		richImpCart.papEngReq.setSenderName("Ragione sociale PagoPA");
		richImpCart.papEngReq.setSenderAddress("via senza nome 61010");
		richImpCart.papEngReq.setSenderCity("Cuneo");
		richImpCart.papEngReq.setSenderPr("Cu");
		richImpCart.papEngReq.setSenderDigitalAddress("via senza nome 61010");
		richImpCart.papEngReq.setArName("Mario Rossi");
		richImpCart.papEngReq.setArAddress("via senza nome 61010");
		richImpCart.papEngReq.setArCap("00144");
		richImpCart.papEngReq.setArCity("Cuneo");
		richImpCart.papEngReq.setVas(vas);
		
		System.out.println(richImpCart.toString());
		
		Assertions.assertNotNull(service.sendDocumentPaper(SERVICE_ID,API_KEY,richImpCart), "unrecognized product Type");
	}
	
	@Test
	//authenticationFailed
	void authenticationFailed() {
		
		RichiestaImpegnoCartaceo richImpCart = new RichiestaImpegnoCartaceo();
		richImpCart.setPapEngReq(new PaperEngageRequest());
		
		AllegatiImpegnoRichiestaCartaceo allImpRichCart1 = new AllegatiImpegnoRichiestaCartaceo();
		allImpRichCart1.setPapEngReqAtt(new PaperEngageRequestAttachments());
		allImpRichCart1.papEngReqAtt.setUri("https://www.eng.it/resources/whitepaper/doc/blockchain/Blockchain_whitepaper_it.pdf");
		allImpRichCart1.papEngReqAtt.setOrder(new BigDecimal("0"));
		allImpRichCart1.papEngReqAtt.setDocumentType("AR");
		allImpRichCart1.papEngReqAtt.setSha256("");
		
		AllegatiImpegnoRichiestaCartaceo allImpRichCart2 = new AllegatiImpegnoRichiestaCartaceo();
		allImpRichCart2.setPapEngReqAtt(new PaperEngageRequestAttachments());
		allImpRichCart2.papEngReqAtt.setUri("https://www.eng.it/resources/whitepaper/doc/blockchain/Blockchain_whitepaper_it.pdf");
		allImpRichCart2.papEngReqAtt.setOrder(new BigDecimal("0"));
		allImpRichCart2.papEngReqAtt.setDocumentType("AR");
		allImpRichCart2.papEngReqAtt.setSha256("");
		
		List<PaperEngageRequestAttachments> attachments = new ArrayList<PaperEngageRequestAttachments>();
		
		attachments.add(allImpRichCart1.papEngReqAtt);
		attachments.add(allImpRichCart2.papEngReqAtt);
		
		OffsetDateTime odt = OffsetDateTime.now();
		
		Map<String, String> vas = new HashMap<String,String>();
		
		vas.put("Servizi", "valore aggiunto");
		
		richImpCart.papEngReq.setIun("ABCD-HILM-YKWX-202202-1");
		richImpCart.papEngReq.setRequestId("ABCD-HILM-YKWX-202202-1_rec0_try1");
		richImpCart.papEngReq.setRequestPaId("00414580183");
		richImpCart.papEngReq.setClientRequestTimeStamp(odt);
		richImpCart.papEngReq.setProductType("AR");
		richImpCart.papEngReq.setAttachments(attachments);
		richImpCart.papEngReq.setPrintType("BN_FRONTE_RETRO");
		richImpCart.papEngReq.setReceiverName("Mario Rossi");
		richImpCart.papEngReq.setReceiverNameRow2("c/o famiglia Bianchi");
		richImpCart.papEngReq.setReceiverAddress("via senza nome 610106");
		richImpCart.papEngReq.setReceiverAddressRow2("scala Z interno 400");
		richImpCart.papEngReq.setReceiverCap("40050");
		richImpCart.papEngReq.setReceiverCity("Argelato");
		richImpCart.papEngReq.setReceiverCity2("fraz. malacappa");
		richImpCart.papEngReq.setReceiverPr("BO");
		richImpCart.papEngReq.setReceiverCountry("Ita");
		richImpCart.papEngReq.setReceiverFiscalCode("00011234");
		richImpCart.papEngReq.setSenderName("Ragione sociale PagoPA");
		richImpCart.papEngReq.setSenderAddress("via senza nome 61010");
		richImpCart.papEngReq.setSenderCity("Cuneo");
		richImpCart.papEngReq.setSenderPr("Cu");
		richImpCart.papEngReq.setSenderDigitalAddress("via senza nome 61010");
		richImpCart.papEngReq.setArName("Mario Rossi");
		richImpCart.papEngReq.setArAddress("via senza nome 61010");
		richImpCart.papEngReq.setArCap("00144");
		richImpCart.papEngReq.setArCity("Cuneo");
		richImpCart.papEngReq.setVas(vas);
		
		System.out.println(richImpCart.toString());
		
		Assertions.assertNotNull(service.sendDocumentPaper("",API_KEY,richImpCart), "authentication failed");
	}
	
	@Test
	//connectionrefused
	void connectionFailed() {
		
		RichiestaImpegnoCartaceo richImpCart = new RichiestaImpegnoCartaceo();
		richImpCart.setPapEngReq(new PaperEngageRequest());
		
		AllegatiImpegnoRichiestaCartaceo allImpRichCart1 = new AllegatiImpegnoRichiestaCartaceo();
		allImpRichCart1.setPapEngReqAtt(new PaperEngageRequestAttachments());
		allImpRichCart1.papEngReqAtt.setUri("https://www.eng.it/resources/whitepaper/doc/blockchain/Blockchain_whitepaper_it.pdf");
		allImpRichCart1.papEngReqAtt.setOrder(new BigDecimal("0"));
		allImpRichCart1.papEngReqAtt.setDocumentType("AR");
		allImpRichCart1.papEngReqAtt.setSha256("");
		
		AllegatiImpegnoRichiestaCartaceo allImpRichCart2 = new AllegatiImpegnoRichiestaCartaceo();
		allImpRichCart2.setPapEngReqAtt(new PaperEngageRequestAttachments());
		allImpRichCart2.papEngReqAtt.setUri("https://www.eng.it/resources/whitepaper/doc/blockchain/Blockchain_whitepaper_it.pdf");
		allImpRichCart2.papEngReqAtt.setOrder(new BigDecimal("0"));
		allImpRichCart2.papEngReqAtt.setDocumentType("AR");
		allImpRichCart2.papEngReqAtt.setSha256("");
		
		List<PaperEngageRequestAttachments> attachments = new ArrayList<PaperEngageRequestAttachments>();
		
		attachments.add(allImpRichCart1.papEngReqAtt);
		attachments.add(allImpRichCart2.papEngReqAtt);
		
		OffsetDateTime odt = OffsetDateTime.now();
		
		Map<String, String> vas = new HashMap<String,String>();
		
		vas.put("Servizi", "valore aggiunto");
		
		richImpCart.papEngReq.setIun("ABCD-HILM-YKWX-202202-1");
		richImpCart.papEngReq.setRequestId("ABCD-HILM-YKWX-202202-1_rec0_try1");
		richImpCart.papEngReq.setRequestPaId("00414580183");
		richImpCart.papEngReq.setClientRequestTimeStamp(odt);
		richImpCart.papEngReq.setProductType("AR");
		richImpCart.papEngReq.setAttachments(attachments);
		richImpCart.papEngReq.setPrintType("BN_FRONTE_RETRO");
		richImpCart.papEngReq.setReceiverName("Mario Rossi");
		richImpCart.papEngReq.setReceiverNameRow2("c/o famiglia Bianchi");
		richImpCart.papEngReq.setReceiverAddress("via senza nome 610106");
		richImpCart.papEngReq.setReceiverAddressRow2("scala Z interno 400");
		richImpCart.papEngReq.setReceiverCap("40050");
		richImpCart.papEngReq.setReceiverCity("Argelato");
		richImpCart.papEngReq.setReceiverCity2("fraz. malacappa");
		richImpCart.papEngReq.setReceiverPr("BO");
		richImpCart.papEngReq.setReceiverCountry("Ita");
		richImpCart.papEngReq.setReceiverFiscalCode("00011234");
		richImpCart.papEngReq.setSenderName("Ragione sociale PagoPA");
		richImpCart.papEngReq.setSenderAddress("via senza nome 61010");
		richImpCart.papEngReq.setSenderCity("Cuneo");
		richImpCart.papEngReq.setSenderPr("Cu");
		richImpCart.papEngReq.setSenderDigitalAddress("via senza nome 61010");
		richImpCart.papEngReq.setArName("Mario Rossi");
		richImpCart.papEngReq.setArAddress("via senza nome 61010");
		richImpCart.papEngReq.setArCap("00144");
		richImpCart.papEngReq.setArCity("Cuneo");
		richImpCart.papEngReq.setVas(vas);
		
		System.out.println(richImpCart.toString());
		
		Assertions.assertNull(service.sendDocumentPaper(null,API_KEY,richImpCart), "Connection refused");
	}

}
