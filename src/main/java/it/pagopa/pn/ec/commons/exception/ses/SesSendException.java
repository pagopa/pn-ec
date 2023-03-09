package it.pagopa.pn.ec.commons.exception.ses;

public class SesSendException extends RuntimeException{

    public SesSendException(String message) {
        super(message);
    }

    public SesSendException() {
        super("An error occurred while sending via SES");
    }

    public static class SesMaxRetriesExceededException extends SesSendException{

        public SesMaxRetriesExceededException() {
            super("SES max retries exceeded");
        }
    }

}
