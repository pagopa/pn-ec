package it.pagopa.pn.ec.repositorymanager.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class DigitalRequestDto {

	private String correlationId;
	private String eventType;
	private String qos;
	private List<String> tags;
	private OffsetDateTime clientRequestTimeStamp;
	private String receiverDigitalAddress;
	private String messageText;
	private String senderDigitalAddress;
	private String channel;
	private String subjectText;
	private String messageContentType;
	private List<String> attachmentsUrls;
	
	public String getCorrelationId() {
		return correlationId;
	}
	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}
	public String getEventType() {
		return eventType;
	}
	public void setEventType(String eventType) {
		this.eventType = eventType;
	}
	public String getQos() {
		return qos;
	}
	public void setQos(String qos) {
		this.qos = qos;
	}
	public List<String> getTags() {
		return tags;
	}
	public void setTags(List<String> tags) {
		this.tags = tags;
	}
	public OffsetDateTime getClientRequestTimeStamp() {
		return clientRequestTimeStamp;
	}
	public void setClientRequestTimeStamp(OffsetDateTime clientRequestTimeStamp) {
		this.clientRequestTimeStamp = clientRequestTimeStamp;
	}
	public String getReceiverDigitalAddress() {
		return receiverDigitalAddress;
	}
	public void setReceiverDigitalAddress(String receiverDigitalAddress) {
		this.receiverDigitalAddress = receiverDigitalAddress;
	}
	public String getMessageText() {
		return messageText;
	}
	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}
	public String getSenderDigitalAddress() {
		return senderDigitalAddress;
	}
	public void setSenderDigitalAddress(String senderDigitalAddress) {
		this.senderDigitalAddress = senderDigitalAddress;
	}
	public String getChannel() {
		return channel;
	}
	public void setChannel(String channel) {
		this.channel = channel;
	}
	public String getSubjectText() {
		return subjectText;
	}
	public void setSubjectText(String subjectText) {
		this.subjectText = subjectText;
	}
	public String getMessageContentType() {
		return messageContentType;
	}
	public void setMessageContentType(String messageContentType) {
		this.messageContentType = messageContentType;
	}
	public List<String> getAttachmentsUrls() {
		return attachmentsUrls;
	}
	public void setAttachmentsUrls(List<String> attachmentsUrls) {
		this.attachmentsUrls = attachmentsUrls;
	}
	@Override
	public String toString() {
		return "DigitalRequestDto [correlationId=" + correlationId + ", eventType=" + eventType + ", qos=" + qos
				+ ", tags=" + tags + ", clientRequestTimeStamp=" + clientRequestTimeStamp + ", receiverDigitalAddress="
				+ receiverDigitalAddress + ", messageText=" + messageText + ", senderDigitalAddress=" + senderDigitalAddress
				+ ", channel=" + channel + ", subjectText=" + subjectText + ", messageContentType=" + messageContentType
				+ ", attachmentsUrls=" + attachmentsUrls + "]";
	}

}
