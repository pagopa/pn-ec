package it.pagopa.pn.ec.commons.rest.call;

public class RestCallException extends RuntimeException{

    public RestCallException(String message) {
        super(message);
    }

    public static class ResourceNotFoundException extends RestCallException{

        public ResourceNotFoundException() {
            super("Resource not found");
        }

        public ResourceNotFoundException(String message) {
            super(message);
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
}
