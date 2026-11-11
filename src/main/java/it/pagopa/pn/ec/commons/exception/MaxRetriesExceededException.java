package it.pagopa.pn.ec.commons.exception;

public class MaxRetriesExceededException extends RuntimeException {

    public MaxRetriesExceededException() {
        super("Max retries exceeded");
    }

    public MaxRetriesExceededException(Throwable cause) {
        super("Max retries exceeded", cause);
    }

}
