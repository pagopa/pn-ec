package it.pagopa.pn.ec.testutils.exception;

public class DynamoDbInitTableCreationException extends RuntimeException{

    public DynamoDbInitTableCreationException(String tableName) {
        super(String.format("Error during %s dynamo db table creation", tableName));
    }
}
