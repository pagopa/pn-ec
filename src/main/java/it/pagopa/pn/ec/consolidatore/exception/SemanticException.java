package it.pagopa.pn.ec.consolidatore.exception;

public class SemanticException extends RuntimeException {

    public SemanticException(String field) {
        super(String.format("Unrecognized '%s'", field));
    }

}
