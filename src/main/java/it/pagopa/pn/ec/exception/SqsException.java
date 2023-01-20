package it.pagopa.pn.ec.exception;

public class SqsException extends RuntimeException{

    public SqsException() {
        super("Generic SQS exception");
    }

    public static class SqsPublishException extends RuntimeException{

        public SqsPublishException(String queueName) {
            super(String.format("Messaging exception during publishing to %s queue", queueName));
        }
    }
}
