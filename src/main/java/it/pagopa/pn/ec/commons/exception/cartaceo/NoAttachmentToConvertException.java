package it.pagopa.pn.ec.commons.exception.cartaceo;

public class NoAttachmentToConvertException extends RuntimeException {

    public NoAttachmentToConvertException() {
        super("There is no attachment to convert");
    }

}
