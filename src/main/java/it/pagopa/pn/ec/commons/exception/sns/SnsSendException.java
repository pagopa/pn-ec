package it.pagopa.pn.ec.commons.exception.sns;

public class SnsSendException extends RuntimeException{

    public SnsSendException() {
        super("An error occurred while sending via SNS");
    }

    public static class SnsMaxRetriesExceeded extends RuntimeException{

        public SnsMaxRetriesExceeded() {
            super("SNS max retries exceeded");
        }
    }
}
