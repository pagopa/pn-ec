package it.pagopa.pn.ec.repositorymanager.exception;

public class RepositoryManagerException extends RuntimeException {

    public RepositoryManagerException() {
        super("Generic exception in RepositoryManagerService");
    }

    public static class IdClientAlreadyPresent extends RuntimeException{

        public IdClientAlreadyPresent(String idClient) {
            super(String.format("Id client %s already exists", idClient));
        }
    }

    public static class DynamoDbException extends RuntimeException{

        public DynamoDbException() {
            super("Error interacting with DynamoDb");
        }
    }
}
