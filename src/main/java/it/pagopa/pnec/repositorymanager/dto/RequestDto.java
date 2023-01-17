package it.pagopa.pnec.repositorymanager.dto;

import java.util.List;

public class RequestDto {

	private String requestId;
	private DigitalRequestDto digitalReq;
	private PaperRequestDto paperReq;
	private List<EventsDto> events;
	
	public String getRequestId() {
		return requestId;
	}
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	public DigitalRequestDto getDigitalReq() {
		return digitalReq;
	}
	public void setDigitalReq(DigitalRequestDto digitalReq) {
		this.digitalReq = digitalReq;
	}
	public PaperRequestDto getPaperReq() {
		return paperReq;
	}
	public void setPaperReq(PaperRequestDto paperReq) {
		this.paperReq = paperReq;
	}
	public List<EventsDto> getEvents() {
		return events;
	}
	public void setEvents(List<EventsDto> events) {
		this.events = events;
	}
	
	@Override
	public String toString() {
		return "RequestDto [requestId=" + requestId + ", digitalReq=" + digitalReq + ", paperReq=" + paperReq + ", events="
				+ events + "]";
	}	
	
}
