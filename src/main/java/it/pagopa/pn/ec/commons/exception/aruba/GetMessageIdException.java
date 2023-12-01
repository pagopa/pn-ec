package it.pagopa.pn.ec.commons.exception.aruba;

public class GetMessageIdException extends RuntimeException {

    public GetMessageIdException(String messageId) {
        super(String.format("An error occurred during getMessageID from Aruba for messageID '%s'", messageId));
    }

}
