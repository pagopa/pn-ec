package it.pagopa.pn.ec.commons.exception;

public class StatusToDeleteException extends RuntimeException{
    public StatusToDeleteException(String messageId) {
        super(String.format("The message with 'id' %s has been deleted, status toDelete", messageId));
    }
}
