package it.pagopa.pn.ec.commons.exception.aruba;

public class ArubaCallMaxRetriesExceededException extends RuntimeException {

    public ArubaCallMaxRetriesExceededException() {
        super("Aruba call max retries exceeded");
    }
}
