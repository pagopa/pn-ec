package it.pagopa.pn.library.pec.exception.pecservice;

public class PnSpapiTemporaryErrorException extends Exception {
    public PnSpapiTemporaryErrorException(String message) {
        super(message);
    }

    public PnSpapiTemporaryErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
