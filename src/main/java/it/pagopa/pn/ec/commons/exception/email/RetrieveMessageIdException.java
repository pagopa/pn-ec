package it.pagopa.pn.ec.commons.exception.email;

public class RetrieveMessageIdException extends RuntimeException {

    public RetrieveMessageIdException() {
        super("An error occurred while retrieving the message id from the MimeMessage object");
    }
}
