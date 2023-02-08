package it.pagopa.pn.ec.commons.exception.sns;

public class SnsSendException extends RuntimeException{

    public SnsSendException(String message) {
        super(message);
    }

    public SnsSendException() {
        super("An error occurred while sending via SNS");
    }

    public static class SnsMaxRetriesExceededException extends SnsSendException{

        public SnsMaxRetriesExceededException() {
            super("SNS max retries exceeded");
        }
    }
}
