package it.pagopa.pn.ec.repositorymanager.model;

import java.util.List;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class Request {

	private String requestId;
	private DigitalRequest digitalReq;
	private PaperRequest paperReq;
	private List<Events> events;
	
	@DynamoDbPartitionKey
	public String getRequestId() {
		return requestId;
	}
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	public DigitalRequest getDigitalReq() {
		return digitalReq;
	}
	public void setDigitalReq(DigitalRequest digitalReq) {
		this.digitalReq = digitalReq;
	}
	public PaperRequest getPaperReq() {
		return paperReq;
	}
	public void setPaperReq(PaperRequest paperReq) {
		this.paperReq = paperReq;
	}
	public List<Events> getEvents() {
		return events;
	}
	public void setEvents(List<Events> events) {
		this.events = events;
	}
	
	@Override
	public String toString() {
		return "Request [requestId=" + requestId + ", digitalReq=" + digitalReq + ", paperReq=" + paperReq + ", events="
				+ events + "]";
	}
		
}
