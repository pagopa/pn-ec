package it.pagopa.pnec.spedizionedocumenticartacei;

public class ServicePaperDocument {
	
	public boolean getConnection(HeaderRequest param) {
		if(param.serviceId != null) {
			return false;
		} else {
		return true;
		}
	}
	
	public boolean getStatusCode(HeaderRequest param) {
		if(param.requestId == null) {
			return true;
		}
		return false;
	}
	
	public boolean getAttachment(Event event) {
		if(event.attachmentUrl == null) {
			return true;
		}
		return false;
	}
	
//	public boolean downloadAttachment() {
//		return false;
//	}
	
	public boolean sendDocument(Scheme scheme) {
		boolean esito = true;
			if(scheme.attachmentUri != null && scheme.requestId != null) {
			esito = false;
			}
		return esito;
	}
}