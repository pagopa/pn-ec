package it.pagopa.pn.ec.commons.exception.email;

public class RetrieveAttachmentException extends RuntimeException{

    public RetrieveAttachmentException() {
        super("An error occurred while retrieving the attachment from the MimeMessage object");
    }

}
