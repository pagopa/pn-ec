package it.pagopa.pn.ec.commons.rest.call;

public class RestCallException extends RuntimeException{

    public RestCallException(String message) {
        super(message);
    }

    public static class ResourceNotFoundException extends RestCallException{

        public ResourceNotFoundException() {
            super("Request not found");
        }

        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
}
