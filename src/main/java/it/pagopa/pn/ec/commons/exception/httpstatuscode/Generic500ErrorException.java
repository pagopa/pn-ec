package it.pagopa.pn.ec.commons.exception.httpstatuscode;

public class Generic500ErrorException extends GenericHttpStatusException {

    public Generic500ErrorException(String title, String details) {
        super(title, details);
    }
}
