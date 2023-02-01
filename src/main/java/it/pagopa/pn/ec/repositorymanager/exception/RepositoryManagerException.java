package it.pagopa.pn.ec.repositorymanager.exception;

public class RepositoryManagerException extends RuntimeException {

    public RepositoryManagerException() {
        super("Generic exception in RepositoryManagerService");
    }

    public static class IdClientAlreadyPresent extends RuntimeException {

        public IdClientAlreadyPresent(String idClient) {
            super(String.format("Id client '%s' already exists", idClient));
        }
    }

    public static class IdRequestAlreadyPresent extends RuntimeException {

        public IdRequestAlreadyPresent(String idRequest) {
            super(String.format("Id request '%s' already present", idRequest));
        }
    }

    public static class IdClientNotFoundException extends RuntimeException {

        public IdClientNotFoundException(String idClient) {
            super(String.format("Id client '%s' not found", idClient));
        }
    }

    public static class RequestNotFoundException extends RuntimeException {

        public RequestNotFoundException(String requestIdx) {
            super(String.format("Request id '%s' not found", requestIdx));
        }
    }

    public static class RequestMalformedException extends RuntimeException {

        public RequestMalformedException(String message) {
            super(message);
        }
    }
}
