package it.pagopa.pn.library.pec.exception.aruba;

public class ArubaCallMaxRetriesExceededException extends RuntimeException {

    public ArubaCallMaxRetriesExceededException() {
        super("Aruba call max retries exceeded");
    }
}
