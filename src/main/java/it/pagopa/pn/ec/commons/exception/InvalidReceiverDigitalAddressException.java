package it.pagopa.pn.ec.commons.exception;

public class InvalidReceiverDigitalAddressException extends RuntimeException {


    public InvalidReceiverDigitalAddressException() {
        super("Invalid receiver digital address");
    }

    public InvalidReceiverDigitalAddressException(String message) {
        super("Invalid receiver digital address: " + message);
    }
}
