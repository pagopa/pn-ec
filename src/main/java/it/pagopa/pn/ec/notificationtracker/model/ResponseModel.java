package it.pagopa.pn.ec.notificationtracker.model;

import lombok.*;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseModel {
	
	 private String message;
	 private boolean allowed ;
	 private String nottifMessage;

	

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isAllowed() {
		return allowed;
	}

	public void setAllowed(boolean allowed) {
		this.allowed = allowed;
	}

	public String getNottifMessage() {
		return nottifMessage;
	}

	public void setNottifMessage(String nottifMessage) {
		this.nottifMessage = nottifMessage;
	}
}
