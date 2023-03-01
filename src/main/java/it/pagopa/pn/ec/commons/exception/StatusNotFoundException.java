package it.pagopa.pn.ec.commons.exception;

public class StatusNotFoundException extends RuntimeException {

	public StatusNotFoundException(String status) {
		super(String.format("Status '%s' has not been found.", status));
	}

}
