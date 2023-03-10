package it.pagopa.pn.ec.commons.exception.httpstatuscode;

public class Generic404ErrorException extends GenericHttpStatusException {

    public Generic404ErrorException(String title, String details) {
        super(title, details);
    }
}
