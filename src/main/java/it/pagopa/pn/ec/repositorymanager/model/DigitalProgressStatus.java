package it.pagopa.pn.ec.repositorymanager.model;

import java.time.OffsetDateTime;

public class DigitalProgressStatus {

	private OffsetDateTime timestamp;
	private String status;
	private String code;
	private String details;
	private GeneratedMessage genMess;
	
	public OffsetDateTime getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(OffsetDateTime timestamp) {
		this.timestamp = timestamp;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getDetails() {
		return details;
	}
	public void setDetails(String details) {
		this.details = details;
	}
	public GeneratedMessage getGenMess() {
		return genMess;
	}
	public void setGenMess(GeneratedMessage genMess) {
		this.genMess = genMess;
	}
	
	@Override
	public String toString() {
		return "DigitalProgressStatus [timestamp=" + timestamp + ", status=" + status + ", code=" + code + ", details="
				+ details + ", genMess=" + genMess + "]";
	}
	
}
