package it.pagopa.pnec.repositorymanager.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class ClientConfiguration {

	private String cxId;
	private String sqsArn;
	private String sqsName;
	private String pecReplyTo;
	private String mailReplyTo;
	private SenderPhysicalAddress senderPhysicalAddress;
	
	@DynamoDbPartitionKey
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
	public SenderPhysicalAddress getSenderPhysicalAddress() {
		return senderPhysicalAddress;
	}
	public void setSenderPhysicalAddress(SenderPhysicalAddress senderPhysicalAddress) {
		this.senderPhysicalAddress = senderPhysicalAddress;
	}
	
	@Override
	public String toString() {
		return "ClientConfiguration [cxId=" + cxId + ", sqsArn=" + sqsArn + ", sqsName=" + sqsName + ", pecReplyTo="
				+ pecReplyTo + ", mailReplyTo=" + mailReplyTo + ", senderPhysicalAddress=" + senderPhysicalAddress
				+ "]";
	}

}
