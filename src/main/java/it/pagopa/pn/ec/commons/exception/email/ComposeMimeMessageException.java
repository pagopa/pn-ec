package it.pagopa.pn.ec.commons.exception.email;

public class ComposeMimeMessageException extends MimeMessageException {

    public ComposeMimeMessageException() {
        super("An error occurred during MIME message composition");
    }

    public ComposeMimeMessageException(String message) {
        super(String.format("An error occurred during MIME message composition : %s", message));
    }
}
