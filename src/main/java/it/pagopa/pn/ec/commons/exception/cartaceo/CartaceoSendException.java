package it.pagopa.pn.ec.commons.exception.cartaceo;

public class CartaceoSendException extends RuntimeException {

	public CartaceoSendException(String message) {
		super(message);
	}

	public CartaceoSendException() {
		super("An error occurred while sending via CARTACEO");
	}

	public static class CartaceoMaxRetriesExceededException extends CartaceoSendException {
		public CartaceoMaxRetriesExceededException() {
			super("CARTACEO max retries exceeded");
		}
	}
}
