package it.pagopa.pnec.repositorymanager.dto;

public class ClientConfigurationDto {

	private String cxId;
	private String sqsArn;
	private String sqsName;
	private String pecReplyTo;
	private String mailReplyTo;
	private SenderPhysicalAddressDto senderPhysicalAddress;
	
	public String getCxId() {
		return cxId;
	}
	public void setCxId(String cxId) {
		this.cxId = cxId;
	}
	public String getSqsArn() {
		return sqsArn;
	}
	public void setSqsArn(String sqsArn) {
		this.sqsArn = sqsArn;
	}
	public String getSqsName() {
		return sqsName;
	}
	public void setSqsName(String sqsName) {
		this.sqsName = sqsName;
	}
	public String getPecReplyTo() {
		return pecReplyTo;
	}
	public void setPecReplyTo(String pecReplyTo) {
		this.pecReplyTo = pecReplyTo;
	}
	public String getMailReplyTo() {
		return mailReplyTo;
	}
	public void setMailReplyTo(String mailReplyTo) {
		this.mailReplyTo = mailReplyTo;
	}
	public SenderPhysicalAddressDto getSenderPhysicalAddress() {
		return senderPhysicalAddress;
	}
	public void setSenderPhysicalAddress(SenderPhysicalAddressDto senderPhysicalAddress) {
		this.senderPhysicalAddress = senderPhysicalAddress;
	}
	@Override
	public String toString() {
		return "ClientConfigurationDto [cxId=" + cxId + ", sqsArn=" + sqsArn + ", sqsName=" + sqsName + ", pecReplyTo="
				+ pecReplyTo + ", mailReplyTo=" + mailReplyTo+ ", senderPhysicalAddress=" + senderPhysicalAddress
				+ "]";
	}
	 
	
}
