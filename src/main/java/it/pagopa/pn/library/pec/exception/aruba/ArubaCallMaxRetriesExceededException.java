package it.pagopa.pn.library.pec.exception.aruba;

import it.pagopa.pn.library.pec.exception.pecservice.MaxRetriesExceededException;

public class ArubaCallMaxRetriesExceededException extends MaxRetriesExceededException {

    public ArubaCallMaxRetriesExceededException() {
        super("Aruba call max retries exceeded");
    }

    public ArubaCallMaxRetriesExceededException(String message) {
        super(message);
    }

    public ArubaCallMaxRetriesExceededException(String message, Throwable cause) {
        super(message, cause);
    }

}
