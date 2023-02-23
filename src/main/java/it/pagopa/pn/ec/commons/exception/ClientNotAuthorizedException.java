package it.pagopa.pn.ec.commons.exception;

public class ClientNotAuthorizedException extends RuntimeException {

    public ClientNotAuthorizedException(String idClient) {
        super(String.format("Client id '%s' is unauthorized", idClient));
    }
}
