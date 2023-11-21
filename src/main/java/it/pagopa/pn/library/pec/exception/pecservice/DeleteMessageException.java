package it.pagopa.pn.library.pec.exception.pecservice;

public class DeleteMessageException extends RuntimeException{

    public DeleteMessageException(String messageID) {
        super(String.format("Exception encountered when deleting message with id '%s'", messageID));
    }
}
