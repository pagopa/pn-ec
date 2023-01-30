package it.pagopa.pn.ec.commons.exception;

public class RequestAlreadyInProgressException extends RuntimeException {

    public RequestAlreadyInProgressException(String idRequest) {
        super(String.format("The request with id %s is already being processed", idRequest));
    }
}
