package it.pagopa.pnec.spedizionedocumenticartacei;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openapitools.client.model.OperationResultCodeResponse;
import org.openapitools.client.model.PaperDeliveryProgressesResponse;
import org.openapitools.client.model.PaperProgressStatusEvent;
import org.openapitools.client.model.PaperProgressStatusEventAttachments;
import org.threeten.bp.OffsetDateTime;

public class ServicePaperDocument {

	public boolean getConnection(HeaderRequest param) {
		if (param.xPagopaExtchServiceId != null) {
			return true;
		} else {
			System.out.println("----------------------------");
			System.out.println("Connection Refused");
			System.out.println("----------------------------");
			return false;
		}
	}

	public Object getStatusCode(HeaderRequest param) {
		if ((param.requestId != null && param.requestId != "") && (param.xPagopaExtchServiceId != null && param.xPagopaExtchServiceId != "")
				&& (param.xApiKey != null && param.xApiKey != "")) {
			ProgressiviConsegnaRispostaCartacea pcrc = new ProgressiviConsegnaRispostaCartacea();
			pcrc = getProgessStatusEvent(param);
			return pcrc;
		}
		RisultatoCodiceRisposta rcr = new RisultatoCodiceRisposta();
		rcr = errorCodeResponse(param);
		return rcr;
	}

	private RisultatoCodiceRisposta errorCodeResponse(HeaderRequest param) {
		
		RisultatoCodiceRisposta rcr = new RisultatoCodiceRisposta();
		rcr.setOpResCodeResp(new OperationResultCodeResponse());
		
		OffsetDateTime odt = OffsetDateTime.now();
		
		if ((param.xPagopaExtchServiceId == "") || (param.xApiKey == null || param.xApiKey == "")) {
			rcr.opResCodeResp.setResultCode("401.00");
			rcr.opResCodeResp.setResultDescription("Authentication Failed");
			rcr.opResCodeResp.setClientResponseTimeStamp(odt);
		} else if (param.requestId == null || param.requestId == "") {
			rcr.opResCodeResp.setResultCode("404.01");
			rcr.opResCodeResp.setResultDescription("requestId never sent");
			rcr.opResCodeResp.setClientResponseTimeStamp(odt);
		}
		System.out.println("----------------------------");
		System.out.println(rcr.toString());
		System.out.println("----------------------------");
		return rcr;
	}
	
	private ProgressiviConsegnaRispostaCartacea getProgessStatusEvent(HeaderRequest param) {
		
		ProgressiviConsegnaRispostaCartacea pcrc = new ProgressiviConsegnaRispostaCartacea();
		pcrc.setPapDelProgrResp(new PaperDeliveryProgressesResponse());
		
		pcrc.papDelProgrResp.setRequestId(param.getRequestId());
		
		ProgressivoStatoEventoCartaceo psec = new ProgressivoStatoEventoCartaceo();
		psec.setPapProgStEv(new PaperProgressStatusEvent());
		
		OffsetDateTime odt = OffsetDateTime.now();
		
		AllegatiProgressivoStatoRichiestaCartacea apsrc1 = new AllegatiProgressivoStatoRichiestaCartacea();
		apsrc1.setPapProgStEvAtt(new PaperProgressStatusEventAttachments());
		
//		NB: interfaccia con il consolidatore
//		apsrc1.papProgStEvAtt.setId("1");
		apsrc1.papProgStEvAtt.getId();
		apsrc1.papProgStEvAtt.setDocumentType("AR");
		apsrc1.papProgStEvAtt.setUri("https://www.eng.it/resources/whitepaper/doc/blockchain/Blockchain_whitepaper_it.pdf");
		apsrc1.papProgStEvAtt.setSha256("");
		apsrc1.papProgStEvAtt.setDate(odt);
		
		AllegatiProgressivoStatoRichiestaCartacea apsrc2 = new AllegatiProgressivoStatoRichiestaCartacea();
		apsrc2.setPapProgStEvAtt(new PaperProgressStatusEventAttachments());
		
		apsrc2.papProgStEvAtt.setId("2");
		apsrc2.papProgStEvAtt.setDocumentType("AR");
		apsrc2.papProgStEvAtt.setUri("https://assets.loescher.it/risorse/download/innovando/itastra/Scheda1_GliArticoli.pdf");
		apsrc2.papProgStEvAtt.setSha256("");
		apsrc2.papProgStEvAtt.setDate(odt);
		
		List<PaperProgressStatusEventAttachments> attachments = new ArrayList<PaperProgressStatusEventAttachments>();
		attachments.add(apsrc1.papProgStEvAtt);
		attachments.add(apsrc2.papProgStEvAtt);
		
		DiscoveredAddress da = new DiscoveredAddress();
		da.setName("Mario Rossi");
		da.setNameRow2("Mario Verdi");
		da.setAddress("Via senza nome 2");
		da.setAddressRow2("Via senza nome 3");
		da.setCap("00121");
		da.setCity("Cuneo");
		da.setCity2("Amalfi");
		da.setPr("Cu");
		da.setCountry("Italy");
		
		psec.papProgStEv.setRequestId("ABCD-HILM-YKWX-202202-1_rec0_try1");
		psec.papProgStEv.setRegisteredLetterCode("123456789abc");
		psec.papProgStEv.setProductType("AR");
		psec.papProgStEv.setIun("ABCD-HILM-YKWX-202202-1");
		psec.papProgStEv.setStatusCode("001");
		psec.papProgStEv.setStatusDescription("Stampato");
		psec.papProgStEv.setStatusDateTime(odt);
		psec.papProgStEv.setDeliveryFailureCause("01 destinatario irreperibile");
		psec.papProgStEv.setAttachments(attachments);
		psec.papProgStEv.setDiscoveredAddress(da);
		psec.papProgStEv.setClientRequestTimeStamp(odt);
		
		List<PaperProgressStatusEvent> listEvents = new ArrayList<PaperProgressStatusEvent>();
		listEvents.add(psec.papProgStEv);
		
		pcrc.papDelProgrResp.setEvents(listEvents);
		
		System.out.println("----------------------------");
		System.out.println(pcrc.toString());
		System.out.println("----------------------------");
		return pcrc;
	}

