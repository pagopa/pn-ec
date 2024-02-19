package it.pagopa.pn.ec.consolidatore.exception;

public class NoSuchEventException extends RuntimeException {

    public NoSuchEventException(String eventName) {
        super(String.format("Event '%s' has not been found in events list", eventName));
    }

}
