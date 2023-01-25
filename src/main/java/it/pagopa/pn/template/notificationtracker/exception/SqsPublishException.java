package it.pagopa.pn.template.notificationtracker.exception;

public class SqsPublishException extends RuntimeException {

    public SqsPublishException(String queueName) {
        super(String.format("An error occurred during publishing to %s queue", queueName));
    }
}
