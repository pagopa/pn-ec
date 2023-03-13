package it.pagopa.pn.ec.commons.exception.httpstatuscode;

public class Generic400ErrorException extends GenericHttpStatusException {

    public Generic400ErrorException(String title, String details) {
        super(title, details);
    }
}
