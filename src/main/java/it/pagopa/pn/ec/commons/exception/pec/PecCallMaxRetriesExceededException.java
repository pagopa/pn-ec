package it.pagopa.pn.ec.commons.exception.pec;

public class PecCallMaxRetriesExceededException extends RuntimeException {

    public PecCallMaxRetriesExceededException() {
        super("Pec call max retries exceeded");
    }
}
