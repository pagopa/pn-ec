package it.pagopa.pn.ec.commons.exception.email;

public class RetrieveSenderException extends RuntimeException {

    public RetrieveSenderException() {
        super("An error occurred while retrieving the sender adress from the MimeMessage object");
    }
}
