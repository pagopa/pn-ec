package it.pagopa.pn.library.pec.exception.pecservice;

public class MaxRetriesExceededException extends RuntimeException {

    public MaxRetriesExceededException() {
        super();
    }

    public MaxRetriesExceededException(String message) {
        super(message);
    }

    public MaxRetriesExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
