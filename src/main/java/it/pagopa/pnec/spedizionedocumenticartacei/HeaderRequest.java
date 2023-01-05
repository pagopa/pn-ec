package it.pagopa.pnec.spedizionedocumenticartacei;

public class HeaderRequest {
	
	//Identificativo con cui Piattaforma Notifiche si identifica
	protected String xPagopaExtchServiceId;
	
	//Credenziale di accesso
	protected String xApiKey;
	
	//Identificativo della richiesta di postalizzazione
	protected String requestId;

	public String getXPagopaExtchServiceId() {
		return xPagopaExtchServiceId;
	}

	public void setXPagopaExtchServiceId(String xPagopaExtchServiceId) {
		this.xPagopaExtchServiceId = xPagopaExtchServiceId;
	}

	public String getXApiKey() {
		return xApiKey;
	}

	public void setXApiKey(String xApiKey) {
		this.xApiKey = xApiKey;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	@Override
	public String toString() {
		return "HeaderRequest [xPagopaExtchServiceId=" + xPagopaExtchServiceId + ", xApiKey=" + xApiKey + ", requestId="
				+ requestId + "]";
	}
	
}
