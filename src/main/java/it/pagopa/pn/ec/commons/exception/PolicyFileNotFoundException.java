package it.pagopa.pn.ec.commons.exception;

public class PolicyFileNotFoundException extends RuntimeException {

    public PolicyFileNotFoundException(String message) {
        super(String.format("Errore nella lettura del file JSON '%s' ", message));
    }
}
