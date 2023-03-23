package it.pagopa.pn.ec.consolidatore.exception;

public class ClientNotAuthorizedOrFoundException extends RuntimeException {

    public ClientNotAuthorizedOrFoundException(String idClient) {
        super(String.format("Client id '%s' is unauthorized or has not been found", idClient));
    }
}
