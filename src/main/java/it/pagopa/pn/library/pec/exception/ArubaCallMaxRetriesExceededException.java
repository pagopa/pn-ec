package it.pagopa.pn.library.pec.exception;

public class ArubaCallMaxRetriesExceededException extends RuntimeException {

    public ArubaCallMaxRetriesExceededException() {
        super("Aruba call max retries exceeded");
    }
}
