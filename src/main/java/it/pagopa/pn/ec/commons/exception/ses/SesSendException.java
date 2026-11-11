package it.pagopa.pn.ec.commons.exception.ses;

public class SesSendException extends RuntimeException{

    public SesSendException(String message) {
        super(message);
    }

    public SesSendException(String message, Throwable cause) {
        super(message, cause);
    }

    public SesSendException() {
        super("An error occurred while sending via SES");
    }

    public SesSendException(Throwable cause) {
        super("An error occurred while sending via SES", cause);
    }

    public static class SesMaxRetriesExceededException extends SesSendException{

        public SesMaxRetriesExceededException() {
            super("SES max retries exceeded");
        }

        public SesMaxRetriesExceededException(Throwable cause) {
            super("SES max retries exceeded", cause);
        }
    }

}
