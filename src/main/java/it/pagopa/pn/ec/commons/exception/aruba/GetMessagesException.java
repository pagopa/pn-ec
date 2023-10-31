package it.pagopa.pn.ec.commons.exception.aruba;

public class GetMessagesException extends RuntimeException {

    public GetMessagesException() {
        super("An error occurred during getMessages from Aruba");
    }

}
