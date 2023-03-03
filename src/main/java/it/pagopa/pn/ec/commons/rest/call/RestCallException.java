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
    }

    public static class BadMessageIdProvidedException extends RestCallException{

        public BadMessageIdProvidedException() {
            super("Bad messageId provided");
        }
    }

    public static class ISEForMessageIdCreationException extends RestCallException{

        public ISEForMessageIdCreationException() {
            super("Internal server error for messageId creation");
        }
    }
}
