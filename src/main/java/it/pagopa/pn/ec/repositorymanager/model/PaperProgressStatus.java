package it.pagopa.pn.ec.repositorymanager.model;

import java.time.OffsetDateTime;
import java.util.List;

import org.openapitools.client.model.PaperProgressStatusEventAttachments;

import it.pagopa.pnec.spedizionedocumenticartacei.DiscoveredAddress;

public class PaperProgressStatus {

	private String registeredLetterCode;
	private String statusCode;
	private String statusDescription;
	private OffsetDateTime statusDateTime;
	private String deliveryFailureCause;
	private List<PaperProgressStatusEventAttachments> attachments;
	private DiscoveredAddress discoveredAddress;
	private OffsetDateTime clientRequestTimeStamp;
	
	public String getRegisteredLetterCode() {
		return registeredLetterCode;
	}
	public void setRegisteredLetterCode(String registeredLetterCode) {
		this.registeredLetterCode = registeredLetterCode;
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
	public OffsetDateTime getStatusDateTime() {
		return statusDateTime;
	}
	public void setStatusDateTime(OffsetDateTime statusDateTime) {
		this.statusDateTime = statusDateTime;
	}
	public String getDeliveryFailureCause() {
		return deliveryFailureCause;
	}
	public void setDeliveryFailureCause(String deliveryFailureCause) {
		this.deliveryFailureCause = deliveryFailureCause;
	}
	public List<PaperProgressStatusEventAttachments> getAttachments() {
		return attachments;
	}
	public void setAttachments(List<PaperProgressStatusEventAttachments> attachments) {
		this.attachments = attachments;
	}
	public DiscoveredAddress getDiscoveredAddress() {
		return discoveredAddress;
	}
	public void setDiscoveredAddress(DiscoveredAddress discoveredAddress) {
		this.discoveredAddress = discoveredAddress;
	}
	public OffsetDateTime getClientRequestTimeStamp() {
		return clientRequestTimeStamp;
	}
	public void setClientRequestTimeStamp(OffsetDateTime clientRequestTimeStamp) {
		this.clientRequestTimeStamp = clientRequestTimeStamp;
	}
	
	@Override
	public String toString() {
		return "PaperProgressStatus [registeredLetterCode=" + registeredLetterCode + ", statusCode=" + statusCode
				+ ", statusDescription=" + statusDescription + ", statusDateTime=" + statusDateTime
				+ ", deliveryFailureCause=" + deliveryFailureCause + ", attachments=" + attachments
				+ ", discoveredAddress=" + discoveredAddress + ", clientRequestTimeStamp=" + clientRequestTimeStamp
				+ "]";
	}
	
}
