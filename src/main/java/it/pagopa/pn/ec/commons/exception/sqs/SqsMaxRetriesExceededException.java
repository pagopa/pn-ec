package it.pagopa.pn.ec.commons.exception.sqs;

public class SqsMaxRetriesExceededException extends RuntimeException{

    public SqsMaxRetriesExceededException() {
        super("Sqs max retries exceeded");
    }

}
