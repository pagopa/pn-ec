package it.pagopa.pn.ec.commons.rest.call;

public class RestCallException extends RuntimeException{

    public RestCallException(String message) {
        super(message);
    }

    public static class ResourceNotFoundException extends RestCallException{

        public ResourceNotFoundException() {
            super("Resource not found");
        }
    }

    public static class ResourceAlreadyExistsException extends RestCallException{

        public ResourceAlreadyExistsException() {
            super("Resource already exists");
        }

        public ResourceAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class ResourceAlreadyInProgressException extends RestCallException{

        public ResourceAlreadyInProgressException() {
            super("Resource already in progress");
        }
    }
}
