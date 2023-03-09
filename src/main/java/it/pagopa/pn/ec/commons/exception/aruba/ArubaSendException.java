package it.pagopa.pn.ec.commons.exception.aruba;

public class ArubaSendException extends RuntimeException{

    public ArubaSendException(String message) { super(message); }

    public ArubaSendException() { super("An error occurred while sending via ARUBA"); }

    public static class ArubaMaxRetriesExceededException extends ArubaSendException {
        public ArubaMaxRetriesExceededException() { super("ARUBA max retries exceeded");}
    }
}
