package it.pagopa.pnec.spedizionedocumenticartacei;

public class HeaderRequest {
	
	//Identificativo con cui Piattaforma Notifiche si identifica
	protected String serviceId;
	
	//Credenziale di accesso
	protected String apiKey;
	
	//Identificativo della richiesta di postalizzazione
	protected String requestId;

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	
}
