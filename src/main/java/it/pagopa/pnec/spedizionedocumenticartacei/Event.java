package it.pagopa.pnec.spedizionedocumenticartacei;

import java.util.List;

public class Event {
	protected String requestId;	
	protected String registeredLetterCode;
	protected String productType;
	protected String iun;
	protected String statusCode;
	protected String statusDescription;
	protected String statusDateTime;
	protected String deliveryFailureCause;
	protected String attachmentId;
	protected String attachmentType;
	protected String attachmentUrl;
	protected String attachmentdate;
	//come e dove definire l'elenco degli indirizzi dei destinatari?
	protected List<String> discoveredAddress;
	protected String clientRequestTimestamp;
	
	public String getRequestId() {
		return requestId;
	}
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	public String getRegisteredLetterCode() {
		return registeredLetterCode;
	}
	public void setRegisteredLetterCode(String registeredLetterCode) {
		this.registeredLetterCode = registeredLetterCode;
	}
	public String getProductType() {
		return productType;
	}
	public void setProductType(String productType) {
		this.productType = productType;
	}
	public String getIun() {
		return iun;
	}
	public void setIun(String iun) {
		this.iun = iun;
	}
	public String getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}
	public String getStatusDescription() {
		return statusDescription;
	}
	public void setStatusDescription(String statusDescription) {
		this.statusDescription = statusDescription;
	}
	public String getStatusDateTime() {
		return statusDateTime;
	}
	public void setStatusDateTime(String statusDateTime) {
		this.statusDateTime = statusDateTime;
	}
	public String getDeliveryFailureCause() {
		return deliveryFailureCause;
	}
	public void setDeliveryFailureCause(String deliveryFailureCause) {
		this.deliveryFailureCause = deliveryFailureCause;
	}
	
	public String getAttachmentId() {
		return attachmentId;
	}
	public void setAttachmentId(String attachmentId) {
		this.attachmentId = attachmentId;
	}
	public String getAttachmentType() {
		return attachmentType;
	}
	public void setAttachmentType(String attachmentType) {
		this.attachmentType = attachmentType;
	}
	public String getAttachmentUrl() {
		return attachmentUrl;
	}
	public void setAttachmentUrl(String attachmentUrl) {
		this.attachmentUrl = attachmentUrl;
	}
	public String getAttachmentdate() {
		return attachmentdate;
	}
	public void setAttachmentdate(String attachmentdate) {
		this.attachmentdate = attachmentdate;
	}
	public List<String> getDiscoveredAddress() {
		return discoveredAddress;
	}
	public void setDiscoveredAddress(List<String> discoveredAddress) {
		this.discoveredAddress = discoveredAddress;
	}
	public String getClientRequestTimestamp() {
		return clientRequestTimestamp;
	}
	public void setClientRequestTimestamp(String clientRequestTimestamp) {
		this.clientRequestTimestamp = clientRequestTimestamp;
	}

}
