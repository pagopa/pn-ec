package it.pagopa.pn.ec.commons.exception.email;

public class ComposeMimeMessageException extends RuntimeException {

    public ComposeMimeMessageException() {
        super("An error occurred during MIME message composition");
    }
}
