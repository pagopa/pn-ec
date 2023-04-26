package it.pagopa.pn.ec.spedizionedocumenticartacei;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.pagopa.pn.ec.rest.v1.dto.*;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ServicePaperDocument {

	static void space() {
		log.info("----------------------------");
	}
	
	public boolean getConnection(HeaderRequest param) {
		if (param.xPagopaExtchServiceId != null) {
			return true;
		} else {
			space();
			log.info("Connection Refused");
			space();
			return false;
		}
	}

	public Object getStatusCode(HeaderRequest param) {
		if ((param.requestId != null && !param.requestId.equals("")) && (param.xPagopaExtchServiceId != null && !param.xPagopaExtchServiceId.equals(""))
				&& (param.xApiKey != null && !param.xApiKey.equals(""))) {
			return getProgessStatusEvent(param);
		}
		return errorCodeResponse(param);
	}

	private RisultatoCodiceRisposta errorCodeResponse(HeaderRequest param) {
		
		RisultatoCodiceRisposta rcr = new RisultatoCodiceRisposta();
		rcr.setOpResCodeResp(new OperationResultCodeResponse());
		
		OffsetDateTime odt = OffsetDateTime.now();
		
		if ((param.xPagopaExtchServiceId.equals("")) || (param.xApiKey == null || param.xApiKey.equals(""))) {
			rcr.opResCodeResp.setResultCode("401.00");
			rcr.opResCodeResp.setResultDescription("Authentication Failed");
			rcr.opResCodeResp.setClientResponseTimeStamp(odt);
		} else if (param.requestId == null || param.requestId.equals("")) {
			rcr.opResCodeResp.setResultCode("404.01");
			rcr.opResCodeResp.setResultDescription("requestId never sent");
			rcr.opResCodeResp.setClientResponseTimeStamp(odt);
		}
		space();
		log.info(rcr.toString());
		space();
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
		
		List<PaperProgressStatusEventAttachments> attachments = new ArrayList<>();
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
		
		List<PaperProgressStatusEvent> listEvents = new ArrayList<>();
		listEvents.add(psec.papProgStEv);
		
		pcrc.papDelProgrResp.setEvents(listEvents);
		
		space();
		log.info(pcrc.toString());
		space();
		return pcrc;
	}

	public List<String> getAttachment(HeaderRequest hr) {
		
		List<String> atUrl = new ArrayList<>();
		
		String s1 = getProgessStatusEvent(hr).papDelProgrResp.getEvents().get(0).getAttachments().get(0).getUri();
		String s2 = getProgessStatusEvent(hr).papDelProgrResp.getEvents().get(0).getAttachments().get(1).getUri();
		
		atUrl.add(s1);
		atUrl.add(s2);
		
		if ((hr.requestId != null && !hr.requestId.equals("")) 
			&& (hr.xPagopaExtchServiceId != null && !hr.xPagopaExtchServiceId.equals("")) 
			&& (hr.xApiKey != null && !hr.xApiKey.equals("")) 
			&& (!s1.equals(""))
			&& (!s2.equals(""))) {
						
			try {
				downloadUsingStream(s1, "C:\\Users\\fcrisciotti\\Downloads\\Blockchain_whitepaper_it.pdf");
				downloadUsingStream(s2, "C:\\Users\\fcrisciotti\\Downloads\\Scheda1_GliArticoli.pdf");
			} catch (IOException e) {
				log.debug("context",e);
			}
			space();
			log.info("{}", atUrl);
			space();
			return atUrl;
		}
		return Collections.emptyList();
	}

	private static void downloadUsingStream(String urlStr, String file) throws IOException {
		URL url = new URL(urlStr);
		try (BufferedInputStream bis = new BufferedInputStream(url.openStream()); FileOutputStream fis = new FileOutputStream(file)) {
			byte[] buffer = new byte[1024];
			int count = 0;
			while ((count = bis.read(buffer, 0, 1024)) != -1) {
				fis.write(buffer, 0, count);
			}
		}
	}

	public RisultatoCodiceRisposta sendDocumentPaper(String serviceId, String apiKey, RichiestaImpegnoCartaceo richImpCart) {
		if (serviceId != null && apiKey != null && richImpCart != null) {
			return callConsolidatore(serviceId, apiKey, richImpCart);
		}
		space();
		log.info("Connection Refused");
		space();
		return null;
	}

	private RisultatoCodiceRisposta callConsolidatore(String serviceId, String apiKey, RichiestaImpegnoCartaceo richImpCart) {

		RisultatoCodiceRisposta rcr = new RisultatoCodiceRisposta();
		rcr.setOpResCodeResp(new OperationResultCodeResponse());
		
		List<String> errorList = new ArrayList<>();
		String errList1 = "field requestId is required";
		String errList2 = "unrecognized product Type";

		OffsetDateTime odt = OffsetDateTime.now();

		List<PaperEngageRequestAttachments> papEngReqAtt = richImpCart.papEngReq.getAttachments();

		if (richImpCart.papEngReq.getRequestId().equals("")) {
			errorList.add(errList1);
			rcr.opResCodeResp.setResultCode("400.01");
			rcr.opResCodeResp.setResultDescription("Syntax Error");
			rcr.opResCodeResp.setErrorList(errorList);
			rcr.opResCodeResp.setClientResponseTimeStamp(odt);
		} else if (!richImpCart.papEngReq.getProductType().equals("AR") && !richImpCart.papEngReq.getProductType().equals("890")
				&& !richImpCart.papEngReq.getProductType().equals("RI") && !richImpCart.papEngReq.getProductType().equals("RS")) {
			errorList.add(errList2);
			rcr.opResCodeResp.setResultCode("400.02");
			rcr.opResCodeResp.setResultDescription("Semantic Error");
			rcr.opResCodeResp.setErrorList(errorList);
			rcr.opResCodeResp.setClientResponseTimeStamp(odt);
		} // verificare caso se richiesta già esiste
		else if (richImpCart.papEngReq.getRequestId().equals("1")) {
			rcr.opResCodeResp.setResultCode("409.00");
			rcr.opResCodeResp.setResultDescription("duplicated requestId");
			rcr.opResCodeResp.setClientResponseTimeStamp(odt);
		}
		else if (!papEngReqAtt.isEmpty()) {
			for(PaperEngageRequestAttachments p: papEngReqAtt) {
				if(p.getUri().isEmpty()) {
					rcr.opResCodeResp.setResultCode("404.00");
					rcr.opResCodeResp.setResultDescription("bad request");
					rcr.opResCodeResp.setClientResponseTimeStamp(odt);
				}
			}
		}
		else if ((apiKey.equals("") || serviceId.equals(""))) {
			rcr.opResCodeResp.setResultCode("401.00");
			rcr.opResCodeResp.setResultDescription("Authentication Failed");
			rcr.opResCodeResp.setClientResponseTimeStamp(odt);
		} else {
			rcr.opResCodeResp.setResultCode("200.00");
			rcr.opResCodeResp.setResultDescription("OK");
			rcr.opResCodeResp.setClientResponseTimeStamp(odt);
			
			space();
			log.info(richImpCart.papEngReq.getAttachments().get(0).getUri());
			log.info(richImpCart.papEngReq.getAttachments().get(1).getUri());
			space();
		}
		space();
		log.info(rcr.toString());
		space();
		return rcr;
	}

}