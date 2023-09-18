package it.pagopa.pn.ec.commons.exception;

public class InvalidApiKeyException extends RuntimeException {

    public InvalidApiKeyException() {
        super("XApiKey is not valid");
    }

}
