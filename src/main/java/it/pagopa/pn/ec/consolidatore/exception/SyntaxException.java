package it.pagopa.pn.ec.consolidatore.exception;

public class SyntaxException extends RuntimeException{

    public SyntaxException(String field) {
        super(String.format("Field '%s' is required", field));
    }

}
