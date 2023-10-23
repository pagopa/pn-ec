package it.pagopa.pn.ec.commons.exception.email;

public class EmailException extends RuntimeException {

    public EmailException(String message) {
        super(message);
    }

    public EmailException() {
        super("An error occurred during lavorazione richiesta email");
    }

    public static class EmailMaxRetriesExceededException extends it.pagopa.pn.ec.commons.exception.cartaceo.CartaceoSendException {
        public EmailMaxRetriesExceededException() {
            super("EMAIL max retries exceeded");
        }
    }
}
