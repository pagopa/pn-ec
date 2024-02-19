package it.pagopa.pn.ec.pec.exception;

public class MaxSizeExceededException extends RuntimeException {

    public MaxSizeExceededException(String message) {
        super(String.format("Max size exceeded : %s", message));
    }

}
