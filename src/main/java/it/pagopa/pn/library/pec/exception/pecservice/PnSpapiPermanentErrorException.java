package it.pagopa.pn.library.pec.exception.pecservice;

public class PnSpapiPermanentErrorException extends Exception {
    public PnSpapiPermanentErrorException(String message) {
        super(message);
    }

    public PnSpapiPermanentErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
