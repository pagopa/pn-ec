package it.pagopa.pn.ec.repositorymanager.dto;

public class EventsDto {

	private DigitalProgressStatusDto digProgrStatus;
	private PaperProgressStatusDto paperProgrStatus;
	
	public DigitalProgressStatusDto getDigProgrStatus() {
		return digProgrStatus;
	}
	public void setDigProgrStatus(DigitalProgressStatusDto digProgrStatus) {
		this.digProgrStatus = digProgrStatus;
	}
	public PaperProgressStatusDto getPaperProgrStatus() {
		return paperProgrStatus;
	}
	public void setPaperProgrStatus(PaperProgressStatusDto paperProgrStatus) {
		this.paperProgrStatus = paperProgrStatus;
	}
	
	@Override
	public String toString() {
		return "EventsDto [digProgrStatus=" + digProgrStatus + ", paperProgrStatus=" + paperProgrStatus + "]";
	}
	
}
