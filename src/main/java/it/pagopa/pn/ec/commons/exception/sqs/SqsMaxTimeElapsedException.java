package it.pagopa.pn.ec.commons.exception.sqs;

public class SqsMaxTimeElapsedException extends RuntimeException {

    public SqsMaxTimeElapsedException() {
        super("Max time elapsed for retries");
    }

}
