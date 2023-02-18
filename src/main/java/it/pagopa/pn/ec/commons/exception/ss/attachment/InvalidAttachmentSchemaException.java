package it.pagopa.pn.ec.commons.exception.ss.attachment;

public class InvalidAttachmentSchemaException extends RuntimeException {

    public InvalidAttachmentSchemaException() {
        super("Schema non valido per il download degli allegati");
    }
}
