package it.pagopa.pn.ec.commons.exception;

import it.pagopa.pn.ec.repositorymanager.model.entity.DigitalProgressStatus;
import it.pagopa.pn.ec.repositorymanager.model.entity.PaperProgressStatus;

public class RepositoryManagerException extends RuntimeException {

    public RepositoryManagerException() {
        super("Generic exception in RepositoryManagerService");
    }

    public RepositoryManagerException(String message) {
        super(message);
    }

    public static class IdClientAlreadyPresent extends RepositoryManagerException {

        public IdClientAlreadyPresent(String idClient) {
            super(String.format("Id client '%s' already exists", idClient));
        }
    }

    public static class IdRequestAlreadyPresent extends RepositoryManagerException {

        public IdRequestAlreadyPresent(String idRequest) {
            super(String.format("Id request '%s' already present", idRequest));
        }
    }

    public static class RequestWithSameHash extends RepositoryManagerException {

        public RequestWithSameHash(String idRequest, String requestHash) {
            super(String.format("Id request '%s' with hash '%s' already present", idRequest, requestHash));
        }
    }

    public static class IdClientNotFoundException extends RepositoryManagerException {

        public IdClientNotFoundException(String idClient) {
            super(String.format("Id client '%s' not found", idClient));
        }
    }

    public static class RequestNotFoundException extends RepositoryManagerException {

        public RequestNotFoundException(String requestIdx) {
            super(String.format("Request id '%s' not found", requestIdx));
        }
    }

    public static class RequestByMessageIdNotFoundException extends RepositoryManagerException {

        public RequestByMessageIdNotFoundException(String messageId) {
            super(String.format("Request with message id '%s' not found", messageId));
        }
    }

    public static class RequestMalformedException extends RepositoryManagerException {

        public RequestMalformedException(String message) {
            super(message);
        }

        public RequestMalformedException() {
            super("Request Body Malformed");
        }
    }

    public static class EventAlreadyExistsException extends RepositoryManagerException {

        public EventAlreadyExistsException(String requestId, DigitalProgressStatus digitalProgressStatus) {
            super(String.format("The %s event registered at %s is already present in the list of events of digital request %s",
                                digitalProgressStatus.getStatus(),
                                digitalProgressStatus.getEventTimestamp(),
                                requestId));
        }

        public EventAlreadyExistsException(String requestId, PaperProgressStatus paperProgressStatus) {
            super(String.format("The %s event registered at %s is already present in the list of events of paper request %s",
                                paperProgressStatus.getStatus(),
                                paperProgressStatus.getStatusDateTime(),
                                requestId));
        }
    }
}
