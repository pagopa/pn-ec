package it.pagopa.pn.ec.repositorymanager.model;

public class Events {

	private DigitalProgressStatus digProgrStatus;
	private PaperProgressStatus paperProgrStatus;
	
	public DigitalProgressStatus getDigProgrStatus() {
		return digProgrStatus;
	}
	public void setDigProgrStatus(DigitalProgressStatus digProgrStatus) {
		this.digProgrStatus = digProgrStatus;
	}
	public PaperProgressStatus getPaperProgrStatus() {
		return paperProgrStatus;
	}
	public void setPaperProgrStatus(PaperProgressStatus paperProgrStatus) {
		this.paperProgrStatus = paperProgrStatus;
	}
	@Override
	public String toString() {
		return "Events [digProgrStatus=" + digProgrStatus + ", paperProgrStatus=" + paperProgrStatus + "]";
	}
	
}
