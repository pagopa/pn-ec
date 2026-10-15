package it.pagopa.pn.ec.commons.exception.sns;

public class SnsSendException extends RuntimeException{

    public SnsSendException(String message) {
        super(message);
    }

    public SnsSendException(String message, Throwable cause) {
        super(message, cause);
    }

    public SnsSendException() {
        super("An error occurred while sending via SNS");
    }

    public SnsSendException(Throwable cause) {
        super("An error occurred while sending via SNS", cause);
    }

    public static class SnsMaxRetriesExceededException extends SnsSendException{

        public SnsMaxRetriesExceededException() {
            super("SNS max retries exceeded");
        }

        public SnsMaxRetriesExceededException(Throwable cause) {
            super("SNS max retries exceeded", cause);
        }
    }
}
