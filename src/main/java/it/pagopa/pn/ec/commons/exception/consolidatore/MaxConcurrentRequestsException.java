package it.pagopa.pn.ec.commons.exception.consolidatore;

public class MaxConcurrentRequestsException extends RuntimeException {

    public MaxConcurrentRequestsException(String message) {
        super(message);
    }

    public MaxConcurrentRequestsException(String message, Throwable cause) {
        super(message, cause);
    }
}
