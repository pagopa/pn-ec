package it.pagopa.pn.ec.exception;

public class SqsException extends RuntimeException{

    public SqsException() {
        super("Generic SQS exception");
    }

    public static class SqsObjectToJsonException extends RuntimeException{

        public SqsObjectToJsonException(String queueName) {
            super(String.format("Jackson error conversion during publishing to %s queue", queueName));
        }
    }
}
