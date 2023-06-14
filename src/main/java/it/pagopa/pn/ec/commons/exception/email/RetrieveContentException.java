package it.pagopa.pn.ec.commons.exception.email;

public class RetrieveContentException extends MimeMessageException{

    public RetrieveContentException() {
        super("An error occurred while retrieving the content from the MimeMessage object");
    }

}