	public List<String> getAttachment(HeaderRequest hr) {
		
		List<String> atUrl = new ArrayList<String>();
		
		String s1 = getProgessStatusEvent(hr).papDelProgrResp.getEvents().get(0).getAttachments().get(0).getUri();
		String s2 = getProgessStatusEvent(hr).papDelProgrResp.getEvents().get(0).getAttachments().get(1).getUri();
		
		atUrl.add(s1);
		atUrl.add(s2);
		
		if ((hr.requestId != null && hr.requestId != "") 
			&& (hr.xPagopaExtchServiceId != null && hr.xPagopaExtchServiceId != "") 
			&& (hr.xApiKey != null && hr.xApiKey != "") 
			&& (s1 != null && s1 != "")
			&& (s2 != null && s2 != "")) {
						
			try {
				downloadUsingStream(s1, "C:\\Users\\fcrisciotti\\Downloads\\Blockchain_whitepaper_it.pdf");
				downloadUsingStream(s2, "C:\\Users\\fcrisciotti\\Downloads\\Scheda1_GliArticoli.pdf");
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("----------------------------");
			System.out.println(atUrl);
			System.out.println("----------------------------");
			return atUrl;
		}
		return null;
	}

	private static void downloadUsingStream(String urlStr, String file) throws IOException {
		URL url = new URL(urlStr);
		BufferedInputStream bis = new BufferedInputStream(url.openStream());
		FileOutputStream fis = new FileOutputStream(file);
		byte[] buffer = new byte[1024];
		int count = 0;
		while ((count = bis.read(buffer, 0, 1024)) != -1) {
			fis.write(buffer, 0, count);
		}
		fis.close();
		bis.close();
	}

	public RisultatoCodiceRisposta sendDocumentPaper(String serviceId, String apiKey, RichiestaImpegnoCartaceo richImpCart) {
		if (serviceId != null && apiKey != null && richImpCart != null) {
			RisultatoCodiceRisposta risCodRisp = new RisultatoCodiceRisposta();
			risCodRisp = callConsolidatore(serviceId, apiKey, richImpCart);
			return risCodRisp;
		}
		System.out.println("----------------------------");
		System.out.println("Connection Refused");
		System.out.println("----------------------------");
		return null;
	}

	private RisultatoCodiceRisposta callConsolidatore(String serviceId, String apiKey, RichiestaImpegnoCartaceo richImpCart) {

		RisultatoCodiceRisposta rcr = new RisultatoCodiceRisposta();
		rcr.setOpResCodeResp(new OperationResultCodeResponse());
		
		List<String> errorList = new ArrayList<String>();
		String errList1 = "field requestId is required";
		String errList2 = "unrecognized product Type";
		
		OffsetDateTime odt = OffsetDateTime.now();

		if (richImpCart.papEngReq.getRequestId() == "") {
			errorList.add(errList1);
			rcr.opResCodeResp.setResultCode("400.01");
			rcr.opResCodeResp.setResultDescription("Syntax Error");
			rcr.opResCodeResp.setErrorList(errorList);
			rcr.opResCodeResp.setClientResponseTimeStamp(odt);
		} else if (richImpCart.papEngReq.getProductType() != "AR" && richImpCart.papEngReq.getProductType() != "890"
				&& richImpCart.papEngReq.getProductType() != "RI" && richImpCart.papEngReq.getProductType() != "RS") {
			errorList.add(errList2);
			rcr.opResCodeResp.setResultCode("400.02");
			rcr.opResCodeResp.setResultDescription("Semantic Error");
			rcr.opResCodeResp.setErrorList(errorList);
			rcr.opResCodeResp.setClientResponseTimeStamp(odt);
		} // verificare caso se richiesta già esiste
		else if (richImpCart.papEngReq.getRequestId() == "1") {
			rcr.opResCodeResp.setResultCode("409.00");
			rcr.opResCodeResp.setResultDescription("duplicated requestId");
			rcr.opResCodeResp.setClientResponseTimeStamp(odt);
		} // ciclo for
		else if (richImpCart.papEngReq.getAttachments().get(0).getUri() == null
				|| richImpCart.papEngReq.getAttachments().get(1).getUri() == null) {
			rcr.opResCodeResp.setResultCode("404.00");
			rcr.opResCodeResp.setResultDescription("bad request");
			rcr.opResCodeResp.setClientResponseTimeStamp(odt);
		} else if ((apiKey == "" || serviceId == "")) {
			rcr.opResCodeResp.setResultCode("401.00");
			rcr.opResCodeResp.setResultDescription("Authentication Failed");
			rcr.opResCodeResp.setClientResponseTimeStamp(odt);
		} else {
			rcr.opResCodeResp.setResultCode("200.00");
			rcr.opResCodeResp.setResultDescription("OK");
			rcr.opResCodeResp.setClientResponseTimeStamp(odt);
			
			System.out.println("----------------------------");
			System.out.println(richImpCart.papEngReq.getAttachments().get(0).getUri());
			System.out.println(richImpCart.papEngReq.getAttachments().get(1).getUri());
			System.out.println("----------------------------");
		}
		System.out.println("----------------------------");
		System.out.println(rcr.toString());
		System.out.println("----------------------------");
		return rcr;
	}

}