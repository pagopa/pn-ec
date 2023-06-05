package it.pagopa.pn.ec.commons.exception;

public class ShaGenerationException extends RuntimeException {

    public ShaGenerationException(String message) {
        super(String.format("Could not generate sha256 string: %s", message));
    }

}
