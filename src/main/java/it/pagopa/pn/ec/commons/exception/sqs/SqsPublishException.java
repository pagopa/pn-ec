package it.pagopa.pn.ec.commons.exception.sqs;

public class SqsPublishException extends RuntimeException {

    public SqsPublishException(String queueName) {
        super(String.format("An error occurred during publishing to '%s' queue", queueName));
    }
}
