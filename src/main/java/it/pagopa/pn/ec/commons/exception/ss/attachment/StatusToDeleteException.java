package it.pagopa.pn.ec.commons.exception.ss.attachment;

public class StatusToDeleteException extends RuntimeException{
    public StatusToDeleteException(String messageId) {
        super(String.format("The message with 'id' %s has been deleted, status toDelete", messageId));
    }
}
