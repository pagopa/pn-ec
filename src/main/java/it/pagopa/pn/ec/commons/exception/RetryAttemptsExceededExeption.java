package it.pagopa.pn.ec.commons.exception;

public class RetryAttemptsExceededExeption extends RuntimeException{

    public RetryAttemptsExceededExeption(String messageId) {
        super(String.format("The message with 'id' %s has been deleted, attempts exceeded", messageId));
    }

}
