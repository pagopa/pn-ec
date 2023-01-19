package it.pagopa.pn.ec.repositorymanager.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class PaperProgressStatusDto {

	private String registeredLetterCode;
	private String statusCode;
	private String statusDescription;
	private OffsetDateTime statusDateTime;
	private String deliveryFailureCause;
	private List<PaperProgressStatusEventAttachmentsDto> attachments;
	private DiscoveredAddressDto discoveredAddress;
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
	public List<PaperProgressStatusEventAttachmentsDto> getAttachments() {
		return attachments;
	}
	public void setAttachments(List<PaperProgressStatusEventAttachmentsDto> attachments) {
		this.attachments = attachments;
	}
	public DiscoveredAddressDto getDiscoveredAddress() {
		return discoveredAddress;
	}
	public void setDiscoveredAddress(DiscoveredAddressDto discoveredAddress) {
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
		return "PaperProgressStatusDto [registeredLetterCode=" + registeredLetterCode + ", statusCode=" + statusCode
				+ ", statusDescription=" + statusDescription + ", statusDateTime=" + statusDateTime
				+ ", deliveryFailureCause=" + deliveryFailureCause + ", attachments=" + attachments
				+ ", discoveredAddress=" + discoveredAddress + ", clientRequestTimeStamp=" + clientRequestTimeStamp
				+ "]";
	}
	
}
